package it.geosolutions.android.wmc;

import android.bluetooth.BluetoothDevice;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import it.geosolutions.android.wmc.bt.BluetoothUtil;
import it.geosolutions.android.wmc.model.Configuration;
import it.geosolutions.android.wmc.model.WMCReadResult;
import it.geosolutions.android.wmc.wmc.WMCFacade;
import it.geosolutions.android.wmc.wmc.WMCImpl;
import it.geosolutions.android.wmc.wmc.WMCMock;

import static org.junit.Assert.assertEquals;
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
    public void testWMCCommunication()  {


        Log.i(TAG,"wmc communication test start");

        final CountDownLatch latch = new CountDownLatch(1);

        assertNotNull(mActivityRule.getActivity());

        final ArrayList<BluetoothDevice> pairedDevices = BluetoothUtil.getPairedDevices(WMCForm.WMC_DEVICE_PREFIX);

        assertNotNull(pairedDevices);
        assertTrue(pairedDevices.size() >= 1);

        final BluetoothDevice device = pairedDevices.get(0);

        wmc = new WMCImpl(mActivityRule.getActivity().getBaseContext(), new WMCFacade.ConnectionListener() {
            @Override
            public void onDeviceConnected(String name) {

                Log.i(TAG,"wmc communication test device connected");

                assertNotNull(wmc);
                assertTrue(wmc.isConnected());
                assertEquals(name, device.getName());

                final Configuration deviceConfiguration = wmc.readConfig();

                assertNotNull(deviceConfiguration);

                sleep(100);

                //read water data
                final WMCReadResult readResult = wmc.read();

                assertNotNull(readResult);

                sleep(100);

                //change config
                final Configuration mockConfiguration = WMCMock.getMockConfiguration();

                //write config
                boolean success = wmc.writeConfig(mockConfiguration);

                assertTrue(success);

                Log.i(TAG,"wmc config test success");

                sleep(100);

                final Configuration editedConfiguration = wmc.readConfig();

                Log.i(TAG,"read edited config success");

                //read written config and confirm changes are applied
                assertEquals(mockConfiguration.ntpAddress, editedConfiguration.ntpAddress);
                assertEquals(mockConfiguration.timerSlot1Start, editedConfiguration.timerSlot1Start);
                assertEquals(mockConfiguration.recipientNum, editedConfiguration.recipientNum);
                assertEquals(mockConfiguration.siteCode, editedConfiguration.siteCode);
                assertEquals(mockConfiguration.sensorType, editedConfiguration.sensorType);

                Log.i(TAG,"compared configs success");

                sleep(200);

                //write hour
                final Calendar c = Calendar.getInstance();
                wmc.syncTime();
                //read hour and check changes are applied

                sleep(200);

                Log.i(TAG,"sync time requested");

                final WMCReadResult nextReadResult = wmc.read();

                //the dates should be nearly equal
                assertTrue(nextReadResult.date.compareTo(c.getTime()) < 100);

                Log.i(TAG,"compare date test success");

                sleep(100);

                double preset = 123456.0d;
                //write preset
                wmc.presetOverallCounter(preset);

                sleep(100);

                //read preset and compare
                final WMCReadResult thirdReadResult = wmc.read();
                assertEquals(preset, thirdReadResult.overall_total, 0.1f);

                Log.i(TAG,"preset test success");

                sleep(100);

                //clear weak index
                wmc.clear(0);

                sleep(100);

                //read and ensure week for cleared index is cleared
                final WMCReadResult anotherReadResult = wmc.read();
                assertEquals( 0f, anotherReadResult.week_Slot1[1], 0.01d);
                Log.i(TAG,"clear weak index test success");

                //Not tested as no SIM may be available
                //1. send sms test
                //2. activate GSM as it may take up to 40 seconds until result is available

                //cleanup
                sleep(100);
                //write the initial configuration
                wmc.writeConfig(deviceConfiguration);

                sleep(100);

                //finally
                wmc.disConnect();
            }

            @Override
            public void onDeviceDisconnected() {

                Log.i(TAG,"disconnected");
                latch.countDown();

            }

            @Override
            public void onDeviceConnectionFailed(String error) {

                fail("Connection failed");
            }

            private void sleep(int duration){
                try {
                    Thread.sleep(duration);
                }catch (InterruptedException e){
                    Log.e("WMCCommTest", "thread sleep interrupted");
                }
            }
        });

        wmc.connect(device);



        try {
            boolean success = latch.await(TEST_MAX_DURATION_SEC, TimeUnit.SECONDS);
            assertTrue("WMC communication test time expired", success);

            if(!success){
                if(wmc.isConnected()){
                    wmc.disConnect();
                }
            }else{

                Log.i(TAG,"WMC communication test successfully terminated");
            }

        } catch (InterruptedException e) {
            fail("timeout exceeded");
            if(wmc.isConnected()){
                wmc.disConnect();
            }
        }
    }

}
