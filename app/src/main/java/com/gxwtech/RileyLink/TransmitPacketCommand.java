package com.gxwtech.RileyLink;

/**
 * Created by geoff on 10/20/15.
 */
public class TransmitPacketCommand implements RileyLinkCommand {
    byte[] mPacket;
    public TransmitPacketCommand(RileyLink rl, byte[] packet) {
        mPacket = packet;
    }
    public RileyLinkCommandResult run(RileyLink rileylink, int timeout_millis) {
        RileyLinkCommandResult rval = null;
        boolean writeOK = rileylink.write(mPacket);
        if (writeOK) {
            rval = new RileyLinkCommandResult(mPacket, RileyLinkCommandResult.STATUS_OK, "Packet sent");
        } else {
            rval = new RileyLinkCommandResult(mPacket, RileyLinkCommandResult.STATUS_ERROR, "RileyLink reports error sending packet");
        }
        return rval;
    }
}
