package com.gxwtech.rtdemo.services;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.gxwtech.rtdemo.BGReading;
import com.gxwtech.rtdemo.Constants;
import com.gxwtech.rtdemo.Intents;
import com.gxwtech.rtdemo.medtronic.PumpData.BasalProfile;
import com.gxwtech.rtdemo.medtronic.PumpData.BasalProfileEntry;
import com.gxwtech.rtdemo.medtronic.PumpData.BasalProfileTypeEnum;
import com.gxwtech.rtdemo.medtronic.PumpData.HistoryReport;
import com.gxwtech.rtdemo.medtronic.PumpData.PumpSettings;
import com.gxwtech.rtdemo.medtronic.PumpData.TempBasalPair;
import com.gxwtech.rtdemo.medtronic.PumpData.records.BolusWizard;
import com.gxwtech.rtdemo.medtronic.TempBasalEvent;
import com.gxwtech.rtdemo.services.pumpmanager.PumpManager;
import com.gxwtech.rtdemo.services.pumpmanager.TempBasalPairParcel;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.joda.time.Minutes;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;


/**
 * Created by geoff on 5/8/15.
 *
 * Needed from pump:
 * pump_high_temp_max
 * bg_target (with bg_min, bg_max) from profile
 * insulin_sensitivity_factor(on time of day)
 * DIA profiles to use.
 * temp_basal data
 * clock data
 * basal data
 *
 * Needed to control pump:
 * setTempBasal()
 *
 */
    /*
    APSLogic requires a lot of data from the pump.
    This makes it very tempting to put the PumpManager in here, or a reference to it.
    Most of the calls to PumpManager are blocking and take a long time to complete, don't like that.
    Also, there is the issue of concurrency with other requests to the pump manager,
    which are currently handled by running all pump manager requests through the RTDemoService message queue.

    Thinking out loud:
    Assume APSLogic is separate service thread from RTService.
    It competes with the GUI for access (via Intents) to the PumpManager through RTService.
    All data requests and responses have to be through Intents.
    This makes the Intent mappings much more complex (2 actors = 2 paths, -> 3 actors = 6 paths)
    that's uuuuugly.

    Assume APSLogic is member of RTDemo service, and runs on the RTDemoService message queue.
    That means it is synchronized with all PumpManager requests and all internet (mongo) requests.
    However, it means that it will block while trying to get pump data.  Ok for now.
    It only has to run once every five minutes.

    So: APSLogic will be a member of RTDemoService, will be able to make direct requests of the
    PumpManager, and will be synchronized with all pump calls as it is running from the RTDemoService
    thread.  One downside is that we will get duplicate calls: the GUI will ask the pump for data,
    and APSLogic will ask the pump for data which will be duplicate data, which will take even longer.
    Perhaps some smart pump-response caching is in order.

    Note: 2015-06-05 suddendly started having problems talking to pump again.  Is the small delay
    added by trying to inform the UI when we're going to sleep for a bit pushing us over the edge
    of the Pump's "time-window" for reading back responses? FIXME

     */


public class APSLogic {
    /* BEGIN settable defaults */
    private static final String TAG = "APSLogic";
    Context mContext;
    PumpManager mPumpManager;
    String mLogfileName;

    // our cache of the profile settings
    // Updated (at the minimum) at the start of each MakeADecision() run
    PersonalProfile mPersonalProfile = new PersonalProfile();

    // pump_high_temp_max is maximum units that the pump will produce for a temp basal (2 U/h for MM722)
    // +++ fixme: read this value from the pump
    private static double pump_high_temp_max = 6.2;

    // don't high-temp if IOB > max_IOB
    double max_IOB = 15;

    // Allow low glucose suspend?
    //boolean enable_low_glucose_suspend = false;
    boolean enable_low_glucose_suspend = true;

    double low_glucose_suspend_at = 85; //mg/dl

    // choices here are: dia_0pt5_hour, dia_1pt5_hour, dia_1_hour, dia_2_hour, dia_2pt5_hour, dia_3_hour, dia_3pt5_hour,dia_4_hour, dia_4pt5_hour, dia_5_hour, dia_5pt5_hour
    // Which Duration of Insulin Action table to use?
    // TODO: change this to an enum
    DIATables.DIATableEnum meal_bolus_dia_table = DIATables.DIATableEnum.DIA_4pt5_hour;
    DIATables.DIATableEnum negative_insulin_dia_table = DIATables.DIATableEnum.DIA_2_hour;
    DIATables.DIATableEnum default_dia_table = DIATables.DIATableEnum.DIA_3_hour;
    /* END settable defaults */

