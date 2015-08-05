package com.gxwtech.rtdemo.medtronic.PumpData.records;


//import com.google.common.primitives.UnsignedBytes;

import android.util.Log;

import com.gxwtech.rtdemo.medtronic.PumpModel;

public class BolusWizard extends TimeStampedRecord {
    private final static String TAG = "BolusWizard";

    private double correction;
    private long bg;
    private int carbInput;
    private double icRatio;
    private int sensitivity;
    private int bgTargetLow;
    private int bgTargetHigh;
    private double bolusEstimate;
    private double foodEstimate;
    private double unabsorbedInsulinTotal;

    public BolusWizard() {
        correction = (double) 0.0;
        bg = 0;
        carbInput = 0;
        icRatio = 0.0;
        sensitivity = 0;
        bgTargetLow = 0;
        bgTargetHigh = 0;
        bolusEstimate = 0.0;
        foodEstimate = 0.0;
        unabsorbedInsulinTotal = 0.0;
    }

    public double getCorrection() {
        return correction;
    }

    public long getBG() {
        return bg;
    }

    public int getCarbInput() {
        return carbInput;
    }

    public double getICRatio() {
        return icRatio;
    }

    public int getSensitivity() {
        return sensitivity;
    }

    public int getBgTargetLow() {
        return bgTargetLow;
    }

    public double getBolusEstimate() {
        return bolusEstimate;
    }

    public double getFoodEstimate() {
        return foodEstimate;
    }

    public double getUnabsorbedInsulinTotal() {
        return unabsorbedInsulinTotal;
    }

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

    protected boolean decode(byte[] data) {
        if (!super.decode(data)) {
            return false;
        }
        int bodyIndex = headerSize + timestampSize;

        // GGW: is the BG number really broken up into a byte in the header and four bits in the body?
        bg = (((data[bodyIndex + 1] & 0x0F) << 8) | data[1]);
        carbInput = readUnsignedByte(data[bodyIndex]);
        if (model == PumpModel.MM523) {
            correction = readUnsignedByte(data[bodyIndex]) / 40.0f;
            icRatio = readUnsignedByte(data[bodyIndex + 14]) / 10.0f;
            sensitivity = readUnsignedByte(data[bodyIndex + 4]);
            bgTargetLow = readUnsignedByte(data[bodyIndex + 5]);
            bgTargetHigh = readUnsignedByte(data[bodyIndex + 3]);
            bolusEstimate = readUnsignedByte(data[bodyIndex + 13]) / 40.0f;
            foodEstimate = readUnsignedByte(data[bodyIndex + 8]) / 40.0f;
            unabsorbedInsulinTotal = readUnsignedByte(data[bodyIndex + 11]) / 40.0f;
        } else {
            correction = (readUnsignedByte(data[bodyIndex + 7]) + (data[bodyIndex + 5] & 0x0F)) / 10.0f;
            icRatio = readUnsignedByte(data[bodyIndex + 2]);
            sensitivity = readUnsignedByte(data[bodyIndex + 3]);
            bgTargetLow = readUnsignedByte(data[bodyIndex + 4]);
            bgTargetHigh = readUnsignedByte(data[bodyIndex + 12]);
            bolusEstimate = readUnsignedByte(data[bodyIndex + 11]) / 10.0f;
            foodEstimate = readUnsignedByte(data[bodyIndex + 6]) / 10.0f;
            unabsorbedInsulinTotal = readUnsignedByte(data[bodyIndex + 9]) / 10.0f;
        }
        //logRecord();
        return true;
    }

    @Override
    public void logRecord() {
        Log.i(TAG, String.format("Time: %s RecordType: %s Bg: %d Carb Input: %d Correction: %.2f icRatio: %.2f Sensitivity: %d BG Target High: %d BG Target Low: %d Bolus Estimate: %.2f Food Estimate: %.2f Unabsorbed Insulin Total: %.2f",
                timeStamp.toString(), recordTypeName, bg, carbInput, correction, icRatio, sensitivity, bgTargetHigh, bgTargetLow, bolusEstimate, foodEstimate, unabsorbedInsulinTotal));
    }
}
