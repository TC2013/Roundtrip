package com.gxwtech.rtdemo;

/**
 * Created by geoff on 10/6/15.
 */

// This is a quick-fix to allow code sharing.
// If responseObject is null, then errorMsg should have a reason.
// else, you may cast responseObject to your expected type.
public class DownloadResponse {
    public Object responseObject;
    public String errorMessage;
    public DownloadResponse() {responseObject = null; errorMessage = null;}
}