    public double iobValueAtAbsTime(Instant startTime, double insulinUnits,
                                    Instant valueTime,
                                    DIATables.DIATableEnum dia_table) {
        boolean debug_iobValueAtAbsTime = false;
        if (debug_iobValueAtAbsTime) {
            log("startTime: " + startTime.toString());
            log("valueTime: " + valueTime.toString());
            log(String.format("insulinUnits: %.3f",insulinUnits));
            log("dia_table: " + dia_table.toString());
        }
        //int elapsed_minutes = (valueTime - startTime).total_seconds() / 60;
        Minutes minutes = Minutes.minutesBetween(startTime,valueTime); // todo: check this
        int elapsed_minutes = minutes.getMinutes();
        if (debug_iobValueAtAbsTime) {
            log(String.format("elapsed minutes since insulin event began: %d",elapsed_minutes));
        }
        double rval = insulinUnits * DIATables.insulinPercentRemaining(elapsed_minutes, dia_table) / 100;
        if (debug_iobValueAtAbsTime) {
            log(String.format("IOB remaining from event: %.3f",rval));
        }
        return rval;
    }

    // to calculate COB:
    // Easier than IOB, as we assume it is a linear relationship rate=profile.carbs_hr,
    // 20 minutes delayed from consumption
    // Geoff, if possible make the minutes delayed from consumption and editable option in the profile UI. -Toby
    public double cobValueAtAbsTime(Instant startTime,
                                    double carbGrams,
                                    Instant valueTime,
                                    double carbs_absorbed_per_hour) {
        // CAR is carbs absorbed per minute
        double CAR = carbs_absorbed_per_hour / 60.0;
        double rval = 0;
        //int elapsed_minutes = (valueTime - startTime).total_seconds() / 60;
        Minutes minutes = Minutes.minutesBetween(startTime, valueTime); // todo: check this
        int elapsed_minutes = minutes.getMinutes();
        if (elapsed_minutes < 0) {
            //none ingested
            rval = 0;
        } else if (elapsed_minutes < 20) {
            //ingested, but none yet absorbed
            rval = carbGrams;
        } else {
            double carbs_absorbed = (CAR * (elapsed_minutes - 20));
            rval = carbGrams - carbs_absorbed;
            // negative values do not make sense
            if (rval < 0) {
                rval = 0;
            }
        }
        dlog(String.format("COB remaining from event: %.1f",rval));
        return rval;
    }

    //Now we define a function to calculate the basal rate at a give time:
    // todo: this should be fixed to handle all basal profiles, not a given profile.
    public double basal_rate_at_abs_time(Instant when, BasalProfile basalProfile) {
        // From the pump's basal profiles, and the pump's idea of the time-of-day,
        // figure out which basal period is active and determine the rate.
        BasalProfileEntry entry = basalProfile.getEntryForTime(when);
        if (entry != null) {
            /*
            log(String.format("For %s, found entry rate=%.3f (%d), start=%s (%d) ",
                    time_of_day.toString("HH:mm"),
                    entry.rate,entry.rate_raw,
                    entry.startTime.toString("HH:mm"),entry.startTime_raw));
                    */
            return entry.rate;
        }
        log("Error: Null Basal Rate Object?");
        return -9.9999E6; // clearly bad value
    }

    // isf is the change in blood glucose due to a unit of insulin.
    // varies with time of day (among many other things!)
    // To fix: need to gather the rate profile from the pump's bolus wizard settings.
    public double isf(Instant when) {
        // TODO: given a time of day, return the insulin sensitivity factor
        // In the RPi version, we retrieved this number (a single number) from MongoDB profile
        // TODO: Must fix this
        //return mPersonalProfile.isf;
        return 40.0; // fixme: very broken.
    }

    // use this to floor to nearest 0.025
    public double mm_floor_rate(double x) {
        return (Math.floor(x * 40.0))/40.0;
    }



