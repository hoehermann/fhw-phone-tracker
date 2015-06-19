package de.fh_wedel.phone_tracker;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Does the actual scanning of BSSIDs in the background.
 */
public class GatherBSSID extends IntentService {

    private static String TAG = "GATHER_BSSID";

    public static String BROADCAST_ACTION = "fhw.gather.bradcast";

    public static String KEY_BROADCAST_SCANS = "scans";

    private WifiManager wifi;

    /**
     * Contains information about all currently known BSSIDs
     */
    private Map<String,ScanResult> knownBSSIDs = new HashMap<>();

    /**
     * The actual configuration of this gathering service.
     */
    private ListWLANConfig config;

    public GatherBSSID() {
        super(GatherBSSID.class.getName());
    }

    /**
     * Throws away all known scan results and uses new scan results.
     */
    public void refreshScanResults() {
        knownBSSIDs.clear();
        List<ScanResult> results = wifi.getScanResults();
        try {
            if (results != null) {
                for (ScanResult sc : results) {
                    if (config.isInterestingSSID(sc.SSID)) {
                        knownBSSIDs.put(sc.BSSID, sc);
                    }
                }

                Intent localIntent = new Intent(BROADCAST_ACTION);

                ScanResult[] scanResults = knownBSSIDs.values().toArray(new ScanResult[knownBSSIDs.size()]);
                localIntent.putExtra(KEY_BROADCAST_SCANS, scanResults);

                LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);

                Log.i(TAG, String.format("Broadcasted %d interesting APs", scanResults.length));
            }
        }
        catch (Exception e)
        {
            Log.e(TAG, "Error processing scan results", e);
        }
    }

    /**
     * Start of lifecycle
     * @param intent
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "BSSID listening service has been started");

        // Retrieve the configuration from the intent
        this.config = intent.getExtras().getParcelable(ListWLAN.STATE_KEY_CONFIG);

        wifi = (WifiManager)getSystemService(Context.WIFI_SERVICE);

        // Listen for available scan results
        BroadcastReceiver scanListener = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                refreshScanResults();
            }
        };

        registerReceiver(scanListener, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        while (true) {
            try {
                wifi.startScan();
                Log.i(TAG, "Triggered scan from service");

                Thread.currentThread().sleep(10000);
            } catch (InterruptedException e) {
                Log.e(TAG, "Could not sleep", e);
            }
        }

        //unregisterReceiver(scanListener);
    }
}
