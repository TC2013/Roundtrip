package com.gxwtech.rtdemo.Medtronic.PumpData;

import com.gxwtech.rtdemo.Medtronic.PumpData.records.BolusWizard;
import com.gxwtech.rtdemo.Medtronic.TempBasalEvent;

import java.util.ArrayList;

/**
 * Created by geoff on 6/5/15.
 *
 * This class is inteded to gather what information we've gleaned from the pump history
 * into one place, make it easier to move around.
 *
 */
public class HistoryReport {
    public ArrayList<BolusWizard> mBolusWizardEvents;
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
