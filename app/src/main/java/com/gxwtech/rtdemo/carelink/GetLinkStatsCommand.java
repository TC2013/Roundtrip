package com.gxwtech.rtdemo.carelink;

/**
 * Created by geoff on 5/5/15.
 */
public class GetLinkStatsCommand extends CarelinkCommand {
    public byte whichStats; // 0 is radio stats, 1 is usb stats

    public GetLinkStatsCommand(byte which) {
        init(CarelinkCommandEnum.CMD_C_LINK_STATS);
        whichStats = which;
    }

    protected byte[] preparePacket() {
        super.preparePacket();
        mRawPacket[1] = whichStats;
        return mRawPacket;
    }

    public void parse() {
        // ignore, for now.
    }
}
