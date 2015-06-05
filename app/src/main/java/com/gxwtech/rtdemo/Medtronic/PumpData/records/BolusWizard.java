package com.gxwtech.rtdemo.Medtronic.PumpData.records;


//import com.google.common.primitives.UnsignedBytes;
import android.util.Log;

import com.gxwtech.rtdemo.Medtronic.PumpModel;

public class BolusWizard extends TimeStampedRecord {
    private final static String TAG = "BolusWizard";

    private double correction;
    private long bg;
    private int carbInput;
    private double carbRatio;
    private int sensitivity;
    private int bgTargetLow;
    private int bgTargetHigh;
    private double bolusEstimate;
    private double foodEstimate;
    private double unabsorbedInsulinTotal;

    public BolusWizard() {
        correction = (double)0.0;
        bg = 0;
        carbInput = 0;
        carbRatio = 0.0;
        sensitivity = 0;
        bgTargetLow = 0;
        bgTargetHigh = 0;
        bolusEstimate = 0.0;
        foodEstimate = 0.0;
        unabsorbedInsulinTotal = 0.0;
    }

    public double getCorrection() { return correction; }
    public long getBG() { return bg; }
    public int getCarbInput() { return carbInput; }
    public double getCarbRatio() { return carbRatio; }
    public int getSensitivity() { return sensitivity; }
    public int getBgTargetLow() { return bgTargetLow; }
    public double getBolusEstimate() { return bolusEstimate; }
    public double getFoodEstimate() { return foodEstimate; }
    public double getUnabsorbedInsulinTotal() { return unabsorbedInsulinTotal; }

    public boolean collectRawData(byte[] data, PumpModel model) {
        super.collectRawData(data, model);
        if (model.ordinal() < PumpModel.MM523.ordinal()) {
            bodySize = 13;
        } else if (model.ordinal() >= PumpModel.MM523.ordinal()) {
            bodySize = 15;
        }
        calcSize();
        return decode(data);
    }

    protected int toInt(byte b) {
        return (int)b;
    }

    protected boolean decode(byte[] data) {
        if (!super.decode(data)) {
            return false;
        }
        int bodyIndex = headerSize + timestampSize;

        bg = (((data[bodyIndex + 1] & 0x0F) << 8) | toInt(data[1]));
        carbInput = data[bodyIndex];
        if (model == PumpModel.MM523) {
            correction = toInt(data[bodyIndex]) / 40.0f;
            carbRatio = toInt(data[bodyIndex + 14]) / 10.0f;
            sensitivity = toInt(data[bodyIndex + 4]);
            bgTargetLow = data[bodyIndex + 5];
            bgTargetHigh = data[bodyIndex + 3];
            bolusEstimate = data[bodyIndex + 13] / 40.0f;
            foodEstimate = data[bodyIndex + 8] / 40.0f;
            unabsorbedInsulinTotal = data[bodyIndex + 11] / 40.0f;
        } else {
            correction = (toInt(data[bodyIndex + 7]) + data[bodyIndex + 5] & 0x0F) / 10.0f;
            carbRatio = toInt(data[bodyIndex + 2]);
            sensitivity = toInt(data[bodyIndex + 3]);
            bgTargetLow = data[bodyIndex + 4];
            bgTargetHigh = data[bodyIndex + 12];
            bolusEstimate = data[bodyIndex + 11] / 10.0f;
            foodEstimate = data[bodyIndex + 6] / 10.0f;
            unabsorbedInsulinTotal = data[bodyIndex + 9] / 10.0f;
        }
        Log.e(TAG,"SUCCESS! Parsed BolusWizard Record");
        logRecord();
        return true;
    }

    @Override
    public void logRecord() {
        Log.i(TAG,String.format("Time: %s RecordType: %s Bg: %d Carb Input: %d Correction: %02f Carb Ratio: %02f Sensitivity: %d BG Target High: %d BG Target Low: %d Bolus Estimate: %02f Food Estimate: %02f Unabsorbed Insulin Total: %02f",
                timeStamp.toString(), recordTypeName, bg, carbInput, correction, carbRatio, sensitivity, bgTargetHigh, bgTargetLow, bolusEstimate, foodEstimate, unabsorbedInsulinTotal));
    }
}
