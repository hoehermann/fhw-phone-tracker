package de.fh_wedel.phone_tracker;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashSet;
import java.util.Set;

/**
 * User defined settings on how, where and when to scan for WiFi BSSIDs.
 */
public class ListWLANConfig implements Parcelable {
    private boolean autoSend = false;

    private int autoSendInterval = 10000;

    private String serverUrl = "http://172.26.0.114:8000/tracker.php";

    /**
     * Used to filter a subset of interesting BSSIDS
     */
    private Set<String> interestingSSIDs = new HashSet<>();

    public ListWLANConfig() {
        interestingSSIDs.add("FH-Visitor");
    }

    /**
     * It seems to be a Android best practice to implement the Parcelable.Creator interface
     * for classes that implement Parcelable. Maybe this will come in handy some time ..?
     */
    public static final Parcelable.Creator<ListWLANConfig> CREATOR
            = new Parcelable.Creator<ListWLANConfig>() {
        public ListWLANConfig createFromParcel(Parcel in) {
            return new ListWLANConfig(in);
        }

        public ListWLANConfig[] newArray(int size) {
            return new ListWLANConfig[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Reconstruct a configuration from a parcel.
     * @param parcel The parcel to read from
     */
    private ListWLANConfig(Parcel parcel) {
        this();

        autoSend = (boolean) parcel.readValue(null);
        serverUrl = parcel.readString();
        autoSendInterval = parcel.readInt();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeValue(autoSend);
        parcel.writeString(serverUrl);
        parcel.writeInt(autoSendInterval);
    }

    /**
     * @param ssid The SSID that might be interesting
     * @return True, if the SSID is interesting
     */
    public boolean isInterestingSSID(String ssid) {
        return (interestingSSIDs.contains(ssid.trim()));
    }

    /**
     * @return True, if the application should keep sending BSSID updates even when not in
     *         foreground.
     */
    public boolean getAutoSend() {
        return autoSend;
    }

    /**
     * @param autoSend True, if the application should keep sending BSSID updates even when not in
     *                 foreground.
     */
    public void setAutoSend(boolean autoSend) {
        this.autoSend = autoSend;
    }

    /**
     * @return The interval (in ms) an autosend action occurs.
     */
    public int getAutoSendInterval() {
        return autoSendInterval;
    }

    /**
     * @param autoSendInterval The interval (in ms) an autosend action occurs.
     */
    public void setAutoSendInterval(int autoSendInterval) {
        this.autoSendInterval = autoSendInterval;
    }

    /**
     * @return The URL of the phone tracker server that is used to display the current position.
     */
    public String getServerUrl() {
        return serverUrl;
    }

    /**
     * @param serverUrl  The URL of the phone tracker server that is used to display the current
     *                   position.
     */
    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }
}
