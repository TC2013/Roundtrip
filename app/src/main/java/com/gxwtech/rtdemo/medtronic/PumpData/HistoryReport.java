package com.gxwtech.rtdemo.medtronic.PumpData;

import com.gxwtech.rtdemo.medtronic.PumpData.records.BolusWizard;
import com.gxwtech.rtdemo.medtronic.TempBasalEvent;

import java.util.ArrayList;

/**
 * Created by geoff on 6/5/15.
 * <p/>
 * This class is inteded to gather what information we've gleaned from the pump history
 * into one place, make it easier to move around.
 */
public class HistoryReport {
    public ArrayList<BolusWizard> mBolusWizardEvents;
    //public ArrayList<TempBasalEvent> mBasalEvents;
    public ArrayList<TempBasalEvent> mBasalEvents;

    public HistoryReport() {
        mBolusWizardEvents = new ArrayList<>();
        mBasalEvents = new ArrayList<>();
    }

    public void addBolusWizardEvent(BolusWizard event) {
        mBolusWizardEvents.add(event);
    }

    public void addTempBasalEvent(TempBasalEvent event) {
        mBasalEvents.add(event);
    }
}
