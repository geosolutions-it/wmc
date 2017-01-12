package it.geosolutions.android.wmc;

import android.bluetooth.BluetoothDevice;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import it.geosolutions.android.wmc.bt.BluetoothUtil;
import it.geosolutions.android.wmc.model.Configuration;
import it.geosolutions.android.wmc.model.WMCReadResult;
import it.geosolutions.android.wmc.wmc.WMCFacade;
import it.geosolutions.android.wmc.wmc.WMCImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Created by Robert Oehler on 14.11.16.
 *
 * test communication with a real WMC device
 *
 * to make use of this test, the current device must have a paired WMC device
 *
 */

@RunWith(AndroidJUnit4.class)
public class WMCCommunicationTest {

    private final static String TAG = "WMCCommTest";

    private final static int TEST_MAX_DURATION_SEC = 30;

    private WMCFacade wmc;

    @Rule
    public ActivityTestRule<WMCActivity> mActivityRule = new ActivityTestRule<>(WMCActivity.class);


    @Test
    public void testWMCCommunication(){


        Log.i(TAG,"form test start");

        final CountDownLatch latch = new CountDownLatch(1);

        assertNotNull(mActivityRule.getActivity());

        final ArrayList<BluetoothDevice> pairedDevices = BluetoothUtil.getPairedDevices(WMCForm.WMC_DEVICE_PREFIX);

        assertNotNull(pairedDevices);
        assertTrue(pairedDevices.size() > 1);

        final BluetoothDevice device = pairedDevices.get(0);

        assertTrue(device.getName().startsWith(WMCForm.WMC_DEVICE_PREFIX));

        wmc = new WMCImpl(mActivityRule.getActivity().getBaseContext(), new WMCFacade.ConnectionListener() {
            @Override
            public void onDeviceConnected(String name) {

                assertNotNull(wmc);
                assertTrue(wmc.isConnected());
                assertEquals(name, device.getName());

                final Configuration deviceConfiguration = wmc.readConfig();

                assertNotNull(deviceConfiguration);

                //TODO read water data

                final WMCReadResult readResult = wmc.read();

                assertNotNull(readResult);

                //TODO change config

                //TODO write config
                //TODO read config and confirm changes are applied

                //write hour
                //read hour and check changes are applied

                //todo write preset
                //todo read preset and compare

                //todo clear weak index
                //TODO read and ensure week for cleared index is cleared

                //Not testes
                //1. send sms test
                //2. activate GSM as it may take up to 40 seconds until result is available

                //finally
                wmc.disConnect();
            }

            @Override
            public void onDeviceDisconnected() {

                Log.i(TAG,"disconnected");

                assertFalse(wmc.isConnected());
                latch.countDown();

            }

            @Override
            public void onDeviceConnectionFailed(String error) {

                fail("Connection failed");
            }
        });

        wmc.connect(device);



        try {
            assertTrue("WMC communication test time expired", latch.await(TEST_MAX_DURATION_SEC, TimeUnit.SECONDS));

            Log.i(TAG,"WMC communication test successfully terminated");
        } catch (InterruptedException e) {
            fail("timeout exceeded");
        }

    }

}
