package com.gxwtech.rtdemo.Services;

import android.util.Log;

import com.gxwtech.rtdemo.BGReading;
import com.gxwtech.rtdemo.Medtronic.PumpData.BasalProfile;
import com.gxwtech.rtdemo.Medtronic.PumpData.BasalProfileTypeEnum;
import com.gxwtech.rtdemo.Medtronic.PumpData.TempBasalPair;
import com.gxwtech.rtdemo.Medtronic.ReadBasalTempCommand;
import com.gxwtech.rtdemo.MongoWrapper;
import com.gxwtech.rtdemo.Services.PumpManager.PumpManager;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.LocalTime;
import org.joda.time.Minutes;
import org.joda.time.Seconds;


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
     */


public class APSLogic {
    /* BEGIN settable defaults */
    private static final String TAG = "APSLogic";
    // high_temp_max is maximum units for a temp basal (we ourselves choose this)
    // (this is above the current temp-basal, so if current basal rate is 0.5, we will at most set
    // the temp basal rate to 0.5 + high_temp_max)
    private static final double high_temp_max = 3;

    // pump_high_temp_max is maximum units that the pump will produce for a temp basal (2 U/h for MM722)
    // +++ fixme: read this value from the pump
    private static double pump_high_temp_max = 6.2;

    // target is our target BG - should be in profile!  +++
    //Geoff, the pump may only specify a target range.  May be best to add these values to UI profile (bg_target/min/max and max_IOB). -Toby
    double bg_target = 115;
    // bg minimum target
    double bg_min = bg_target - 20; // i.e. 135
    double bg_max = bg_target + 20; // i.e. 95

    // don't high-temp if IOB > max_IOB
    double max_IOB = 15;

    // Allow low glucose suspend?
    //boolean enable_low_glucose_suspend = false;
    boolean enable_low_glucose_suspend = true;

    double low_glucose_suspend_at = 85; //mg/dl

    // choices here are: dia_0pt5_hour, dia_1pt5_hour, dia_1_hour, dia_2_hour, dia_2pt5_hour, dia_3_hour, dia_3pt5_hour,dia_4_hour, dia_4pt5_hour, dia_5_hour, dia_5pt5_hour
    // Which Duration of Insulin Action table to use?
    // TODO: change this to an enum
    String meal_bolus_dia_table = "dia_4pt5_hour";
    String negative_insulin_dia_table = "dia_2_hour";
    String default_dia_table = "dia_3_hour";
    /* END settable defaults */

    public double iobValueAtAbsTime(DateTime startTime, double insulinUnits,
                                    DateTime valueTime,
                                    DIATables.DIATableEnum dia_table) {
        boolean debug_iobValueAtAbsTime = false;
        if (debug_iobValueAtAbsTime) {
            log("startTime: " + startTime);
            log("valueTime: " + valueTime);
            log("insulinUnits: " + insulinUnits);
            log("dia_table: " + dia_table);
        }
        //int elapsed_minutes = (valueTime - startTime).total_seconds() / 60;
        Minutes minutes = Minutes.minutesBetween(startTime,valueTime); // todo: check this
        int elapsed_minutes = minutes.getMinutes();
        if (debug_iobValueAtAbsTime) {
            log("elapsed minutes since insulin event began: " + elapsed_minutes);
        }
        double rval = insulinUnits * DIATables.insulinPercentRemaining(elapsed_minutes, dia_table) / 100;
        if (debug_iobValueAtAbsTime) {
            log("IOB remaining from event: " + rval);
        }
        return rval;
    }

    // to calculate COB:
    // Easier than IOB, as we assume it is a linear relationship rate=profile.carbs_hr,
    // 20 minutes delayed from consumption
    // Geoff, if possible make the minutes delayed from consumption and editable option in the profile UI. -Toby
    public double cobValueAtAbsTime(DateTime startTime,
                                    double carbGrams,
                                    DateTime valueTime,
                                    double carbs_absorbed_per_hour) {
        // CAR is carbs absorbed per minute
        double CAR = carbs_absorbed_per_hour / 60.0;
        double rval = 0;
        //int elapsed_minutes = (valueTime - startTime).total_seconds() / 60;
        Minutes minutes = Minutes.minutesBetween(startTime,valueTime); // todo: check this
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
        dlog("COB remaining from event: " + rval);
        return rval;
    }

