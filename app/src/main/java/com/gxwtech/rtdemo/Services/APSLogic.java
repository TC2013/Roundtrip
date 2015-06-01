package com.gxwtech.rtdemo.Services;

import android.util.Log;

import com.gxwtech.rtdemo.Medtronic.PumpData.BasalProfile;

import java.util.Calendar;

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
public class APSLogic {
    private static final String TAG = "APSLogic";
    // high_temp_max is maximum units for a temp basal (we ourselves choose this)
    // (this is above the current temp-basal, so if current basal rate is 0.5, we will at most set
    // the temp basal rate to 0.5 + high_temp_max)
    private static final double high_temp_max = 3;

    // pump_high_temp_max is maximum units that the pump will produce for a temp basal (2 U/h for MM722)
    // +++ fixme: read this value from the pump
    private double pump_high_temp_max = 6.2;

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


    public APSLogic() {
        init();
    }

    public void init() {

    }

    // When this method succeeds, it also contacts the database to add the new treatment.
    // Need curr_basal to calculate if this is a negative insulin event
    public void setTempBasal(double rateUnitsPerHour, int periodMinutes, double currBasal) {
    }

    //Geoff, rather than storing the profile in Mongo, it would be ideal to put it into the UI
    public Profile getProfile() {
        return new Profile();
    }

    // Get currently used Basals Profile from the pump.
    // We want to fetch these once on startup, then used cached copies.
    public BasalProfile getCurrentBasalProfile() { return new BasalProfile(); }

    //startTime and valueTime should be Calendars, I think
    public double iobValueAtAbsTime(int startTime, double insulinUnits,
                                    int valueTime,
                                    DIATables.DIATableEnum dia_table) {
        boolean debug_iobValueAtAbsTime = false;
        if (debug_iobValueAtAbsTime) {
            Log.d(TAG, "startTime: " + startTime);
            Log.d(TAG, "valueTime: " + valueTime);
            Log.d(TAG, "insulinUnits: " + insulinUnits);
            Log.d(TAG, "dia_table: " + dia_table);
        }
        //int elapsed_minutes = (valueTime - startTime).total_seconds() / 60;
        int elapsed_minutes = (valueTime - startTime); // in minutes
        if (debug_iobValueAtAbsTime) {
            Log.d(TAG, "elapsed minutes since insulin event began: " + elapsed_minutes);
        }
        double rval = insulinUnits * DIATables.insulinPercentRemaining(Math.round(elapsed_minutes), dia_table) / 100;
        if (debug_iobValueAtAbsTime) {
            Log.d(TAG, "IOB remaining from event: " + rval);
        }
        return rval;
    }

    // to calculate COB:
    // Easier than IOB, as we assume it is a linear relationship rate=profile.carbs_hr,
    // 20 minutes delayed from consumption
    // Geoff, if possible make the minutes delayed from consumption and editable option in the profile UI. -Toby
    public double cobValueAtAbsTime(int startTime,
                                    double carbGrams,
                                    int valueTime,
                                    double carbs_absorbed_per_hour) {
        // CAR is carbs absorbed per minute
        double CAR = carbs_absorbed_per_hour / 60.0;
        double rval = 0;
        //int elapsed_minutes = (valueTime - startTime).total_seconds() / 60;
        int elapsed_minutes = (valueTime - startTime);
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
        //Log.d(TAG,"COB remaining from event: " + rval);
        return rval;
    }
    //TODO: print('Reading temp basal data from ' + TBSO_FILE)
    //TODO: #print('Reading clock data from ' + CLOCK_FILE)
    //TODO: #print('Reading basal data from ' + BASALS_FILE)

    //Now we define a function to calculate the basal rate at a give time:
    // this depends on having a working basal period/rate table
    public double basal_rate_at_abs_time(double time_of_day) {
        // TODO: given a time of day, return the basal rate (units per hour)
        return 0.0;
    }

