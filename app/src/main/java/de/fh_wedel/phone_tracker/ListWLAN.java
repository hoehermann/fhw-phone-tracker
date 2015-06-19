package de.fh_wedel.phone_tracker;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class ListWLAN extends Activity
{
    private static final String TAG = "ListWLANActivity";

    public static final String STATE_KEY_CONFIG = "config";

    private ListWLANConfig config;

    WifiManager wifi;
    TextView textView;
    EditText editTextComment;
    EditText editTextServerUrl;
    CheckBox checkBoxAutosend;

    ScanResult[] bufferedResults;

    /* Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Possibly read configuration from previous state
        if (savedInstanceState != null) {
            config = savedInstanceState.getParcelable(STATE_KEY_CONFIG);
        }

        // If there was no previous state: Use default configuration
        if (config == null) {
            config = new ListWLANConfig();
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                bufferedResults = (ScanResult[]) intent.getExtras().getParcelableArray(GatherBSSID.KEY_BROADCAST_SCANS);

                Log.i(TAG, String.format("UI received %d interesting APs", bufferedResults.length));

            }
        }, new IntentFilter(GatherBSSID.BROADCAST_ACTION));


        setContentView(R.layout.activity_list_wlan);

        textView = (TextView) findViewById(R.id.textViewLog);
        editTextComment = (EditText) findViewById(R.id.textComment);

        // Wire up the UI for the server URL
        editTextServerUrl = (EditText) findViewById(R.id.textServer);
        editTextServerUrl.setText(config.getServerUrl());
        editTextServerUrl.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                config.setServerUrl(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });


        checkBoxAutosend = (CheckBox) findViewById(R.id.checkBoxAutosend);
        checkBoxAutosend.setChecked(config.getAutoSend());
        checkBoxAutosend.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                config.setAutoSend(isChecked);
            }
        });
        final Button buttonClear = (Button) findViewById(R.id.buttonClear);
        buttonClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ListWLAN.this.onClear();
            }
        });

        // Wire up the start scanning button to
        Button buttonStart = (Button) findViewById(R.id.buttonStart);
        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkWifi()) {
                    Intent intent = new Intent(ListWLAN.this, GatherBSSID.class);
                    intent.putExtra(STATE_KEY_CONFIG, config);

                    startService(intent);
                }
            }
        });
        Button buttonStop = (Button) findViewById(R.id.buttonStop);
        buttonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        Button buttonSend = (Button) findViewById(R.id.buttonSend);
        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bufferedResults != null) {
                    ListWLAN.this.send(bufferedResults);
                }
            }
        });

        wifi = (WifiManager)getSystemService(Context.WIFI_SERVICE);

        // TODO: register for network change. disable on wrong ssid or no connection
    }

    /**
     * Called to retrieve per-instance state from an activity before being killed so that the state
     * can be restored in onCreate(Bundle) or onRestoreInstanceState(Bundle) (the Bundle populated
     * by this method will be passed to both).
     * @param outState The bundle to persist to
     */
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(STATE_KEY_CONFIG, config);
    }

    private boolean checkWifi() {
        if (wifi.isWifiEnabled() == false)
        {
            textView.append("wifi is disabled\n");
            return false;
        } else {
            //textView.append("wifi is enabled\n");
            WifiInfo info = wifi.getConnectionInfo ();
            String ssid = info.getSSID();
            if (ssid == null) {
                textView.append("unable to read SSID\n");
                return false;
            } else {
                ssid = ssid.trim();
                textView.append("Connected SSID is " + ssid + "\n");
                ssid = ssid.replace("\"", "");
                if (config.isInterestingSSID(ssid)) {
                    //textView.append("SSID okay\n");
                    return true;
                } else {
                    textView.append("SSID is not interesting\n");
                    return false;
                }
            }
        }
    }

    /**
     * Sends known access points to the phone tracker server.
     */
    private void send(ScanResult[] scanResults) {
        if (wifi.isWifiEnabled() == true) {
            JSONObject json = new JSONObject();
            try {
                json.put("phone", "test");
                json.put("comment", editTextComment.getText());
                editTextComment.setText("");
                for (ScanResult entry : scanResults) {
                    String bssid = entry.BSSID;
                    Integer level = entry.level;
                    JSONObject ap = new JSONObject();
                    ap.put("bssid", bssid);
                    ap.put("level", level);
                    json.accumulate("stations", ap);
                }
                //textView.append(json.toString());
            } catch (JSONException e) {
                //e.printStackTrace();
                textView.append("json exception");
            }
            //postData(json);
            postDataAsync(json);
        }
    }

    private void onClear() {
        textView.setText("");
    }

    /**
     * Send JSON data to the server in a background task
     * @param json Arbitrary JSON data
     */
    private void postDataAsync(JSONObject json) {
        final ListWLAN self = this;

        class PostDataTask extends AsyncTask<JSONObject, Void, Exception> {
            @Override
            protected Exception doInBackground(JSONObject... params) {
                return self.postData(params[0]);
            }
            @Override
            protected void onPostExecute(Exception result) {
                if (result == null) {
                    self.textView.append("Send finished\n");
                } else {
                    self.textView.append(String.format("Send failed: %s\n", result.getMessage()));
                }
            }
        }
        ListWLAN.this.textView.append(String.format("Sending found BSSIDs to \"%s\"...\n", config.getServerUrl()));
        new PostDataTask().execute(json);
    }

    /**
     * Send JSON data to the server on the foreground thread
     * @param json Arbitrary JSON data
     */
    private Exception postData(JSONObject json) {
        // from http://androidsnippets.com/executing-a-http-post-request-with-httpclient
        // and http://stackoverflow.com/questions/6218143/how-to-send-post-request-in-json-using-httpclient
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(config.getServerUrl());
        try {
            StringEntity se = new StringEntity(json.toString());
            httppost.setEntity(se);
            //httppost.setHeader("Accept", "application/json");
            httppost.setHeader("Content-type", "application/json");
            HttpResponse response = httpclient.execute(httppost);

            return null;
        } catch (ClientProtocolException e) {
            String debugOutput = String.format("ClientProtocolException occurred: %s\n", e);
            Log.e(TAG, debugOutput, e);

            return e;
        } catch (IOException e) {
            String debugOutput = String.format("IOException occurred: %s\n", e);
            Log.e(TAG, debugOutput, e);

            return e;
        }
    }

}