    // This function is to be run after CollectData()
    // Here we make a decision about TempBasals, based on all factors
    private void MakeADecision() {
        // get a recent blood-glucose (BG) reading from CGM or from Mongo (to start with)
        // make sure we have all the pump info we need, such as Basal Profiles,
        // current BasalProfile in use,
        // any recent Bolus or Bolus Wizard activity, for IOB and COB calculations
        // patient profile, for ISF calculations, carbRatios, BG target, max/min,
        // current TempBasal activity from pump
        // and a SANE current Date/Time (clocks are finicky...)
        // MaxIOB
        // and user preferences such as algorithm options like
        // enable_low_glucose_suspend, AR Regression

        // TODO: make each run of MakeADecision log to a text file locally.
        // TODO: Delete log files older than xxx

        // ensure that our settings from PersonalPreferences are up to date
        updateFromPersonalPrefs();

        log(String.format("CAR=%.1f, bgmax=%.1f, target=%.1f, bgmin=%.1f, max temp=%.3f",
                mPersonalProfile.CAR,
                mPersonalProfile.BGMax,
                mPersonalProfile.TargetBG,
                mPersonalProfile.BGMin,
                mPersonalProfile.MaxTempBasalRate));

        log("Getting status of temp basal from pump.");
                getCurrentTempBasalFromPump();
                log(String.format("Temp Basal status: %.2f U, %d minutes remaining.",
                        mCurrentTempBasal.mInsulinRate, mCurrentTempBasal.mDurationMinutes));
        log("Getting RTC clock data from pump");
        DateTime rtcDateTime = getRTCTimestampFromPump();
        log("Pump RTC: " + rtcDateTime.toDateTimeISO().toString());
        log("Pump RTC in local time: " + rtcDateTime.toLocalDateTime().toString());
        Instant now = new Instant(); // cache local system time
        log("Local System Time is " + now.toDateTime().toLocalDateTime().toString());
        Minutes pumpTimeOffsetMinutes = Minutes.minutesBetween(now,rtcDateTime);
        log(String.format("Pump Time is %d minutes %s system time.",
                Math.abs(pumpTimeOffsetMinutes.getMinutes()),
                (pumpTimeOffsetMinutes.getMinutes() < 0) ? "behind" : "ahead of"));

        log("Getting pump settings");
        // NOTE: get pump settings before getting basal profiles.
        getPumpSettingsFromPump();
        log(String.format("PumpSettings reports Max Basal Rate: %.2f",mPumpSettings.mMaxBasal));

        // how often should we check for changes in pump settings?
        // Make a button on UI to reset/reload settings from pump.
        //if (!gotBasalProfiles) {
            // NOTE: get pump settings before getting basal profiles
            log("Getting basal profiles from pump");
            getBasalProfiles();
        //}
        double currentBasalRate = basal_rate_at_abs_time(now,
                getCurrentBasalProfile());
        sendCurrBasalToUI(currentBasalRate);

        // Get total IOB and COB from pump bolus wizard events
        double iobTotal = 0; // insulin-on-board total, amount of unabsorbed insulin (in Units) in body
        double cobTotal = 0; // carbohydrates-on-board total, amount of undigested carbohydrates in body (grams)
        double remainingBGImpact_IOBtotal = 0;
        double remainingBGImpact_COBtotal = 0;
        // Go get the history report from the pump
        // NOTE! this command may take many seconds to return (around 6 seconds, 23 seconds if we have
        // to renew the pump's wireless power control
        HistoryReport historyReport = getPumpManager().getPumpHistory();
        if (historyReport.mBolusWizardEvents.size() == 0) {
            log("No Bolus Wizard events found in history");
        } else {
            for (BolusWizard bw : historyReport.mBolusWizardEvents) {
                DateTime timestamp = bw.getTimeStamp();
                if (timestamp.isBefore(now.toDateTime().minusMinutes(DIATables.insulinImpactMinutesMax))) {
                    // The Bolus occurred a long time ago (insulinImpartMinutesMax (300 minutes))
                    // we can safely ignore it.
                } else {
                    double bolusAmount = bw.getBolusEstimate();
                    double carbInput = bw.getCarbInput();
                    double icRatio = bw.getICRatio();
                    double isf = bw.getSensitivity();
                    log(String.format("Found Bolus Wizard Event(%s, Carbs %.1f gm, Insulin %.3f U)",
                            timestamp.toLocalDateTime().toString(),
                            carbInput,
                            bolusAmount));

                    //TODO: Use correct table, (from profile?)
                    double iob = iobValueAtAbsTime(timestamp.toInstant(), bolusAmount, now.toInstant(),
                            DIATables.DIATableEnum.DIA_3_hour);
                    double remainingBGImpact_IOBpartial = iob * isf;
                    log(String.format("Bolus wizard event (%s): IOB=%.1f U, isf=%.1f, bg impact remaining=%.1f mg/dL",
                            timestamp.toString("HH:mm"),
                            iob,
                            isf,remainingBGImpact_IOBpartial));
                    remainingBGImpact_IOBtotal = remainingBGImpact_IOBtotal + remainingBGImpact_IOBpartial;
                    iobTotal = iobTotal + iob;

                    double cob = cobValueAtAbsTime(timestamp.toInstant(), carbInput, now.toInstant(),
                            mPersonalProfile.CAR);
                    double remainingBGImpact_COBpartial = cob * isf / icRatio;
                    log(String.format("Bolus wizard event (%s): COB=%.1f gm, carbRatio=%.1f, BG impact remaining=%.1f mg/dL",
                            timestamp.toString("HH:mm"),
                            cob,icRatio,
                            remainingBGImpact_COBpartial));
                    remainingBGImpact_COBtotal = remainingBGImpact_COBtotal + remainingBGImpact_COBpartial;
                    cobTotal = cobTotal + cob;
                }
            }
        }
        // Now deal with any temp basal events reported:
        if (historyReport.mBasalEvents.size() == 0) {
            log("No Temp Basal events found in history.");
        } else

        {
            ArrayList<Instant> endTimes = new ArrayList<>();
            //Sorting: create a sorter by date
            // process temp basal events in order, calculating IOB for each event.
            // Must process in order, so that we can see when one starts and stops
            Collections.sort(historyReport.mBasalEvents, new Comparator<TempBasalEvent>() {
                @Override
                public int compare(TempBasalEvent ev1, TempBasalEvent ev2) {
                    return ev1.mTimestamp.compareTo(ev2.mTimestamp);
                }
            });
            // For debug: make sure all events are in order
            /*
            for (TempBasalEvent tb : historyReport.mBasalEvents) {
                log(String.format("Found Temp Basal Event(%s,rate:%.3f U/hr,duration: %d minutes",
                        tb.mTimestamp, tb.mBasalPair.mInsulinRate, tb.mBasalPair.mDurationMinutes));
            }
            */
            // if the rate and duration are zero, it is the end of a temp basal (or a cancellation)
            // Find start and end time for each event
            //   Find scheduled end-time
            for (int i = 0; i < historyReport.mBasalEvents.size(); i++) {
                TempBasalEvent tb = historyReport.mBasalEvents.get(i);
                Instant endtime = tb.mTimestamp.plusMinutes(tb.mBasalPair.mDurationMinutes).toInstant();
                if (i < historyReport.mBasalEvents.size() - 1) {
                    // Not the last event, so next event may end this one.
                    Instant nextStartTime = historyReport.mBasalEvents.get(i + 1).mTimestamp.toInstant();
                    if (nextStartTime.isBefore(endtime)) {
                        // next event does end this one.
                        endtime = nextStartTime;
                    }
                }
                // Remember, one of them may not have ended yet.
                if (DateTime.now().plus(pumpTimeOffsetMinutes).isBefore(endtime)) {
                    endtime = DateTime.now().plus(pumpTimeOffsetMinutes).toInstant();
                }
                // Now record the endtime for the event (using parallel arrays)
                endTimes.add(i, endtime);
            }
            /* Temp basals are delivered slowly. To estimate the IOB, we will
             * divide up the temp basal and treat it as a series of boluses delivered
             * once per minute.
             */
            for (int i = 0; i < historyReport.mBasalEvents.size(); i++) {
                TempBasalEvent tb = historyReport.mBasalEvents.get(i);
                if (endTimes.get(i).isBefore(now.toDateTime().minusMinutes(DIATables.insulinImpactMinutesMax))) {
                    // The temp basal ended a long time ago (insulinImpartMinutesMax (300 minutes))
                    // we can safely ignore it.
                } else {
                    Minutes actualDuration = Minutes.minutesBetween(tb.mTimestamp.toInstant(), endTimes.get(i));
                    double insulinDelivered = (tb.mBasalPair.mInsulinRate / 60) * actualDuration.getMinutes();

                    double thisEventIOBRemaining = 0.0;
                    double thisEventIOBImpact = 0.0;
                    ArrayList<DIATables.DIATableEnum> tablesUsed = new ArrayList<>();

                    BasalProfileEntry bpEntry = getCurrentBasalProfile().getEntryForTime(tb.mTimestamp.toInstant());
                    log(String.format("At start of insulin event(%s), basal rate is %.3f (%d), start %s (%d)",
                            tb.mTimestamp.toLocalTime().toString("HH:mm"),
                            bpEntry.rate, bpEntry.rate_raw,
                            bpEntry.startTime.toString("HH:mm"),bpEntry.startTime_raw));

                    bpEntry = getCurrentBasalProfile().getEntryForTime(endTimes.get(i));
                    log(String.format("At end of insulin event(%s), basal rate is %.3f (%d), start %s (%d)",
                            endTimes.get(i).toDateTime().toLocalTime().toString("HH:mm"),
                            bpEntry.rate, bpEntry.rate_raw,
                            bpEntry.startTime.toString("HH:mm"),bpEntry.startTime_raw));

                    for (int j = 0; j < actualDuration.getMinutes(); j++) {
                        DIATables.DIATableEnum whichTable = default_dia_table;

                    /*
                     *  Temp basals that are below the current basal rate are handled by
                     *  using a rate that is relative to the basal at the time of insulin delivery.
                     *  i.e. We determine what the normal basal rate was at the time of the 1-minute interval
                     *  and subtract it from the temp basal rate.  This can give us a negative insulin
                     *  rate.  When using negative relative rates, use the negative insulin table.
                     */
                        // FIXME: this means that we have to keep track of which basal profile was in use!
                        Instant insulinTime = tb.mTimestamp.plusMinutes(j).toInstant();

                        double relativeRate = tb.mBasalPair.mInsulinRate - basal_rate_at_abs_time(insulinTime,
                                getCurrentBasalProfile() // <--- fixme: basal profiles may have changed, current is not correct
                        );

                        if (relativeRate < 0) {
                            whichTable = negative_insulin_dia_table;
                        }
                        if (!tablesUsed.contains(whichTable)) {
                            tablesUsed.add(whichTable);
                        }
                        double part = iobValueAtAbsTime(insulinTime,
                                relativeRate / 60, now.toInstant(), whichTable);
                        thisEventIOBRemaining += part;
                        double remainingBGImpact_IOBPartial = part * isf(insulinTime);
                        thisEventIOBImpact += remainingBGImpact_IOBPartial;
                    } // end integration
                    remainingBGImpact_IOBtotal += thisEventIOBImpact;
                    iobTotal += thisEventIOBRemaining;
                    String tableString = "";
                    for (DIATables.DIATableEnum en : tablesUsed) {
                        tableString += en.name() + " ";
                    }
                    if (tableString.equals("")) {
                        tableString = "none";
                    }
                    // TODO: show work.
                    // The relative rate calculation is wrong because:
                    // A) The actual relative rate is computed on a per minute basis, to account for changes in basal rate
                    // B) It only uses the "current basal profile", not the one that was active at the time
                    log(String.format("TempBasalEvent rate: %.3f U/hr, relative: %.3f U/hr, start: %s, end %s, remaining IOB %.3f, isf=45.0, impact %.1f, tables used: %s",
                            tb.mBasalPair.mInsulinRate,
                            tb.mBasalPair.mInsulinRate - basal_rate_at_abs_time(tb.mTimestamp.toInstant(),
                                    getCurrentBasalProfile()), // <-- fixme: use correct basal profile
                            tb.mTimestamp.toLocalTime().toString("HH:mm"),
                            endTimes.get(i).toDateTime(DateTimeZone.getDefault()).toString("HH:mm"),
                            thisEventIOBRemaining,
                            thisEventIOBImpact,
                            tableString));
                }
            }
        }


        log(String.format("Totals: IOB=%.3f U, COB=%.1f gm",iobTotal,cobTotal));
        log(String.format("BG impact remaining from IOB=%.1f mg/dL, COB=%.1f mg/dL",
                remainingBGImpact_IOBtotal, remainingBGImpact_COBtotal));
        sendIOBToUI(iobTotal);
        sendCOBToUI(cobTotal);

        // if most recent reading is more than ten minutes old, do nothing.
        // If a temp basal is running, fine.  It will expire.
        Minutes cgm_elapsed = Minutes.minutesBetween(mCachedLatestBGReading.mTimestamp,now);
        if (cgm_elapsed.getMinutes() > 10) {
            log(String.format("Most recent CGM reading is %d minutes old. Quitting.",
                    cgm_elapsed.getMinutes()));
            return;
        }

        log(String.format("Using CGM reading %.2f, which is %d minutes old",
                mCachedLatestBGReading.mBg,cgm_elapsed.getMinutes()));

        // todo: get these values from pump history, MongoDB, CGM, etc.
        double bg = mCachedLatestBGReading.mBg; // for simplified reading of the algorithm below
        /*
        todo: sanity-check latest BG reading (is it less than 39? is it greater than 500?
         */

        //calc desired BG adjustment
        // We then predict a value for the BG for "sometime in the future, when all IOB and COB are used up".
        // We don't know when that will be (though that would be an interesting algorithm to write).
        // If we see that the eventual BG will be too much or too little, we decide if we can take action.

        double eventualBG = bg - remainingBGImpact_IOBtotal + remainingBGImpact_COBtotal;
        log(String.format("eventualBG = Current BG (%.1f) - bg change from IOB (%.1f)  + bg change from COB (%.1f) = %.1f",
                bg,remainingBGImpact_IOBtotal, remainingBGImpact_COBtotal,eventualBG));

        double predictedBG;

        // the strangeness in using the average of BG and eventual BG is (I think) due to the logarithmic nature of BG values
        // Remove this once we put AR in place?
        if (bg > eventualBG) {
            // we are predicting a drop in BG
            predictedBG = eventualBG;
        } else {
            // we are predicting a rise in BG
            predictedBG = (bg + eventualBG) / 2;
        }

        sendPredBGToUI(predictedBG);

        /*
        todo: many places below talk about "for %d more minutes" but reference a total, not remaining duration. fix.
         */

        if (predictedBG < mPersonalProfile.BGMin) {
            log(String.format("Predicting that BG will fall to %.1f which is less than %.1f. Can we do something about it?",
                    predictedBG,mPersonalProfile.BGMin));
            // we predict that the BG will go too low,
            // so lower the current basal rate with a temp-basal
            double newTempBasalRate; // our desired temp basal rate (Units/Hr)
            log("Warning - using static isf value of 45.0");
            newTempBasalRate = Math.max(0, currentBasalRate - 2 * (mPersonalProfile.BGMin - predictedBG)
                    / isf(now));
            log(String.format("We would like to set temporary rate to %.3f U/h",newTempBasalRate));
            newTempBasalRate = mm_floor_rate(newTempBasalRate);
            log(String.format("New temporary rate rounded to %.3f U/h", newTempBasalRate));

            // abide by pump's max. (it will enforce this anyway...)
            double pumpLimitedTempBasalRate = Math.min(newTempBasalRate, pump_high_temp_max);
            if (pumpLimitedTempBasalRate != newTempBasalRate) {
                newTempBasalRate = pumpLimitedTempBasalRate;
                log(String.format("Pump will limit temporary rate to %.3f U/h",newTempBasalRate));
            }

            if (mCurrentTempBasal.mDurationMinutes > 0) {
                log(String.format("Pump is currently administering a temp basal of %.3f U/h for %d more minutes.",
                        mCurrentTempBasal.mInsulinRate,mCurrentTempBasal.mDurationMinutes));
                if (newTempBasalRate >= mCurrentTempBasal.mInsulinRate) {
                    log("Pump is already doing its best here, so leave it alone.");
                } else {
                    log("Pump is already administering a temp basal, but we want a lower one.");
                    // set duration of temp basal to the minimum allowed rate.
                    setTempBasal(newTempBasalRate, 30, currentBasalRate);
                    // 30 minutes is minimum for minimed
                }
            } else {
                log(String.format("Pump is not currently administering a temp basal.  Current basal rate is %.3f U/h",currentBasalRate));
                if (newTempBasalRate < currentBasalRate) {
                    // set duration of temp basal to the minimum allowed rate.
                    setTempBasal(newTempBasalRate, 30, currentBasalRate);
                    // 30 minutes is minimum for Mini-Med
                } else {
                    log(String.format("Current basal rate is already at %.3f U/hr.  Nothing to do.",currentBasalRate));
                }
            }
        } else if (predictedBG < mPersonalProfile.TargetBG) {
            log(String.format("Predicting that BG will fall below %.1f, but will stay above %.1f",
                    mPersonalProfile.TargetBG,
                    mPersonalProfile.BGMin));
            // we predict that bg will be lower than target, but within "normal" range
            // cancel any high-temp, let any low-temp run
            if (mCurrentTempBasal.mDurationMinutes > 0) {
                log(String.format("Pump is currently administering a temp basal of %.3f U/h for %d more minutes.",
                        mCurrentTempBasal.mInsulinRate,mCurrentTempBasal.mDurationMinutes));
                // a temp basal rate adjustment is running.
                if (mCurrentTempBasal.mInsulinRate > currentBasalRate) {
                    log("Because temp basal rate adjustment is higher than regular basal rate, cancelling temp basal.");
                    // cancel any existing temp basal
                    setTempBasal(0, 0, currentBasalRate);
                } else {
                    log("Current temp basal rate adjustment is lower than regular basal rate, so no action is necessary.");
                }
            }
        } else if (predictedBG > mPersonalProfile.BGMax) {
            log(String.format("Predicting that BG will rise to %.1f which is above %.1f. Can we do something about it?",
                    predictedBG,mPersonalProfile.BGMax));
            // high-temp as required, to get predicted BG down to bg_max
            double fastInsulin;
            log("Warning - using static isf of 45.0");
            fastInsulin = 2 * (predictedBG - mPersonalProfile.BGMax) / isf(now);
            if (fastInsulin > mPersonalProfile.MaxTempBasalRate) {
                log(String.format("Insulin delivery limited (by roundtrip) from %.3f U/h to %.3f U/h.",
                        fastInsulin,mPersonalProfile.MaxTempBasalRate));
            }
            TempBasalPair newTempBasal = new TempBasalPair();
            newTempBasal.mInsulinRate = currentBasalRate + Math.min(mPersonalProfile.MaxTempBasalRate, fastInsulin);
            log(String.format("We would like to set temporary rate to %.3f U/h",newTempBasal.mInsulinRate));
            newTempBasal.mInsulinRate = mm_floor_rate(newTempBasal.mInsulinRate);
            log(String.format("New temporary rate rounded to %.3f U/h",newTempBasal.mInsulinRate));
            // abide by pump"s max. (it will enforce this anyway...)
            newTempBasal.mInsulinRate = Math.min(newTempBasal.mInsulinRate, pump_high_temp_max);
            log(String.format("Pump will limit temporary rate to %.3f U/h",newTempBasal.mInsulinRate));
            if (iobTotal >= max_IOB) {
                log(String.format("IOB (%.1f U) already exceeds maximum allowed (%.1f U), so no course of action available.",iobTotal,max_IOB));
            } else {
                // IOB total is ok, check for currently-administered temp-basal
                if (mCurrentTempBasal.mDurationMinutes > 0) {
                    log(String.format("Pump is currently administering a temp basal of %.3f U/h for %d more minutes.",
                            mCurrentTempBasal.mInsulinRate,mCurrentTempBasal.mDurationMinutes));
                    // add 0.1 to compensate for rounding at pump -- if pump is delivering 2.7 and we want to deliver 2.8, we do nothing.
                    if (newTempBasal.mInsulinRate <= (mCurrentTempBasal.mInsulinRate + 0.1)) {
                        log("recommended rate is less than current rate (or close), therefore we do nothing.");
                    } else {
                        log("Recommended rate is more than current temp basal rate, so over-write old temp basal with new");
                        setTempBasal(newTempBasal.mInsulinRate, 30, currentBasalRate);
                    }
                } else {
                    // Pump is not administering a temp basal currently.
                    if (newTempBasal.mInsulinRate > (currentBasalRate + 0.1)) {
                        log(String.format("If no action is taken, predicting BG will rise above %.1f, so recommending %.3f U/h for 30 min.",
                                mPersonalProfile.BGMax,
                                newTempBasal.mInsulinRate));
                        setTempBasal(newTempBasal.mInsulinRate, 30, currentBasalRate);
                    } else {
                        log(String.format("Desired Temp Basal rate of %.3f is close to current basal rate of %.3f. Doing nothing.",
                                newTempBasal.mInsulinRate,currentBasalRate));
                    }
                }
            }
        } else if ((predictedBG > mPersonalProfile.TargetBG) || ((iobTotal > max_IOB) && (predictedBG > mPersonalProfile.BGMax))) {
            // cancel any low temp, let any high-temp run
            log(String.format("Predicting BG will rise to %.1f which is above %.1f but is below %.1f.",
                    predictedBG,mPersonalProfile.TargetBG,mPersonalProfile.BGMax));
            if (mCurrentTempBasal.mDurationMinutes > 0) {
                log(String.format("Pump is currently administering a temp basal of %.3f U/h for %d more minutes.",
                        mCurrentTempBasal.mInsulinRate,mCurrentTempBasal.mDurationMinutes));
                // a temp basal rate adjustment is running
                if (mCurrentTempBasal.mInsulinRate > currentBasalRate) {
                    // it is a high-temp, so let it run
                    log("Predicted BG is in the higher-range, current temp-basal is slightly higher, so let it run");
                } else {
                    log("Predicted BG is in the higher range, so cancel lower temp-basal");
                    setTempBasal(0, 0, currentBasalRate);
                }
            } else {
                log("No temp-basal is currently running. Good.");
            }
        } else {
            log("how did we get here?");
        }
    }

