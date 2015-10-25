package com.gxwtech.RileyLink;

/**
 * Created by geoff on 10/20/15.
 */
public class RileyLinkCommandResult {

    public static final int STATUS_OK = 600;
    public static final int STATUS_NO_RESPONSE = 601;
    public static final int STATUS_ERROR = 602;
    public static final int STATUS_NONE = 603;

    public byte mPacket[];
    public int mStatus;
    public String mStatusMessage;
    public RileyLinkCommandResult() {
        init(null, STATUS_NONE, "Command has not been run");
    }
    public RileyLinkCommandResult(byte[] packet, int status, String statusMessage) {
        init(packet,status,statusMessage);
    }
    protected void init(byte[] packet, int status, String statusMessage) {
        mPacket = packet;
        mStatus = status;
        mStatusMessage = statusMessage;
    }

}
