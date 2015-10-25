package com.gxwtech.RileyLink;

/**
 * Created by geoff on 7/27/15.
 */
public interface RileyLinkCommand {
    public RileyLinkCommandResult run(RileyLink rileylink, int timeout_millis);
}