    /*******************************************************
     *
     * Past here is the infrastructure for this class.
     * Shouldn't have to modify anything past here.
     *
     * I'm trying to keep all the java-overhead gunk out of the way.
     * Anything that isn't directly related to making decisions.
     *
     *******************************************************/
    public APSLogic(Context context, PumpManager pumpManager) {
        mContext = context;
        mPumpManager = pumpManager;
        mLogfileName = DateTime.now().toString();
        init();
    }
    public void init() {
        // initialize member vars to sane settings.
    }

    // This should be a real class, not an internal one:
    public class PersonalProfile {
        public double CAR = -10E6; // Carb Absorption Ratio - how fast the body absorbs carbs.
        public double BGMax = 0.0;
        public double TargetBG = 0.0;
        public double BGMin = 0.0;
        // MaxTempBasalRate is the maximum units for a temp basal (we ourselves choose this)
        // (this is relative to the current temp-basal, so if current basal rate is 0.5, we will at most set
        // the temp basal rate to 0.5 + MaxTempBasalRate)
        public double MaxTempBasalRate = 0.0;
    }

    public void runAPSLogicOnce() {
        MakeADecision();
    }

    public void updateCachedLatestBGReading(BGReading bgr) {
        mCachedLatestBGReading = bgr;
    }