    //Now we define a function to calculate the basal rate at a give time:
    // this depends on having a working basal period/rate table
    public double basal_rate_at_abs_time(LocalTime time_of_day) {
        // TODO: given a time of day, return the basal rate (units per hour)
        return 0.0;
    }

    // isf is the change in blood glucose due to a unit of insulin.
    // varies with time of day (among many other things!)
    public double isf(LocalTime time_of_day) {
        // TODO: given a time of day, return the insulin sensitivity factor
        return 0.0;
    }

    // use this to round to nearest 0.025 (fixme!! should round-down!)
    public double mm_round_rate(double x) {
        return Math.round(x * 40)/40;
    }

    public class Profile {
        public double carbRatio = -10E6; // conversion from XX to XX (insulin to carbs ratio)
    }

    // This function is to be run after CollectData()
    // Here we make a decision about TempBasals, based on all factors
    private void MakeADecision() {
        DateTime now = DateTime.now(); // cache current time, as understood by this device
        log("NOW is " + now.toLocalDateTime().toString());

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

        // In Python we did this:
        log("Getting status of temp basal from pump.");
        TempBasalPair currentTempBasal = getCurrentTempBasalFromPump();
        log(String.format("Temp Basal status: %.02f U, %d minutes remaining.",
                currentTempBasal.mInsulinRate, currentTempBasal.mDurationMinutes));
        log("Getting RTC clock data from pump");
        DateTime rtcDateTime = getRTCTimestampFromPump();
        log("Pump RTC: " + rtcDateTime.toDateTimeISO().toString());

        if (!gotBasalProfiles) {
            log("Getting basal profiles from pump");
            getBasalProfiles();
        }

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
        double bg = mCachedLatestBGReading.mBg;
        /*
        todo: sanity-check latest BG reading (is it less than 39? is it greater than 500?
         */
        double iobTotal = -10E6; // insulin-on-board total, amount of unabsorbed insulin (in Units) in body
        double cobTotal = -10E6; // carbohydrates-on-board total, amount of undigested carbohydrates in body (grams)
        double remainingBGImpact_IOBtotal = -10E6; // BG impact (in mg/dL) of remaining insulin
        double remainingBGImpact_COBtotal = -10E6; // BG impact (in mg/dL) of remaining carbs

        log("TODO: get patient profile (from pump?)");
        Profile profile = new Profile(); // todo: get a real profile

        log(String.format("IOB (total): %.3f",iobTotal));
        log(String.format("COB (total): %.3f",cobTotal));
        log(String.format("remaining BG impact due to insulin events: %.2f", remainingBGImpact_IOBtotal));
        log(String.format("remaining BG impact due to carbs events: %.2f", remainingBGImpact_COBtotal));

        //calc desired BG adjustment
        // We then predict a value for the BG for "sometime in the future, when all IOB and COB are used up".
        // We don't know when that will be (though that would be an interesting algorithm to write).
        // If we see that the eventual BG will be too much or too little, we decide if we can take action.

        double icRatio = profile.carbRatio;
        double eventualBG = bg - remainingBGImpact_IOBtotal + remainingBGImpact_COBtotal;
        log(String.format("eventualBG = Current BG (%.1f) - bg change from IOB (%.1f)  + bg change from COB (%.1f) = %.1f",
                bg,remainingBGImpact_IOBtotal, remainingBGImpact_COBtotal,eventualBG));

        double predictedBG;

        if (bg > eventualBG) {
            // we are predicting a drop in BG
            predictedBG = Math.round(eventualBG);
        } else {
            // we are predicting a rise in BG
            predictedBG = Math.round((bg + eventualBG) / 2);
        }

        // the strangeness in using the average of BG and eventual BG is (I think) due to the logarithmic nature of BG values
        // Remove this once we put AR in place?

        double currentBasalRate = basal_rate_at_abs_time(now.toLocalTime());

        /*
        todo: many places below talk about "for %d more minutes" but reference a total, not remaining duration. fix.
         */

        if (predictedBG < bg_min) {
            log(String.format("Predicting that BG will fall to %.1f which is less than %.1f. Can we do something about it?",
                    predictedBG,bg_min));
            // we predict that the BG will go too low,
            // so lower the current basal rate with a temp-basal
            double newTempBasalRate; // our desired temp basal rate (Units/Hr)
            newTempBasalRate = Math.max(0, currentBasalRate - 2 * (bg_min - predictedBG) / isf(now.toLocalTime()));
            log(String.format("We would like to set temporary rate to %.3f U/h",newTempBasalRate));
            newTempBasalRate = mm_round_rate(newTempBasalRate);
            log(String.format("New temporary rate rounded to %.3f U/h", newTempBasalRate));
            // abide by pump's max. (it will enforce this anyway...)
            newTempBasalRate = Math.min(newTempBasalRate, pump_high_temp_max);
            log(String.format("Pump will limit temporary rate to %.3f U/h",newTempBasalRate));

            if (currentTempBasal.mDurationMinutes > 0) {
                log(String.format("Pump is currently administering a temp basal of %.3f U/h for %d more minutes.",
                        currentTempBasal.mInsulinRate,currentTempBasal.mDurationMinutes));
                if (newTempBasalRate >= currentTempBasal.mInsulinRate) {
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
                }
            }
        } else if (predictedBG < bg_target) {
            log(String.format("Predicting that BG will fall below %.1f, but will stay above %.1f",bg_target,bg_min));
            // we predict that bg will be lower than target, but within "normal" range
            // cancel any high-temp, let any low-temp run
            if (currentTempBasal.mDurationMinutes > 0) {
                log(String.format("Pump is currently administering a temp basal of %.03f U/h for %d more minutes.",
                        currentTempBasal.mInsulinRate,currentTempBasal.mDurationMinutes));
                // a temp basal rate adjustment is running.
                if (currentTempBasal.mInsulinRate > currentBasalRate) {
                    log("Because temp basal rate adjustment is higher than regular basal rate, cancelling temp basal.");
                    // cancel any existing temp basal
                    setTempBasal(0, 0, currentBasalRate);
                } else {
                    log("Current temp basal rate adjustment is lower than regular basal rate, so no action is necessary.");
                }
            }
        } else if (predictedBG > bg_max) {
            log(String.format("Predicting that BG will rise to %.1f which is above %.1f. Can we do something about it?",
                    predictedBG,bg_max));
            // high-temp as required, to get predicted BG down to bg_max
            double fastInsulin;
            fastInsulin = 2 * (predictedBG - bg_max) / isf(now.toLocalTime());
            if (fastInsulin > high_temp_max) {
                log(String.format("Insulin delivery limited (by roundtrip) from %.3f U/h to %.3f U/h.",
                        fastInsulin,high_temp_max));
            }
            TempBasalPair newTempBasal = new TempBasalPair();
            newTempBasal.mInsulinRate = currentBasalRate + Math.min(high_temp_max, fastInsulin);
            log(String.format("We would like to set temporary rate to %.3f U/h",newTempBasal.mInsulinRate));
            newTempBasal.mInsulinRate = mm_round_rate(newTempBasal.mInsulinRate);
            log(String.format("New temporary rate rounded to %.3f U/h",newTempBasal.mInsulinRate));
            // abide by pump"s max. (it will enforce this anyway...)
            newTempBasal.mInsulinRate = Math.min(newTempBasal.mInsulinRate, pump_high_temp_max);
            log(String.format("Pump will limit temporary rate to %.3f U/h",newTempBasal.mInsulinRate));
            if (iobTotal >= max_IOB) {
                log(String.format("IOB (%.1f U) already exceeds maximum allowed (%.1f U), so no course of action available.",iobTotal,max_IOB));
            } else {
                // IOB total is ok, check for currently-administered temp-basal
                if (currentTempBasal.mDurationMinutes > 0) {
                    log(String.format("Pump is currently administering a temp basal of %.3f U/h for %d more minutes.",
                            currentTempBasal.mInsulinRate,currentTempBasal.mDurationMinutes));
                    // add 0.1 to compensate for rounding at pump -- if pump is delivering 2.7 and we want to deliver 2.8, we do nothing.
                    if (newTempBasal.mInsulinRate <= (currentTempBasal.mInsulinRate + 0.1)) {
                        log("recommended rate is less than current rate (or close), therefore we do nothing.");
                    } else {
                        log("Recommended rate is more than current temp basal rate, so over-write old temp basal with new");
                        setTempBasal(newTempBasal.mInsulinRate, 30, currentBasalRate);
                    }
                } else {
                    // Pump is not administering a temp basal currently.
                    if (newTempBasal.mInsulinRate > (currentBasalRate + 0.1)) {
                        log(String.format("If no action is taken, predicting BG will rise above %.1f, so recommending %.3f U/h for 30 min.",
                                bg_max,newTempBasal.mInsulinRate));
                        setTempBasal(newTempBasal.mInsulinRate, 30, currentBasalRate);
                    } else {
                        log("Recommended setting for temporary basal rate adjustment is less than (or close) current basal rate. Doing nothing.");
                    }
                }
            }
        } else if ((predictedBG > bg_target) || ((iobTotal > max_IOB) && (predictedBG > bg_max))) {
            // cancel any low temp, let any high-temp run
            log(String.format("Predicting BG will rise to %.1f which is above %.1f but is below %.1f.",
                    predictedBG,bg_target,bg_max));
            if (currentTempBasal.mDurationMinutes > 0) {
                log(String.format("Pump is currently administering a temp basal of %.3f U/h for %d more minutes.",
                        currentTempBasal.mInsulinRate,currentTempBasal.mDurationMinutes));
                // a temp basal rate adjustment is running
                if (currentTempBasal.mInsulinRate > currentBasalRate) {
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
    public APSLogic() {
        init();
    }
    public void init() {
        // initialize member vars to sane settings.
    }

    public void testModule() {
        MakeADecision();
    }

    public void updateCachedLatestBGReading(BGReading bgr) {
        mCachedLatestBGReading = bgr;
    }

    BasalProfile basalProfileSTD, basalProfileA, basalProfileB;
    boolean gotBasalProfiles = false;
    BGReading mCachedLatestBGReading = new BGReading();
    TempBasalPair mCurrentTempBasal = new TempBasalPair();

    private boolean getBasalProfiles() {
        basalProfileSTD = getPumpManager().getProfile(BasalProfileTypeEnum.STD);
        basalProfileA = getPumpManager().getProfile(BasalProfileTypeEnum.A);
        basalProfileB = getPumpManager().getProfile(BasalProfileTypeEnum.B);
        gotBasalProfiles = (basalProfileSTD != null) && (basalProfileA != null) && (basalProfileB != null);
        return gotBasalProfiles;
    }

    // use this to get access to the pump manager.
    // Don't cache the pumpManager, as it can be reinstantiated when the USB device is (un)plugged.
    // todo: this may cause crashes when the carelink is (un)plugged.  fix in RTDemoService? how?
    private PumpManager getPumpManager() {
        return RTDemoService.getInstance().getPumpManager();
    }

    // When this method succeeds, it also contacts the database to add the new treatment.
    // Need curr_basal to calculate if this is a negative insulin event
    public void setTempBasal(double rateUnitsPerHour, int periodMinutes, double currBasalRate) {
        log(String.format("<Set Temp Basal: rate=%.3f, minutes=%d>",rateUnitsPerHour,periodMinutes));
    }

    //Geoff, rather than storing the profile in Mongo, it would be ideal to put it into the UI
    public Profile getProfile() {
        return new Profile();
    }

    // Get currently used Basals Profile from the pump.
    // We want to fetch these once on startup, then used cached copies.
    public BasalProfile getCurrentBasalProfile() { return new BasalProfile(); }

    // This function is run when we receive a reading from xDrip, broadcast as an Intent
    public void receiveXDripBGReading(BGReading bgr) {
        // sanity check and cache the latest reading, used only if xDrip samples are enabled.
    }

    // This command retrieves the CURRENTLY ACTIVE temp basal from the pump
    // This command can "sleep" for up to 20 seconds while running.
    public TempBasalPair getCurrentTempBasalFromPump() {
        TempBasalPair rval;
        rval = getPumpManager().getCurrentTempBasal();
        return rval;
    }

    public DateTime getRTCTimestampFromPump() {
        DateTime dt = getPumpManager().getRTCTimestamp();
        return dt;
    }

    // This is used to send messages to the MonitorActivity about APSLogic's actions and decisions
    // Those messages are displayed in the lower part of the MonitorActivity's window
    // This is a call to RTDemoService so that I can keep Android stuff out of this class.
    private void log(String message) {
        dlog(message);
        RTDemoService.getInstance().broadcastAPSLogicStatusMessage(message);
    }

    // This is same as above, but doesn't log to the window
    // That makes it easy to change what appears on the window and what goes to the Android log.
    private void dlog(String message) {
        Log.d(TAG,message);
    }

}
