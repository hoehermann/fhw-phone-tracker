package de.fh_wedel.phone_tracker;

import android.os.Parcel;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

/**
 * Mainly serialization and default value tests.
 */
public class ListWLANConfigTest extends AndroidTestCase {


    @SmallTest
    public void testSerialization() {
        ListWLANConfig cfgIn = new ListWLANConfig();
        cfgIn.setAutoSend(true);
        cfgIn.setAutoSendInterval(42);
        cfgIn.setServerUrl("http://localhost");

        Parcel parcel = Parcel.obtain();

        cfgIn.writeToParcel(parcel, 0);

        ListWLANConfig cfgOut = ListWLANConfig.CREATOR.createFromParcel(parcel);

        assertEquals(cfgIn.getAutoSend(), cfgOut.getAutoSend());
        assertEquals(cfgIn.getAutoSendInterval(), cfgOut.getAutoSendInterval());
        assertEquals(cfgIn.getServerUrl(), cfgOut.getServerUrl());

    }

}