    BasalProfile basalProfileSTD, basalProfileA, basalProfileB, mCurrentBasalProfile;

    boolean gotBasalProfiles = false;
    BGReading mCachedLatestBGReading = new BGReading();
    TempBasalPair mCurrentTempBasal = new TempBasalPair();
    PumpSettings mPumpSettings = new PumpSettings();

    private boolean getBasalProfiles() {
        basalProfileSTD = getPumpManager().getProfile(BasalProfileTypeEnum.STD);
        Log.w(TAG,"Basal Profile STD ----------------");
        basalProfileSTD.dumpBasalProfile();

        basalProfileA = getPumpManager().getProfile(BasalProfileTypeEnum.A);
        Log.w(TAG,"Basal Profile A ------------------");
        basalProfileA.dumpBasalProfile();

        basalProfileB = getPumpManager().getProfile(BasalProfileTypeEnum.B);
        Log.w(TAG,"Basal Profile B ------------------");
        basalProfileB.dumpBasalProfile();

        gotBasalProfiles = (basalProfileSTD != null) && (basalProfileA != null) && (basalProfileB != null);
        mCurrentBasalProfile = basalProfileSTD;
        // fixme: need to get pump settings before basal profiles.  Should ensure this happens.
        if (mPumpSettings.mSelectedPattern == BasalProfileTypeEnum.A) {
            mCurrentBasalProfile = basalProfileA;
            log("Using basal profile A");
        } else if (mPumpSettings.mSelectedPattern == BasalProfileTypeEnum.B) {
            mCurrentBasalProfile = basalProfileB;
            log("Using basal profile B");
        } else {
            log("Using basal profile STD");
        }
        if (basalProfileSTD == null) log("Basal profile STD is null!");
        if (basalProfileA == null) log("Basal profile A is null!");
        if (basalProfileB == null) log("Basal profile B is null!");
        return gotBasalProfiles;
    }

