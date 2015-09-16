package com.gxwtech.rtdemo.uploader;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.json.JSONObject;
import org.junit.Test;

import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by Fokko on 7-8-15.
 */
public class MqttUploaderTest extends TestCase {

    @Test
    public void testGetInstance() throws Exception {
        MqttUploader uploader = MqttUploader.getInstance();

        DataDefinition.MeterEntry.Builder entryBuilder = DataDefinition.MeterEntry.newBuilder()
                .setMeterBgMgdl(120);

        DataDefinition.MeterEntry entry = entryBuilder.build();

        DataDefinition.G4Download.Builder frameBuilder = DataDefinition.G4Download.newBuilder()
                .setDownloadTimestamp(getTime())
                .setDownloadStatus(DataDefinition.DownloadStatus.SUCCESS)
                .addMeter(entry);

        DataDefinition.G4Download frame = frameBuilder.build();

        uploader.publishMessage(frame.toByteArray());

        uploader.disconnect();
    }

    private String getTime() {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        df.setTimeZone(tz);
        return df.format(new Date());
    }
}