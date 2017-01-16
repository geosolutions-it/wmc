package it.geosolutions.android.wmc;

import android.support.test.rule.ActivityTestRule;
import android.support.test.rule.UiThreadTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import it.geosolutions.android.wmc.model.Configuration;
import it.geosolutions.android.wmc.service.events.WMCConnectionStateChangedEvent;
import it.geosolutions.android.wmc.wmc.WMCMock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Created by Robert Oehler on 08.11.16.
 *
 * reference : https://developer.android.com/training/testing/ui-testing/espresso-testing.html
 *
 * Espresso UI tests work under the following constraint:
 *
 * "... at least 90 percent of the view's area is displayed to the user.
 *
 * as Espresso by default is looking for elements which are completely visible on device screen.
 *
 * So please execute this on a (possible large) tablet, otherwise Espresso may fail or freeze
 */

@RunWith(AndroidJUnit4.class)
public class FormTest {

    private final static String TAG = "FormTest";

    private final static int TEST_MAX_DURATION_SEC = 10;

    private final static int TEST_TS_1_START = 1;
    private final static int TEST_TS_1_STOP  = 2;
    private final static int TEST_TS_2_START = 2;
    private final static int TEST_TS_2_STOP  = 3;

    private final static int TEST_LF_CONST  = 42;
    private final static int TEST_LITRES    = 66;

    private final static int TEST_SENSOR_TYPE    = 0;

    private CountDownLatch latch;

    @Rule
    public UiThreadTestRule uiThreadTestRule = new UiThreadTestRule();

    @Rule
    public ActivityTestRule<WMCActivity> mActivityRule = new ActivityTestRule<>(WMCActivity.class);