    // use this to get access to the pump manager.
    // Don't cache the pumpManager, as it can be reinstantiated when the USB device is (un)plugged.
    // todo: this may cause crashes when the carelink is (un)plugged.  fix in RTDemoService? how?
    private PumpManager getPumpManager() {
        return mPumpManager;
    }

    // When this method succeeds, it also contacts the database to add the new treatment.
    // todo?: Need curr_basal to calculate if this is a negative insulin event
    public void setTempBasal(double rateUnitsPerHour, int periodMinutes, double currBasalRate) {
        log(String.format("Set Temp Basal: rate=%.3f, minutes=%d",rateUnitsPerHour,periodMinutes));
        getPumpManager().setTempBasal(rateUnitsPerHour,periodMinutes);
    }

    // Get currently used Basals Profile from the pump.
    // We want to fetch these once on startup, then used cached copies.
    public BasalProfile getCurrentBasalProfile() {
        return mCurrentBasalProfile;
    }

    // This function is run when we receive a reading from xDrip, broadcast as an Intent
    public void receiveXDripBGReading(BGReading bgr) {
        // sanity check and cache the latest reading, used only if xDrip samples are enabled.
    }

    public void updateFromPersonalPrefs() {
        // get CAR value from prefs
        SharedPreferences settings = mContext.getSharedPreferences(Constants.PreferenceID.MainActivityPrefName, 0);
        double car = (double)settings.getFloat(Constants.PrefName.CARPrefName, (float) 30.0);
        // Notify APSLogic of new value
        setCAR(car);
        setMaxTempBasalRate((double) settings.getFloat(Constants.PrefName.PPMaxTempBasalRatePrefName, (float) 6.1));
        setBGMin((double) settings.getFloat(Constants.PrefName.PPBGMinPrefName, (float) 95.0));
        setTargetBG((double) settings.getFloat(Constants.PrefName.PPTargetBGPrefName, (float) 115.0));
        setBGMax((double) settings.getFloat(Constants.PrefName.PPBGMaxPrefName, (float) 125.0));
    }