    // isf is the change in blood glucose due to a unit of insulin.
    // varies with time of day (among many other things!)
    public double isf(double time_of_day) {
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

    public class TempBasal {
        public double rate;
        public double duration;
    }
    public TempBasal getCurrentTempBasalFromPump() {
        // todo: fix me
        return new TempBasal();
    }

    public void MakeADecision() {
        // todo: fixme
        int now = 0;
        // todo: get these values from pump history, MongoDB, CGM, etc.
        double bg = -10E6; // blood glucose
        double iobTotal = -10E6; // insulin-on-board total, amount of unabsorbed insulin (in Units) in body
        double cobTotal = -10E6; // carbohydrates-on-board total, amount of undigested carbohydrates in body (grams)
        double remainingBGImpact_IOBtotal = -10E6; // BG impact (in mg/dL) of remaining insulin
        double remainingBGImpact_COBtotal = -10E6; // BG impact (in mg/dL) of remaining carbs
        Profile profile = new Profile(); // todo: get a real profile

        Log.d(TAG,String.format("IOB (total): %.3f",iobTotal));
        Log.d(TAG,String.format("COB (total): %.3f",cobTotal));
        Log.d(TAG,String.format("remaining BG impact due to insulin events: %.2f", remainingBGImpact_IOBtotal));
        Log.d(TAG,String.format("remaining BG impact due to carbs events: %.2f", remainingBGImpact_COBtotal));

        //calc desired BG adjustment
        // We then predict a value for the BG for "sometime in the future, when all IOB and COB are used up".
        // We don't know when that will be (though that would be an interesting algorithm to write).
        // If we see that the eventual BG will be too much or too little, we decide if we can take action.

        double icRatio = profile.carbRatio;
        double eventualBG = bg - remainingBGImpact_IOBtotal + remainingBGImpact_COBtotal;
        Log.d(TAG,String.format("eventualBG = Current BG (%d) - bg change from IOB (%d)  + bg change from COB (%d) = %d",
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

        // if most recent reading is more than ten minutes old
        int cgm_elapsed = 0;
        if (cgm_elapsed/*.total_seconds()*/ / 60 > 10) {
            Log.d(TAG, String.format("Most recent CGM reading is %(f)2f minutes old. Quitting.",
                    cgm_elapsed/*.total_seconds() / 60)*/));
            //exit(-2);
        }

        double currentBasalRate = basal_rate_at_abs_time(now);
        TempBasal currentTempBasal = getCurrentTempBasalFromPump();

        if (predictedBG < bg_min) {
            Log.d(TAG, String.format("Predicting that BG will fall to %g which is less than %g. Can we do something about it?",
                    predictedBG,bg_min));
            // we predict that the BG will go too low,
            // so lower the current basal rate with a temp-basal
            double newTempBasalRate; // our desired temp basal rate (Units/Hr)
            newTempBasalRate = Math.max(0, currentBasalRate - 2 * (bg_min - predictedBG) / isf(now));
            Log.d(TAG, String.format("We would like to set temporary rate to %g U/h",newTempBasalRate));
            newTempBasalRate = mm_round_rate(newTempBasalRate);
            Log.d(TAG, String.format("New temporary rate rounded to %g U/h", newTempBasalRate));
            // abide by pump's max. (it will enforce this anyway...)
            newTempBasalRate = Math.min(newTempBasalRate, pump_high_temp_max);
            Log.d(TAG, String.format("Pump will limit temporary rate to %g U/h",newTempBasalRate));

            if (currentTempBasal.duration > 0) {
                Log.d(TAG, String.format("Pump is currently administering a temp basal of %g U/h for %d more minutes.",
                        currentTempBasal.rate,currentTempBasal.duration));
                if (newTempBasalRate >= currentTempBasal.rate) {
                    Log.d(TAG, "Pump is already doing its best here, so leave it alone.");
                } else {
                    Log.d(TAG, "Pump is already administering a temp basal, but we want a lower one.");
                    // set duration of temp basal to the minimum allowed rate.
                    setTempBasal(newTempBasalRate, 30, currentBasalRate);
                    // 30 minutes is minimum for minimed
                }
            } else {
                Log.d(TAG, String.format("Pump is not currently administering a temp basal.  Current basal rate is %g U/h",currentBasalRate));
                if (newTempBasalRate < currentBasalRate) {
                    // set duration of temp basal to the minimum allowed rate.
                    setTempBasal(newTempBasalRate, 30, currentBasalRate);
                    // 30 minutes is minimum for Mini-Med
                }
            }
        } else if (predictedBG < bg_target) {
            Log.d(TAG, String.format("Predicting that BG will fall below %g, but will stay above %g.",bg_target,bg_min));
            // we predict that bg will be lower than target, but within "normal" range
            // cancel any high-temp, let any low-temp run
            if (currentTempBasal.duration > 0) {
                Log.d(TAG, String.format("Pump is currently administering a temp basal of %g U/h for %d more minutes.",
                        currentTempBasal.rate,currentTempBasal.duration));
                // a temp basal rate adjustment is running.
                if (currentTempBasal.rate > currentBasalRate) {
                    Log.d(TAG, "Because temp basal rate adjustment is higher than regular basal rate, cancelling temp basal.");
                    // cancel any existing temp basal
                    setTempBasal(0, 0, currentBasalRate);
                } else {
                    Log.d(TAG, "Current temp basal rate adjustment is lower than regular basal rate, so no action is necessary.");
                }
            }
        } else if (predictedBG > bg_max) {
            Log.d(TAG, String.format("Predicting that BG will rise to %g which is above %g. Can we do something about it?",
                    predictedBG,bg_max));
            // high-temp as required, to get predicted BG down to bg_max
            double fastInsulin;
            fastInsulin = 2 * (predictedBG - bg_max) / isf(now);
            if (fastInsulin > high_temp_max) {
                Log.d(TAG, String.format("Insulin delivery limited (by roundtrip) from %g U/h to %g U/h.",
                        fastInsulin,high_temp_max));
            }
            TempBasal newTempBasal = new TempBasal();
            newTempBasal.rate = currentBasalRate + Math.min(high_temp_max, fastInsulin);
            Log.d(TAG, String.format("We would like to set temporary rate to %g U/h",newTempBasal.rate));
            newTempBasal.rate = mm_round_rate(newTempBasal.rate);
            Log.d(TAG, String.format("New temporary rate rounded to %g U/h",newTempBasal.rate));
            // abide by pump"s max. (it will enforce this anyway...)
            newTempBasal.rate = Math.min(newTempBasal.rate, pump_high_temp_max);
            Log.d(TAG, String.format("Pump will limit temporary rate to U/h",newTempBasal.rate));
            if (iobTotal >= max_IOB) {
                Log.d(TAG, String.format("IOB (%g U) already exceeds maximum allowed (%g U), so no course of action available.",iobTotal,max_IOB));
            } else {
                // IOB total is ok, check for currently-administered temp-basal
                if (currentTempBasal.duration > 0) {
                    Log.d(TAG, String.format("Pump is currently administering a temp basal of %g U/h for %d more minutes.",
                            currentTempBasal.rate,currentTempBasal.duration));
                    // add 0.1 to compensate for rounding at pump -- if pump is delivering 2.7 and we want to deliver 2.8, we do nothing.
                    if (newTempBasal.rate <= (currentTempBasal.rate + 0.1)) {
                        Log.d(TAG, "recommended rate is less than current rate (or close), therefore we do nothing.");
                    } else {
                        Log.d(TAG, "Recommended rate is more than current temp basal rate, so over-write old temp basal with new");
                        setTempBasal(newTempBasal.rate, 30, currentBasalRate);
                    }
                } else {
                    // Pump is not administering a temp basal currently.
                    if (newTempBasal.rate > (currentBasalRate + 0.1)) {
                        Log.d(TAG, String.format("If no action is taken, predicting BG will rise above %g, so recommending %g U/h for 30 min.",
                                bg_max,newTempBasal.rate));
                        setTempBasal(newTempBasal.rate, 30, currentBasalRate);
                    } else {
                        Log.d(TAG, "Recommended setting for temporary basal rate adjustment is less than (or close) current basal rate. Doing nothing.");
                    }
                }
            }
        } else if ((predictedBG > bg_target) || ((iobTotal > max_IOB) && (predictedBG > bg_max))) {
            // cancel any low temp, let any high-temp run
            Log.d(TAG, String.format("Predicting BG will rise to %g which is above %g but is below %g.",
                    predictedBG,bg_target,bg_max));
            if (currentTempBasal.duration > 0) {
                Log.d(TAG, String.format("Pump is currently administering a temp basal of %g U/h for  more minutes.",
                        currentTempBasal.rate,currentTempBasal.duration));
                // a temp basal rate adjustment is running
                if (currentTempBasal.rate > currentBasalRate) {
                    // it is a high-temp, so let it run
                    Log.d(TAG, "Predicted BG is in the higher-range, current temp-basal is slightly higher, so let it run");
                } else {
                    Log.d(TAG, "Predicted BG is in the higher range, so cancel lower temp-basal");
                    setTempBasal(0, 0, currentBasalRate);
                }
            } else {
                Log.d(TAG, "No temp-basal is currently running. Good.");
            }
        } else {
            Log.d(TAG, "how did we get here?");
        }
    }
}