    @Test
    public void testForm() throws InterruptedException {

        assertNotNull(mActivityRule.getActivity());

        Log.i(TAG,"form test start");

        latch = new CountDownLatch(1);

        //inflate form and test

        final WMCForm form = mActivityRule.getActivity().getFormFragment();

        assertNotNull(form);

        EventBus.getDefault().register(this);

        //make sure the form uses the mock WMC, not a real device
        form.setDebug(true);
        //and it does not listen itself for WMC service events
        form.setTest();

        //"connect" - will only work if at least one paired device is available on the device
        mActivityRule.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                Button connectButton = (Button) mActivityRule.getActivity().findViewById(R.id.connect_device_button);

                assertNotNull(connectButton);

                connectButton.performClick();

            }
        });

        try {
            assertTrue("FormTest time expired", latch.await(TEST_MAX_DURATION_SEC, TimeUnit.SECONDS));

            EventBus.getDefault().unregister(this);

            Log.i(TAG,"FormTest successfully terminated");
        } catch (InterruptedException e) {
            fail("timeout exceeded");
        }
    }

    @Subscribe
    public void onEvent(WMCConnectionStateChangedEvent event){

        switch (event.getState()){

            case CONNECTED:

                Log.i(TAG,"device connected");

                final WMCForm form = mActivityRule.getActivity().getFormFragment();

                assertNotNull(form);
                
                final Configuration mockConf = WMCMock.getMockConfiguration();

                //Testing the UI in five steps
                //1. get references

                EditText ts1 = ((EditText) mActivityRule.getActivity().findViewById(R.id.tslots_t1_b_et));
                EditText ts2 = ((EditText) mActivityRule.getActivity().findViewById(R.id.tslots_t1_e_et));
                EditText ts3 = ((EditText) mActivityRule.getActivity().findViewById(R.id.tslots_t2_b_et));
                EditText ts4 = ((EditText) mActivityRule.getActivity().findViewById(R.id.tslots_t2_e_et));

                EditText litres_et = ((EditText) mActivityRule.getActivity().findViewById(R.id.hf_et));
                EditText lf_et = ((EditText) mActivityRule.getActivity().findViewById(R.id.k_const_et));

                EditText provider_et = ((EditText) mActivityRule.getActivity().findViewById(R.id.provider_et));
                EditText pincode_et  = ((EditText) mActivityRule.getActivity().findViewById(R.id.pin_code_et));
                EditText recipient_et= ((EditText) mActivityRule.getActivity().findViewById(R.id.sms_rec_et));
                EditText ntp_et      = ((EditText) mActivityRule.getActivity().findViewById(R.id.time_server_et));
                EditText origin_et   = ((EditText) mActivityRule.getActivity().findViewById(R.id.sms_origin_et));

                EditText site_code_et   = ((EditText) mActivityRule.getActivity().findViewById(R.id.code_et));

                Spinner sensorTypeSpinner = ((Spinner) mActivityRule.getActivity().findViewById(R.id.type_spinner));
                Spinner digitsSpinner     = ((Spinner) mActivityRule.getActivity().findViewById(R.id.digits_spinner));
                Spinner deviceSpinner  = (Spinner) mActivityRule.getActivity().findViewById(R.id.device_spinner);
                Spinner weekdaySpinner = (Spinner) mActivityRule.getActivity().findViewById(R.id.weekday_spinner);

                Button presetButton  = (Button) mActivityRule.getActivity().findViewById(R.id.button_preset);
                Button clearButton   = (Button) mActivityRule.getActivity().findViewById(R.id.button_clear);
                Button graphButton   = (Button) mActivityRule.getActivity().findViewById(R.id.button_graph);
                Button syncButton    = (Button) mActivityRule.getActivity().findViewById(R.id.sync_button);
                Button smsTestButton = (Button) mActivityRule.getActivity().findViewById(R.id.sms_test_button);
                Button gsmOnButton   = (Button) mActivityRule.getActivity().findViewById(R.id.gsm_on_button);
                Button connectButton = (Button) mActivityRule.getActivity().findViewById(R.id.connect_device_button);
                ImageButton refreshButton = (ImageButton) mActivityRule.getActivity().findViewById(R.id.refresh_devices_button);

                //2. Not connected yet, assert disabled mode
                boolean enabled = false;

                assertEquals(presetButton.isEnabled(), enabled);
                assertEquals(clearButton.isEnabled(), enabled);
                assertEquals(graphButton.isEnabled(), enabled);
                assertEquals(syncButton.isEnabled(), enabled);
                assertEquals(smsTestButton.isEnabled(), enabled);
                assertEquals(gsmOnButton.isEnabled(), enabled);
                assertEquals(sensorTypeSpinner.isEnabled(), enabled);
                assertEquals(digitsSpinner.isEnabled(), enabled);
                assertEquals(weekdaySpinner.isEnabled(), enabled);

                assertEquals(site_code_et.isFocusable(), enabled);
                assertEquals(litres_et.isFocusable(), enabled);
                assertEquals(lf_et.isFocusable(), enabled);
                assertEquals(ts1.isFocusable(), enabled);
                assertEquals(ts2.isFocusable(), enabled);
                assertEquals(ts3.isFocusable(), enabled);
                assertEquals(ts4.isFocusable(), enabled);
                assertEquals(provider_et.isFocusable(), enabled);
                assertEquals(pincode_et.isFocusable(), enabled);
                assertEquals(recipient_et.isFocusable(), enabled);
                assertEquals(ntp_et.isFocusable(), enabled);
                assertEquals(origin_et.isFocusable(), enabled);

                //these are enabled/"connect"/visible when not connected
                assertEquals(deviceSpinner.isEnabled(), !enabled);
                assertEquals(connectButton.getText(), mActivityRule.getActivity().getString(R.string.button_connect));
                assertEquals(refreshButton.getVisibility(), View.VISIBLE);

                //now "connect"
                form.hideProgress();
                form.changeState(true, true);
                form.onConfigurationRead(mockConf);
                enabled = true;

                //3.check form enabling
                //these become editable
                assertEquals(presetButton.isEnabled(), enabled);
                assertEquals(clearButton.isEnabled(), enabled);
                assertEquals(graphButton.isEnabled(), enabled);
                assertEquals(syncButton.isEnabled(), enabled);
                assertEquals(gsmOnButton.isEnabled(), enabled);
                assertEquals(site_code_et.isFocusable(), enabled);
                assertEquals(litres_et.isFocusable(), enabled);
                assertEquals(lf_et.isFocusable(), enabled);
                assertEquals(ts1.isFocusable(), enabled);
                assertEquals(ts2.isFocusable(), enabled);
                assertEquals(ts3.isFocusable(), enabled);
                assertEquals(ts4.isFocusable(), enabled);
                assertEquals(provider_et.isFocusable(), enabled);
                assertEquals(pincode_et.isFocusable(), enabled);
                assertEquals(recipient_et.isFocusable(), enabled);
                assertEquals(ntp_et.isFocusable(), enabled);
                assertEquals(origin_et.isFocusable(), enabled);

                //sms test button is only enabled when rssi was received
                assertEquals(smsTestButton.isEnabled(), false);

                //these are disabled/"Disconnect"/invisible when connected
                assertEquals(deviceSpinner.isEnabled(), !enabled);
                assertEquals(connectButton.getText(), form.getString(R.string.button_disconnect));
                assertEquals(refreshButton.getVisibility(), View.GONE);

                //4. check form is populated

                assertEquals(ts1.getText().toString(), Integer.toString(mockConf.timerSlot1Start));
                assertEquals(ts2.getText().toString(), Integer.toString(mockConf.timerSlot1Stop));
                assertEquals(ts3.getText().toString(), Integer.toString(mockConf.timerSlot2Start));
                assertEquals(ts4.getText().toString(), Integer.toString(mockConf.timerSlot2Stop));

                assertEquals(litres_et.getText().toString(), Integer.toString(mockConf.sensorLitresRound));
                assertEquals(lf_et.getText().toString(), Integer.toString(mockConf.sensor_LF_Const));

                assertEquals(sensorTypeSpinner.getSelectedItemPosition(), mockConf.sensorType);

                assertEquals(provider_et.getText().toString(), new String(mockConf.provider));
                assertEquals(pincode_et.getText().toString(), new String(mockConf.pinCode));
                assertEquals(recipient_et.getText().toString(), new String(mockConf.recipientNum));
                assertEquals(ntp_et.getText().toString(), new String(mockConf.ntpAddress));
                assertEquals(origin_et.getText().toString(), new String(mockConf.originNum));

                assertEquals(site_code_et.getText().toString(), String.format(Locale.getDefault(),"%04d",mockConf.siteCode));

                if (mockConf.sensorType == 1) { //sensorType "HF & Dir" makes lf const non-editable
                    assertFalse(lf_et.isEnabled());
                } else { // with sensorType "LF" this const is editable
                    assertTrue(lf_et.isEnabled());
                }

                if(mockConf.version >= 0x200) {
                    //enable and set according to config
                    assertTrue(digitsSpinner.isEnabled());

                    if (mockConf.digits == 6) {
                        assertEquals(digitsSpinner.getSelectedItemPosition(), 0);
                    } else if (mockConf.digits == 7){
                        assertEquals(digitsSpinner.getSelectedItemPosition(), 1);
                    } else {
                        assertEquals(digitsSpinner.getSelectedItemPosition(), 2);
                    }
                }else{
                    assertFalse(digitsSpinner.isEnabled());
                    assertEquals(digitsSpinner.getSelectedItemPosition(), 0);
                }

                //5. now test editing, type some time slot values
                ts1.setText(Integer.toString(TEST_TS_1_START));
                ts2.setText(Integer.toString(TEST_TS_1_STOP));
                ts3.setText(Integer.toString(TEST_TS_2_START));
                ts4.setText(Integer.toString(TEST_TS_2_STOP));

                lf_et.setText(Integer.toString(TEST_LF_CONST));
                litres_et.setText(Integer.toString(TEST_LITRES));

                //sensor type spinner
                assertNotSame(sensorTypeSpinner.getSelectedItemPosition(), TEST_SENSOR_TYPE);
                assertNotNull(sensorTypeSpinner.getAdapter());
                assertEquals(sensorTypeSpinner.getAdapter().getCount(), 2);

                sensorTypeSpinner.performClick();
                sensorTypeSpinner.setSelection(TEST_SENSOR_TYPE);

                assertTrue(sensorTypeSpinner.isEnabled());
                assertEquals(sensorTypeSpinner.getSelectedItemPosition(), TEST_SENSOR_TYPE);

                final Configuration currentConfig = form.getCurrentConfiguration();

                assertEquals(currentConfig.timerSlot1Start, TEST_TS_1_START);
                assertEquals(currentConfig.timerSlot1Stop,  TEST_TS_1_STOP);
                assertEquals(currentConfig.timerSlot2Start, TEST_TS_2_START);
                assertEquals(currentConfig.timerSlot2Stop,  TEST_TS_2_STOP);

                assertEquals(currentConfig.sensor_LF_Const,  TEST_LF_CONST);
                assertEquals(currentConfig.sensorLitresRound,  TEST_LITRES);

                Log.i(TAG,"asserts successful");

                //6. "disconnect"
                form.disconnect(true);


                break;
            case DISCONNECTED:


                Log.i(TAG,"disconnected");

                latch.countDown();

                break;
            case CONNECTION_ERROR:

                fail("Connection failed");
                break;
        }
    }


    //@Test
    public void oldtestForm() throws InterruptedException {

        Log.i(TAG,"form test start");

        final CountDownLatch latch = new CountDownLatch(1);

        assertNotNull(mActivityRule.getActivity());

        final WMCForm form = mActivityRule.getActivity().getFormFragment();

        assertNotNull(form);

        final Configuration mockConf = WMCMock.getMockConfiguration();

        //make sure the form uses the mock WMC, not a real device
//        form.setWMC(new WMCMock(new WMCFacade.ConnectionListener() {
//            @Override
//            public void onDeviceConnected(String name) {
//
//                mActivityRule.getActivity().runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//
//                        Log.i(TAG,"device connected");
//
//                        /**
//                         * Testing the UI in five steps
//                         */
//                        //1. get references
//
//                        EditText ts1 = ((EditText) mActivityRule.getActivity().findViewById(R.id.tslots_t1_b_et));
//                        EditText ts2 = ((EditText) mActivityRule.getActivity().findViewById(R.id.tslots_t1_e_et));
//                        EditText ts3 = ((EditText) mActivityRule.getActivity().findViewById(R.id.tslots_t2_b_et));
//                        EditText ts4 = ((EditText) mActivityRule.getActivity().findViewById(R.id.tslots_t2_e_et));
//
//                        EditText litres_et = ((EditText) mActivityRule.getActivity().findViewById(R.id.hf_et));
//                        EditText lf_et = ((EditText) mActivityRule.getActivity().findViewById(R.id.k_const_et));
//
//                        EditText provider_et = ((EditText) mActivityRule.getActivity().findViewById(R.id.provider_et));
//                        EditText pincode_et  = ((EditText) mActivityRule.getActivity().findViewById(R.id.pin_code_et));
//                        EditText recipient_et= ((EditText) mActivityRule.getActivity().findViewById(R.id.sms_rec_et));
//                        EditText ntp_et      = ((EditText) mActivityRule.getActivity().findViewById(R.id.time_server_et));
//                        EditText origin_et   = ((EditText) mActivityRule.getActivity().findViewById(R.id.sms_origin_et));
//
//                        EditText site_code_et   = ((EditText) mActivityRule.getActivity().findViewById(R.id.code_et));
//
//                        Spinner sensorTypeSpinner = ((Spinner) mActivityRule.getActivity().findViewById(R.id.type_spinner));
//                        Spinner digitsSpinner     = ((Spinner) mActivityRule.getActivity().findViewById(R.id.digits_spinner));
//                        Spinner deviceSpinner  = (Spinner) mActivityRule.getActivity().findViewById(R.id.device_spinner);
//                        Spinner weekdaySpinner = (Spinner) mActivityRule.getActivity().findViewById(R.id.weekday_spinner);
//
//                        Button presetButton  = (Button) mActivityRule.getActivity().findViewById(R.id.button_preset);
//                        Button clearButton   = (Button) mActivityRule.getActivity().findViewById(R.id.button_clear);
//                        Button graphButton   = (Button) mActivityRule.getActivity().findViewById(R.id.button_graph);
//                        Button syncButton    = (Button) mActivityRule.getActivity().findViewById(R.id.sync_button);
//                        Button smsTestButton = (Button) mActivityRule.getActivity().findViewById(R.id.sms_test_button);
//                        Button gsmOnButton   = (Button) mActivityRule.getActivity().findViewById(R.id.gsm_on_button);
//                        Button connectButton = (Button) mActivityRule.getActivity().findViewById(R.id.connect_device_button);
//                        ImageButton refreshButton = (ImageButton) mActivityRule.getActivity().findViewById(R.id.refresh_devices_button);
//
//                        //2. Not connected yet, assert disabled mode
//                        boolean enabled = false;
//
//                        assertEquals(presetButton.isEnabled(), enabled);
//                        assertEquals(clearButton.isEnabled(), enabled);
//                        assertEquals(graphButton.isEnabled(), enabled);
//                        assertEquals(syncButton.isEnabled(), enabled);
//                        assertEquals(smsTestButton.isEnabled(), enabled);
//                        assertEquals(gsmOnButton.isEnabled(), enabled);
//                        assertEquals(sensorTypeSpinner.isEnabled(), enabled);
//                        assertEquals(digitsSpinner.isEnabled(), enabled);
//                        assertEquals(weekdaySpinner.isEnabled(), enabled);
//
//                        assertEquals(site_code_et.isFocusable(), enabled);
//                        assertEquals(litres_et.isFocusable(), enabled);
//                        assertEquals(lf_et.isFocusable(), enabled);
//                        assertEquals(ts1.isFocusable(), enabled);
//                        assertEquals(ts2.isFocusable(), enabled);
//                        assertEquals(ts3.isFocusable(), enabled);
//                        assertEquals(ts4.isFocusable(), enabled);
//                        assertEquals(provider_et.isFocusable(), enabled);
//                        assertEquals(pincode_et.isFocusable(), enabled);
//                        assertEquals(recipient_et.isFocusable(), enabled);
//                        assertEquals(ntp_et.isFocusable(), enabled);
//                        assertEquals(origin_et.isFocusable(), enabled);
//
//                        //these are enabled/"connect"/visible when not connected
//                        assertEquals(deviceSpinner.isEnabled(), !enabled);
//                        assertEquals(connectButton.getText(), mActivityRule.getActivity().getString(R.string.button_connect));
//                        assertEquals(refreshButton.getVisibility(), View.VISIBLE);
//
//                        form.onConnected(mockConf, "formTest");
//
//                        //3.check form enabling
//                        enabled = true;
//
//                        //these become editable
//                        assertEquals(presetButton.isEnabled(), enabled);
//                        assertEquals(clearButton.isEnabled(), enabled);
//                        assertEquals(graphButton.isEnabled(), enabled);
//                        assertEquals(syncButton.isEnabled(), enabled);
//                        assertEquals(smsTestButton.isEnabled(), enabled);
//                        assertEquals(gsmOnButton.isEnabled(), enabled);
//                        assertEquals(site_code_et.isFocusable(), enabled);
//                        assertEquals(litres_et.isFocusable(), enabled);
//                        assertEquals(lf_et.isFocusable(), enabled);
//                        assertEquals(ts1.isFocusable(), enabled);
//                        assertEquals(ts2.isFocusable(), enabled);
//                        assertEquals(ts3.isFocusable(), enabled);
//                        assertEquals(ts4.isFocusable(), enabled);
//
//                        //these dont change, remain ineditable
//                        assertEquals(provider_et.isFocusable(), !enabled);
//                        assertEquals(pincode_et.isFocusable(), !enabled);
//                        assertEquals(recipient_et.isFocusable(), !enabled);
//                        assertEquals(ntp_et.isFocusable(), !enabled);
//                        assertEquals(origin_et.isFocusable(), !enabled);
//
//                        //these are disabled/"Disconnect"/invisible when connected
//                        assertEquals(deviceSpinner.isEnabled(), !enabled);
//                        assertEquals(connectButton.getText(), mActivityRule.getActivity().getString(R.string.button_disconnect));
//                        assertEquals(refreshButton.getVisibility(), View.GONE);
//
//                        //4. check form is populated
//
//                        assertEquals(ts1.getText().toString(), Integer.toString(mockConf.timerSlot1Start));
//                        assertEquals(ts2.getText().toString(), Integer.toString(mockConf.timerSlot1Stop));
//                        assertEquals(ts3.getText().toString(), Integer.toString(mockConf.timerSlot2Start));
//                        assertEquals(ts4.getText().toString(), Integer.toString(mockConf.timerSlot2Stop));
//
//                        assertEquals(litres_et.getText().toString(), Integer.toString(mockConf.sensorLitresRound));
//                        assertEquals(lf_et.getText().toString(), Integer.toString(mockConf.sensor_LF_Const));
//
//                        assertEquals(sensorTypeSpinner.getSelectedItemPosition(), mockConf.sensorType);
//
//                        assertEquals(provider_et.getText().toString(), new String(mockConf.provider));
//                        assertEquals(pincode_et.getText().toString(), new String(mockConf.pinCode));
//                        assertEquals(recipient_et.getText().toString(), new String(mockConf.recipientNum));
//                        assertEquals(ntp_et.getText().toString(), new String(mockConf.ntpAddress));
//                        assertEquals(origin_et.getText().toString(), new String(mockConf.originNum));
//
//                        assertEquals(site_code_et.getText().toString(), String.format(Locale.getDefault(),"%04d",mockConf.siteCode));
//
//                        if (mockConf.sensorType == 1) { //sensorType "HF & Dir" makes lf const non-editable
//                            assertFalse(lf_et.isEnabled());
//                        } else { // with sensorType "LF" this const is editable
//                            assertTrue(lf_et.isEnabled());
//                        }
//
//                        if(mockConf.version >= 0x200) {
//                            //enable and set according to config
//                            assertTrue(digitsSpinner.isEnabled());
//
//                            if (mockConf.digits == 6) {
//                                assertEquals(digitsSpinner.getSelectedItemPosition(), 0);
//                            } else if (mockConf.digits == 7){
//                                assertEquals(digitsSpinner.getSelectedItemPosition(), 1);
//                            } else {
//                                assertEquals(digitsSpinner.getSelectedItemPosition(), 2);
//                            }
//                        }else{
//                            assertFalse(digitsSpinner.isEnabled());
//                            assertEquals(digitsSpinner.getSelectedItemPosition(), 0);
//                        }
//
//                        //5. now test editing, type some time slot values
//                        ts1.setText(Integer.toString(TEST_TS_1_START));
//                        ts2.setText(Integer.toString(TEST_TS_1_STOP));
//                        ts3.setText(Integer.toString(TEST_TS_2_START));
//                        ts4.setText(Integer.toString(TEST_TS_2_STOP));
//
//                        lf_et.setText(Integer.toString(TEST_LF_CONST));
//                        litres_et.setText(Integer.toString(TEST_LITRES));
//
//                        //TODO spinner ???
//                        sensorTypeSpinner.setOnItemSelectedListener(form);
//                        assertTrue(sensorTypeSpinner.getOnItemSelectedListener() != null);
//                        assertNotNull(sensorTypeSpinner.getAdapter());
//                        assertEquals(sensorTypeSpinner.getAdapter().getCount(), 2);
//
//                        sensorTypeSpinner.performClick();
//
//                        assertTrue(sensorTypeSpinner.isEnabled());
//
//
//                        View ddv = sensorTypeSpinner.getAdapter().getDropDownView(TEST_SENSOR_TYPE, null, null);
//                        View view = sensorTypeSpinner.getAdapter().getView(TEST_SENSOR_TYPE,null, null);
//
//                        //normal way, not working
////                        sensorTypeSpinner.requestFocus();
////                        sensorTypeSpinner.setSelection(TEST_SENSOR_TYPE);
////
////                        ddv.performClick();
////                        view.performClick();
//
//                        final Configuration currentConfig = form.getCurrentConfiguration();
//
//                        assertEquals(currentConfig.timerSlot1Start, TEST_TS_1_START);
//                        assertEquals(currentConfig.timerSlot1Stop,  TEST_TS_1_STOP);
//                        assertEquals(currentConfig.timerSlot2Start, TEST_TS_2_START);
//                        assertEquals(currentConfig.timerSlot2Stop,  TEST_TS_2_STOP);
//
//                        assertEquals(currentConfig.sensor_LF_Const,  TEST_LF_CONST);
//                        assertEquals(currentConfig.sensorLitresRound,  TEST_LITRES);
//
//                        //TODO spinner selection is not fired
////                        assertEquals(currentConfig.sensorType, TEST_SENSOR_TYPE);
//
//                        Log.i(TAG,"asserts successful");
//
//                        //6. "disconnect" using the disconnect button
//                        mActivityRule.getActivity().findViewById(R.id.connect_device_button).performClick();
//
//                    }
//                });
//            }
//
//            @Override
//            public void onDeviceDisconnected() {
//
//                Log.i(TAG,"disconnected");
//
//                latch.countDown();
//
//            }
//
//            @Override
//            public void onDeviceConnectionFailed(String message) {
//
//                fail("Connection failed");
//            }
//        }));

        //"connect" - will only work if at least one paired device is available on the device
        mActivityRule.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivityRule.getActivity().findViewById(R.id.connect_device_button).performClick();
            }
        });

        try {
            assertTrue("FormTest time expired", latch.await(TEST_MAX_DURATION_SEC, TimeUnit.SECONDS));

            Log.i(TAG,"FormTest successfully terminated");
        } catch (InterruptedException e) {
            fail("timeout exceeded");
        }

    }
}