    public void setCAR(double car) {
        mPersonalProfile.CAR = car;
    }

    public void setMaxTempBasalRate(double mtbr) {
        mPersonalProfile.MaxTempBasalRate = mtbr;
    }

    public void setBGMin(double bgmin) {
        mPersonalProfile.BGMin = bgmin;
    }

    public void setTargetBG(double target) {
        mPersonalProfile.TargetBG = target;
    }

    public void setBGMax(double bgmax) {
        mPersonalProfile.BGMax = bgmax;
    }

    // This command retrieves the CURRENTLY ACTIVE temp basal from the pump
    // This command can "sleep" for up to 20 seconds while running.
    public void getCurrentTempBasalFromPump() {
        TempBasalPair rval;
        rval = getPumpManager().getCurrentTempBasal();
        mCurrentTempBasal = rval;
        sendTempBasalToUI(mCurrentTempBasal);
    }

    public DateTime getRTCTimestampFromPump() {
        DateTime dt = getPumpManager().getRTCTimestamp();
        return dt;
    }

    public void getPumpSettingsFromPump() {
        PumpSettings settings = getPumpManager().getPumpSettings();
        mPumpSettings = settings;
    }

    // Communications with the UI involve Android system classes and calls,
    // Let RTDemoService be the interface for all that stuff
    public void sendTempBasalToUI(TempBasalPair pair) {
        Intent intent = new Intent(Intents.APSLOGIC_TEMPBASAL_UPDATE);
        intent.putExtra("name", Constants.ParcelName.TempBasalPairParcelName);
        intent.putExtra(Constants.ParcelName.TempBasalPairParcelName, new TempBasalPairParcel(pair));
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }
    public void sendCurrBasalToUI(double basalRate) {
        Intent intent = new Intent(Intents.APSLOGIC_CURRBASAL_UPDATE).putExtra("value", basalRate);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }
    public void sendPredBGToUI(double predictedBG) {
        Intent intent = new Intent(Intents.APSLOGIC_PREDBG_UPDATE).putExtra("value",predictedBG);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }
    public void sendIOBToUI(double iobTotal) {
        Intent intent = new Intent(Intents.APSLOGIC_IOB_UPDATE).putExtra("value",iobTotal);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }
    public void sendCOBToUI(double cobTotal) {
        Intent intent = new Intent(Intents.APSLOGIC_COB_UPDATE).putExtra("value",cobTotal);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

    public void broadcastAPSLogicStatusMessage(String message) {
        Intent intent = new Intent(Intents.APSLOGIC_LOG_MESSAGE);
        intent.putExtra("message", message);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

    // This is used to send messages to the MonitorActivity about APSLogic's actions and decisions
    // Those messages are displayed in the lower part of the MonitorActivity's window
    public void log(String message) {
        dlog(message);
        broadcastAPSLogicStatusMessage(message);
        writeMessageToLogfile(message);
    }

    // This is same as above, but doesn't log to the window
    // That makes it easy to change what appears on the window and what goes to the Android log.
    private static void dlog(String message) {
        Log.d(TAG,message);
    }

    private void writeMessageToLogfile(String message) {
        File log=new File(Environment.getExternalStorageDirectory(),
                mLogfileName);
        try {
            // if file doesnt exists, then create it
            if (!log.exists()) {
                log.createNewFile();
            }
            BufferedWriter out=new BufferedWriter(new FileWriter(log.getAbsolutePath(),true));

            out.write(DateTime.now().toString() + message);
            out.write("\n");
            out.close();
        }
        catch (IOException e) {
            Log.e(TAG, "Exception appending to log file", e);
        }
    }

}