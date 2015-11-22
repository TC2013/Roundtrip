package com.gxwtech.rtdemo;

import android.util.Log;

import com.google.common.base.Joiner;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.AbstractHttpMessage;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.joda.time.Duration.standardSeconds;

public class RestV1Wrapper /* extends AbstractRestUploader */ {
    private static final String TAG = "RestV1Wrapper";

    private final URI uri;
    private HttpClient client;
    private final String secret; // needed for uploads only.


    public class BGReadingResponse {
        public boolean isOk;
        public String errorMessage;
        public BGReading bgReading;
        public BGReadingResponse() { isOk = false; errorMessage = null; bgReading = null;}
    }


    public RestV1Wrapper(URI uri) {
        this.uri = RestUriUtils.removeToken(uri);
        checkNotNull(uri);
        checkArgument(RestUriUtils.hasToken(uri), "Rest API v1 requires a token.");
        secret = RestUriUtils.generateSecret(uri.getUserInfo());
    }

    public URI getUri() {
        return uri;
    }

    public HttpClient getClient() {
        if (client != null) {
            return client;
        }
        client = new DefaultHttpClient();
        return client;
    }

    public void setClient(HttpClient client) {
        this.client = client;
    }

    public static final DateTime DEXCOM_EPOCH = new DateTime(2009, 1, 1, 0, 0, 0, 0).withZone(DateTimeZone.UTC);

    // TODO: probably not the right way to do this but it seems to do the trick. Need to revisit this to fully understand what is going on during DST change
    public static DateTime receiverTimeToDateTime(long deltaInSeconds) {
        int offset = DateTimeZone.getDefault().getOffset(DEXCOM_EPOCH) - DateTimeZone.getDefault().getOffset(Instant.now());
        return DEXCOM_EPOCH.plus(offset).plus(standardSeconds(deltaInSeconds)).withZone(DateTimeZone.UTC);
    }

    public static Date receiverTimeToDate(long delta) {
        return receiverTimeToDateTime(delta).toDate();
    }


    private JSONObject toJSONObject(DBTempBasalEntry record) throws JSONException {
        JSONObject json = new JSONObject();
        DateTime timestamp = new DateTime(record.mTimestamp.toDate());
        json.put("enteredBy", "Roundtrip");
        json.put("eventType", "Temp Basal");
        //json.put("datetime", timestamp.toString());
        json.put("datetime", ISODateTimeFormat.dateTime().print(timestamp));
        json.put("insulin",String.format("%.3f", record.mRelativeInsulin));
        json.put("durationMin",String.format("%d",record.mDurationMinutes));
        json.put("created_at",record.mTimestamp.toDateTime(DateTimeZone.UTC).toString());
        json.put("notes","Start: " + record.startTime + "\nEnd: " + record.endTime+"\n");
        return json;

        /*
        BasicDBObject myDBObj = new BasicDBObject("enteredBy",enteredBy)
                .append("eventType",eventType)
                .append("date",mTimestamp.getMillis())
                        //.append("date", String.format("%d", mTimestamp.getMillis()))
                .append("insulin", String.format("%.3f", mRelativeInsulin))
                .append("durationMin", String.format("%d", mDurationMinutes))
                .append("created_at", mTimestamp.toDateTime(DateTimeZone.UTC).toString())
                .append("notes", "Start: " + startTime + "\nEnd: " + endTime + "\n");
                */

    }

    protected void setExtraHeaders(AbstractHttpMessage post) {
        post.setHeader("api-secret", secret);
    }



    protected boolean doPost(String endpoint, JSONObject jsonObject) throws IOException {
        HttpPost httpPost = new HttpPost(Joiner.on('/').join(uri.toString(), endpoint));
        httpPost.addHeader("Content-Type", "application/json");
        httpPost.addHeader("Accept", "application/json");
        setExtraHeaders(httpPost);
        httpPost.setEntity(new StringEntity(jsonObject.toString()));
        HttpResponse response = getClient().execute(httpPost);
        int statusCodeFamily = response.getStatusLine().getStatusCode() / 100;
        response.getEntity().consumeContent();
        return statusCodeFamily == 2;
    }


