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
import android.os.StrictMode;
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
import java.util.List;
import java.util.Map;


public class ListWLAN extends Activity
{
    WifiManager wifi;
    TextView textView;
    Map<String,Integer> bssidList;
    EditText editText;
    static Handler intervalHandler;
    int interval = 10000; //milliseconds
    static boolean autosend = false;
    CheckBox checkBoxAutosend;

    public ListWLAN() {
        bssidList = new HashMap<String,Integer>();
        intervalHandler = new Handler();
    }

    /* Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        /*
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        */

        setContentView(R.layout.activity_list_wlan);

        textView = (TextView) findViewById(R.id.textView);
        editText = (EditText) findViewById(R.id.editText);
        checkBoxAutosend = (CheckBox) findViewById(R.id.checkBoxAutosend);
        checkBoxAutosend.setChecked(autosend);
        checkBoxAutosend.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {autosend = isChecked;}});
        Button buttonClear = (Button) findViewById(R.id.buttonClear);
        buttonClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ListWLAN.this.onClear();
            }
        });
        Button buttonStart = (Button) findViewById(R.id.buttonStart);
        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ListWLAN.this.onStartInterval();
            }
        });
        Button buttonStop = (Button) findViewById(R.id.buttonStop);
        buttonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ListWLAN.this.onStopInterval();
            }
        });
        Button buttonSend = (Button) findViewById(R.id.buttonSend);
        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ListWLAN.this.send();
            }
        });

        wifi = (WifiManager)getSystemService(Context.WIFI_SERVICE);

        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                ListWLAN.this.onReceiveScanResults();
            }
        }, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        // TODO: register for network change. disable on wrong ssid or no connection
    }

    private boolean checkWifi() {
        if (wifi.isWifiEnabled() == false)
        {
            textView.append("wifi is disabled\n");
            return false;
        } else {
            textView.append("wifi is enabled\n");
            WifiInfo info = wifi.getConnectionInfo ();
            String ssid = info.getSSID();
            if (ssid == null) {
                textView.append("unable to read SSID\n");
                return false;
            } else {
                ssid = ssid.trim();
                textView.append("connected SSID is " + ssid + "\n");
                ssid = ssid.replace("\"", "");
                if (ssid.equals("FH-Visitor")) {
                    textView.append("SSID okay\n");
                    return true;
                } else {
                    textView.append("SSID wrong\n");
                    return false;
                }
            }
        }
    }

    private void onStartInterval() {
        if (ListWLAN.this.checkWifi() == true) {
            onScan();
            intervalHandler.postDelayed(new Runnable() {
                public void run() {
                    if (ListWLAN.this.checkWifi() == true) {
                        ListWLAN.this.onScan();
                        intervalHandler.postDelayed(this, interval);
                    }
                }
            }, interval);
        }
    }

    private void onStopInterval() {
        intervalHandler.removeCallbacksAndMessages(null);
    }

    private void onScan() {
        textView.append("requesting scan...\n");
        wifi.startScan();
    }

    private void send() {
        if (wifi.isWifiEnabled() == true) {
            JSONObject json = new JSONObject();
            try {
                json.put("phone", "test");
                json.put("comment", editText.getText());
                editText.setText("");
                for (Map.Entry<String, Integer> entry : bssidList.entrySet()) {
                    String bssid = entry.getKey();
                    Integer level = entry.getValue();
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

    public void onReceiveScanResults()
    {
        textView.append("retrieving...\n");
        bssidList.clear();
        List<ScanResult> results = wifi.getScanResults();
        try {
            if (results != null) {
                for (ScanResult sc : results) {
                    /*
                    // cf. https://code.google.com/p/android/issues/detail?id=61128
                    long age = SystemClock.elapsedRealtime()*1000 - sc.timestamp;
                    if (age < 10*1000*1000) {
                    */
                        //textView.append(sc.BSSID + " @" + Integer.toString(sc.level) + "dBm "+"\n");
                        bssidList.put(sc.BSSID, sc.level);
                    //}
                }
                textView.append("have " + Integer.toString(bssidList.size()) + " stations\n");
                if (checkBoxAutosend.isChecked()) {
                    send();
                }
            }
        }
        catch (Exception e)
        {
            textView.append("exception occurred\n");
        }
    }

    private void postDataAsync(JSONObject json) {
        class postDataTask extends AsyncTask<JSONObject, Void, Long> {
            @Override
            protected Long doInBackground(JSONObject... params) {
                ListWLAN.this.postData(params[0]);
                return new Long(1);
            }
            @Override
            protected void onPostExecute(Long result) {
                ListWLAN.this.textView.append("send finished\n");
            }
        }
        new postDataTask().execute(json);
    }

    private void postData(JSONObject json) {
        // from http://androidsnippets.com/executing-a-http-post-request-with-httpclient
        // and http://stackoverflow.com/questions/6218143/how-to-send-post-request-in-json-using-httpclient
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost("http://stud.fh-wedel.de/~inf100314/tracker/tracker.php");
        try {
            StringEntity se = new StringEntity(json.toString());
            httppost.setEntity(se);
            //httppost.setHeader("Accept", "application/json");
            httppost.setHeader("Content-type", "application/json");
            HttpResponse response = httpclient.execute(httppost);
        } catch (ClientProtocolException e) {
            textView.append("ClientProtocolException occurred\n");
        } catch (IOException e) {
            textView.append("IOException occurred\n");
        }
    }

}