    public boolean uploadTreatment(DBTempBasalEntry record) throws IOException {
        try {
            // TODO(trhodeos): in Uploader.java, this method still used 'entries' as the endpoint,
            // but this seems like a bug to me.
            return doPost("treatments", toJSONObject(record));
        } catch (JSONException e) {
            Log.e("RestV1Wrapper","Could not create JSON object for rest v1 treatment record:"+e.toString());
            return false;
        }
    }

    public DownloadResponse doJSONDownload(String uriString) {
        DownloadResponse rval = new DownloadResponse();
        InputStream inputStream = null;
        String json = "";
        JSONObject jObj = null;
        boolean isOk = false;
        if (null!=uriString) {
            try {
                HttpGet httpGet = new HttpGet(new URI(uriString));
                Log.d(TAG, "downloading URL: " + uriString);
                // get the data from the site
                HttpResponse response = getClient().execute(httpGet);
                int statusCodeFamily = response.getStatusLine().getStatusCode() / 100;
                isOk = (statusCodeFamily == 2);
                rval.errorMessage = null;
                if (!isOk) {
                    rval.errorMessage = response.getStatusLine().getReasonPhrase();
                }
                inputStream = response.getEntity().getContent();
            } catch (URISyntaxException e) {
                isOk = false;
                rval.errorMessage = "doDownload failed: invalid URI: " + uriString;
            } catch (IOException e) {
                isOk = false;
                rval.errorMessage = "doDownload failed: " + e.toString();
            } catch (IllegalArgumentException e) {
                isOk = false;
                rval.errorMessage = "doDownload failed (invalid uri)" + e.toString();
            }

            if (!isOk) {
                return rval;
            }

            // try to read the string
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        inputStream, "iso-8859-1"), 8);
                StringBuilder sb = new StringBuilder();
                String line = null;
                while ((line = reader.readLine()) != null) {
                    sb.append(line + "\n");
                }
                inputStream.close();
                json = sb.toString();
            } catch (Exception e) {
                isOk = false;
                rval.errorMessage = "Error converting result " + e.toString();
            }

            // try parse the string to a JSON object
            boolean isJSONObject = true;
            try {
                jObj = new JSONObject(json);
                rval.responseObject = jObj;
                isOk = true;
                rval.errorMessage = null;
            } catch (JSONException e) {
                // It might have been an "Array" instead of an "Object", so do not give up yet.
                isJSONObject = false;
            }
            if (!isJSONObject) { // then maybe it's a JSONArray (sigh)
                try {
                    JSONArray jra = new JSONArray(json);
                    rval.responseObject = jra;
                    isOk = true;
                    rval.errorMessage = null;
                } catch (JSONException e) {
                    // ok, now we give up.
                    isOk = false;
                    rval.responseObject = null;
                    rval.errorMessage = "Error parsing data " + e.toString();
                    return rval;
                }
            }

        } else {
            isOk = false;
            rval.errorMessage = "Invalid URI";
        }

        return rval;
    }

    // this uri will get the latest entry in json format
    // https://YOUR_SITE.azurewebsites.net/api/v1/entries/.json?count=1

    // get the last ten treatments
    // https://YOUR_SITE.azurewebsites.net/api/v1/treatments/.json?count=10

    // todo: rewrite this using normal api/v1, rather than cheating and using pebble

    public BGReadingResponse doDownloadBGReading() {
        BGReadingResponse rval = new BGReadingResponse();
        rval.isOk = true;
        if (null!=uri) {
            String pebbleUrl = uri.getScheme() + "://" + uri.getHost() + "/pebble";
            DownloadResponse dr = doJSONDownload(pebbleUrl);
            if (null!=dr.responseObject) {
                // try to get our data from the json object
                try {
                    JSONObject jObj = (JSONObject)(dr.responseObject);
                    JSONObject values = jObj.getJSONArray("bgs").getJSONObject(0);
                    String sgvString = values.getString("sgv");
                    int sgv = Integer.parseInt(sgvString);
                    long datetime = values.getLong("datetime");
                    rval.bgReading = new BGReading(new DateTime(datetime),sgv);
                    // need to check Time Zone?
                } catch (JSONException e) {
                    rval.isOk = false;
                    rval.errorMessage = "Error parsing JSON: " + e.toString();
                    return rval;
                }
            } else {
                rval.isOk = false;
                rval.errorMessage = dr.errorMessage;
            }
        } else {
            rval.isOk = false;
            rval.errorMessage = "Invalid URI";
        }
        return rval;
    }

    /*
    Example of a JSON record of a recent BG Entry (pebble):
    { "status":[{"now":1444069231017}],
      "bgs":[
        {"sgv":"158",
         "trend":4,
         "direction":"Flat",
         "datetime":1444069169745,
         "filtered":181000,
         "unfiltered":179000,
         "noise":1,"bgdelta":-1,"battery":"78","iob":"0"}],"cals":[{"slope":701,"intercept":38694,"scale":0.9}]}
     */

    /*
    [
     {"_id":"5613dc2cbe9f81400ebe1aa5","enteredBy":"Roundtrip","eventType":"BG Check","notes":"Example Treatment Entry 2","created_at":"2015-10-06T14:35:00.000Z"},
     {"_id":"5613dbfdbe9f81400ebe1aa3","enteredBy":"Roundtrip","eventType":"Note","notes":"Example Treatment Entry","created_at":"2015-10-06T14:33:00.000Z"}
    ]
    */
    // typical default URI: https://YOUR_API_SECRET@YOUR_WEBSITE.azurewebsites.net/api/v1
    // typical search URI: https://YOUR_WEBSITE.azurewebsites.net/api/v1/treatments?find[created_at][$gte]=2015-10-01
    public DownloadResponse downloadRecentTreatments(int ageInMinutes) {
        List<DBTempBasalEntry> entries = new ArrayList<>();
        DownloadResponse rval = new DownloadResponse();
        if (null!=uri) {
            String searchDate = DateTime.now().minusMinutes(ageInMinutes).toString();
            String searchString = uri.getScheme() + "://" + uri.getHost() + uri.getPath() + "/treatments/.json?find[created_at][$gte]=" + searchDate.toString();
            DownloadResponse dr = doJSONDownload(searchString);
            if (null!=dr.responseObject) {
                // try to get our data from the json object
                try {
                    JSONArray jra = (JSONArray)(dr.responseObject);
                    if (jra.length() > 0) {
                        for (int i = 0; i < jra.length(); i++) {
                            JSONObject eObj = jra.getJSONObject(i);
                            if (eObj.has(DBTempBasalEntry.enteredByString)
                                    && (eObj.getString(DBTempBasalEntry.enteredByString) == DBTempBasalEntry.enteredBy)
                                    && (eObj.has(DBTempBasalEntry.eventTypeString)
                                    && (eObj.getString(DBTempBasalEntry.eventTypeString) == DBTempBasalEntry.eventType))) {
                                // This entry is one of ours.
                                double relativeInsulin = eObj.getDouble("insulin");
                                String dateString = eObj.getString("created_at");
                                // need to check Time Zone?
                                int durationMin = eObj.getInt("durationMin");
                                entries.add(new DBTempBasalEntry(new DateTime(dateString), relativeInsulin, durationMin));
                            }
                        }
                    }
                    rval.responseObject = entries;
                } catch (JSONException e) {
                    rval.responseObject = null;
                    rval.errorMessage = "Error parsing JSON: " + e.toString();
                    return rval;
                }
            } else {
                rval.responseObject = null;
                rval.errorMessage = dr.errorMessage;
            }
        } else {
            rval.responseObject = null;
            rval.errorMessage = "Invalid URI";
        }
        return rval;
    }


}
