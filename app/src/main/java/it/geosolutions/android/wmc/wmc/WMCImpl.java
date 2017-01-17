package it.geosolutions.android.wmc.wmc;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import java.util.Calendar;
import java.util.GregorianCalendar;

import it.geosolutions.android.wmc.BuildConfig;
import it.geosolutions.android.wmc.bt.BTConnectionProvider;
import it.geosolutions.android.wmc.bt.BTWizBluetoothConnection;
import it.geosolutions.android.wmc.model.Configuration;
import it.geosolutions.android.wmc.model.WMCReadResult;
import it.geosolutions.android.wmc.util.ShiftUtils;

/**
 * Created by Robert Oehler on 09.11.16.
 *
 * Implements the connection to a WMC device
 *
 * Makes use of a Bluetooth connection creating using the
 * BTWiz library and a Modbus instance
 *
 * the actual comm
 *
 */

public class WMCImpl implements WMCFacade  {

    private final static String TAG = "WMCImpl";

    private final boolean USE_BT_WIZ = true;

    private static final char RTC_SYNC = 7;
    private static final char PRESET_CNT_OVERALL = 10;
    private static final char STOP_TEST_RSSI = 200;
    private static final char START_TEST_RSSI = 201;
    private static final char SYS_RESET = 300;
    private static final char PRESET_CNT_TODAY = 20;
    private static final char PRESET_CNT_WEEK_SUN = 30;
    private static final char SEND_SMS_TEST = 234;
    private static final char GEN_CMD_ADDR = 100;
    private static final char POLL_BEGIN_ADDR = 141;
    private static final char POLL_END_ADDR = 206;
    private static final char GSM_SSI_ADDR = 204;

    //listener for clients which want to be informed about the state of the connection
    private ConnectionListener mConnectionListener;
    //connection provider
    private BTConnectionProvider btConnectionProvider;
    //Modbus instance
    private Modbus modbus;


    /**
     * created a WMCImpl instance
     * @param context context used for the Bluetooth connection and String resource lookup
     * @param pListener a listener for the client of this instance
     */
    public WMCImpl(final Context context, final ConnectionListener pListener){

        mConnectionListener = pListener;
        modbus = new Modbus(context);

        if(USE_BT_WIZ){
            btConnectionProvider = new BTWizBluetoothConnection(context);
        }else{
            //TODO
            btConnectionProvider = new BTWizBluetoothConnection(context);
        }
    }


    /**
     * Connects to the device @param device
     * ending up in a connection or an error
     *
     * @param device the device to connect to
     */
    @Override
    public void connect(final BluetoothDevice device) {

        btConnectionProvider.connect(device, mConnectionListener);

    }

    /**
     * closes the current connection
     */
    @Override
    public void disConnect() {

        btConnectionProvider.disconnect(mConnectionListener);
    }

    @Override
    public boolean isConnected() {
        return btConnectionProvider.isConnected();
    }

    /**
     * reads the current configuration from the device
     * @return the config or null if an error occurred
     */
    @Override
    public Configuration readConfig() {

        if(!btConnectionProvider.isConnected()){
            return null;
        }

        char[] values = new char[64];

        char pollStart = 0;
        char pollLength = 50;
        boolean success;

        success = modbus.sendFc3(btConnectionProvider, (byte) 1, pollStart, pollLength, values);

        if(success){
            Configuration config = new Configuration();

            int idx = 0;

            config.signature         = values[idx++];
            config.version           = values[idx++];
            config.timerSlot1Start   = values[idx++];
            config.timerSlot1Stop    = values[idx++];
            config.timerSlot2Start   = values[idx++];
            config.timerSlot2Stop    = values[idx++];
            config.sensorType        = values[idx++];
            config.sensorLitresRound = values[idx++];
            config.sensor_LF_Const   = values[idx++];

            //strings, 1.provider
            byte[] provider = new byte[Configuration.BYTES_MAX_PROVIDER];
            byte[] temp = new byte[2];
            for (int i = 0; i < 12; i++) {
                char ba_sh = values[idx++];
                ShiftUtils.charToTwoBytes(ba_sh, temp, false);
                provider[2 * i]     = temp[0];
                provider[2 * i + 1] = temp[1];
            }
            /**
             * the device sends a fixed number of bytes (e.g. 24 for the provider, see Configuration.BYTES_MAX constants)
             * if the actual String is shorter, the rest of the array contains 0 values
             * which result in invalid characters when transformed to String.
             * For this reason these 0 values are skipped when transforming the bytes into a String.
             */
            config.provider = cutZeroBytesAndTransformToString(provider);
            if (BuildConfig.DEBUG) {
                Log.i(TAG, String.format("read provider %s", config.provider));
            }

            //2.pinCode
            byte[] pinCode = new byte[Configuration.BYTES_MAX_PINCODE];
            for (int i = 0; i < 3; i++) {
                char ba_sh = values[idx++];
                ShiftUtils.charToTwoBytes(ba_sh, temp, false);
                pinCode[2 * i]     = temp[0];
                pinCode[2 * i + 1] = temp[1];
            }
            config.pinCode = cutZeroBytesAndTransformToString(pinCode);
            if (BuildConfig.DEBUG) {
                Log.i(TAG, String.format("read pinCode %s", config.pinCode));
            }

            //3.recipient
            byte[] recipient = new byte[Configuration.BYTES_MAX_RECIPIENT];
            for (int i = 0; i < 7; i++) {
                char ba_sh = values[idx++];
                ShiftUtils.charToTwoBytes(ba_sh, temp, false);
                recipient[2 * i]     = temp[0];
                recipient[2 * i + 1] = temp[1];
            }
            config.recipientNum = cutZeroBytesAndTransformToString(recipient);
            if (BuildConfig.DEBUG) {
                Log.i(TAG, String.format("read orig num %s", config.recipientNum));
            }

            //4.ntp address
            byte[] ntp = new byte[Configuration.BYTES_MAX_NTPADDRESS];
            for (int i = 0; i < 8; i++) {
                char ba_sh = values[idx++];
                ShiftUtils.charToTwoBytes(ba_sh, temp, false);
                ntp[2 * i]     = temp[0];
                ntp[2 * i + 1] = temp[1];
            }
            config.ntpAddress =cutZeroBytesAndTransformToString(ntp);
            if (BuildConfig.DEBUG) {
                Log.i(TAG, String.format("read ntp %s", config.ntpAddress));
            }

            //now two chars, siteCode
            config.siteCode = values[idx++];
            //and timeZone
            config.timeZone = values[idx++];

            //another String, origin num
            byte[] origin = new byte[Configuration.BYTES_MAX_ORIGINNUM];
            for (int i = 0; i < 7; i++){
                char ba_sh = values[idx++];
                ShiftUtils.charToTwoBytes(ba_sh, temp, false);
                origin[2 * i]     = temp[0];
                origin[2 * i + 1] = temp[1];
            }
            config.originNum = cutZeroBytesAndTransformToString(origin);
            if (BuildConfig.DEBUG) {
                Log.i(TAG, String.format("read orig num %s", config.originNum));
            }

            if (config.version >= 0x200) {
                config.digits = values[idx++];
            }else{
                config.digits = 6;
            }


            //done
            return config;
        }

        return null;
    }

    /**
     * transforms a byte array into a String by
     * 1. counting the entries in data array until the first 0 appears
     * 2. creating a string of the data until this index
     * @param data the data to transform
     * @return the created String or null if the data was null or empty
     */
    private String cutZeroBytesAndTransformToString(final byte[] data){

        if(data == null || data.length == 0){
            return null;
        }

        //measure the size of non-0 values
        int size = 0;
        while (size < data.length){
            if (data[size] == 0){
                break;
            }
            size++;
        }

        return new String(data, 0, size);
    }

    /**
     * writes a config @param config to the device
     * @param config the config to write
     * @return if the operation was successful
     */
    @Override
    public boolean writeConfig(Configuration config) {

        boolean success = false;

        if(!btConnectionProvider.isConnected()){
            return false;
        }

        char[] dat = new char[64];
        char offs = 0;

        dat[offs++] = (char) config.timerSlot1Start;
        dat[offs++] = (char) config.timerSlot1Stop;
        dat[offs++] = (char) config.timerSlot2Start;
        dat[offs++] = (char) config.timerSlot2Stop;
        dat[offs++] = (char) config.sensorType;
        dat[offs++] = (char) config.sensorLitresRound;
        dat[offs++] = (char) config.sensor_LF_Const;

        byte[] bytes = config.provider.getBytes();
        offs = buildCharsFromStringBytes(bytes, Configuration.BYTES_MAX_PROVIDER, dat, offs);

        bytes = config.pinCode.getBytes();
        offs = buildCharsFromStringBytes(bytes, Configuration.BYTES_MAX_PINCODE, dat, offs);

        bytes = config.recipientNum.getBytes();
        offs = buildCharsFromStringBytes(bytes, Configuration.BYTES_MAX_RECIPIENT, dat, offs);

        bytes = config.ntpAddress.getBytes();
        offs = buildCharsFromStringBytes(bytes, Configuration.BYTES_MAX_NTPADDRESS, dat, offs);

        dat[offs++] = (char) config.siteCode;
        dat[offs++] = 1;    //time zone --> always 1

        bytes = config.originNum.getBytes();
        offs = buildCharsFromStringBytes(bytes, Configuration.BYTES_MAX_ORIGINNUM, dat, offs);

        if (config.version >= 0x200) {
            dat[offs++] = (char) config.digits;    //digits
        }

        try {
            success = modbus.sendFc16(btConnectionProvider, (byte) 1, (char) 2, offs, dat);
        }catch (Exception e) {
            Log.e(TAG, "error sending writing conf to device", e);
        }

        return success;
    }

    /**
     * converts the bytes (of a string) which has the maximum length @param len
     * into chars (unsigned 16 bit integers) and inserts them into the char array @param dat
     * at the position @param offs
     * @param bytes the data to read and convert
     * @param len the maximum length of bytes, may be smaller then 0 is written
     * @param dat the data to write to
     * @param offs the offset before the operation
     * @return the current position in the char array
     */
    private char buildCharsFromStringBytes(byte[] bytes, int len, char[] dat, char offs){
        byte firstByte, secondByte;
        char tmp;
        for (int i = 0; i < len; i+=2) {
            if(bytes.length > i){
                firstByte = bytes[i];
            }else{
                firstByte = 0;
            }
            if(bytes.length > i + 1){
                secondByte =  bytes[i + 1];
            }else{
                secondByte = 0;
            }
            tmp = ShiftUtils.twoBytesToChar(firstByte, secondByte, false);
            dat[offs++] = tmp;
        }

        return offs;
    }

    /**
     * reads the water meter counter data from the device
     *
     * @return the data or null if an error occurred
     */
    @Override
    public WMCReadResult read() {

        if(!btConnectionProvider.isConnected()){
            return null;
        }

        //Create array to accept read values:
        char[] values = new char[110];
        char pollStart;
        char pollLength;
        boolean success = false;

        pollStart = POLL_BEGIN_ADDR;
        pollLength = (char) (POLL_END_ADDR - POLL_BEGIN_ADDR);

        //Read registers and display data in desired format:
        try {
            success = modbus.sendFc3(btConnectionProvider, (byte) 1, pollStart, pollLength, values);
        } catch (Exception err) {
            Log.e(TAG, "error reading water data", err);
        }

        if(success){

            int idx = 3;
            char hi;
            int rssi = 0;

            double total, tot_slot1, tot_slot2;
            /**
             * the "total" data arrives as 4 (2 byte) chars (uint16)
             * which when arranged  form an 8 byte double containing the current count
             * there exist 3 total doubles [tot, slot1, slot2]
             */
            total = ShiftUtils.charArrayToDouble(values, idx, true);
            idx += 4;
            tot_slot1 = ShiftUtils.charArrayToDouble(values, idx, true);
            idx += 4;
            tot_slot2 = ShiftUtils.charArrayToDouble(values, idx, true);
            idx += 4;

            /**
             * the "week" data consists of 2 chars (uint16) which together
             * form a 4 byte float value
             * there exist 3 floats [tot, slot1, slot2] for every day in a week,
             * starting with TODAY at [0], then SUN, MON etc
             * forming eventually 3 float[8] arrays
             */
            float[] week_tot   = new float[Configuration.WEEKDAY_ARRAY_LENGTH];
            float[] week_slot1 = new float[Configuration.WEEKDAY_ARRAY_LENGTH];
            float[] week_slot2 = new float[Configuration.WEEKDAY_ARRAY_LENGTH];

            for(int i = 0; i < Configuration.WEEKDAY_ARRAY_LENGTH; i++){
                week_tot[i]   = ShiftUtils.charArrayToFloat(values, idx, true);
                idx += 2;
                week_slot1[i] = ShiftUtils.charArrayToFloat(values, idx, true);
                idx += 2;
                week_slot2[i] = ShiftUtils.charArrayToFloat(values, idx, true);
                idx += 2;
            }

            hi = values[GSM_SSI_ADDR - POLL_BEGIN_ADDR];
            hi &= 0x00FF;
            if (hi != 0 && hi != 99){
                rssi = hi;
                if(BuildConfig.DEBUG) {
                    Log.i(TAG, "rssi received" + rssi);
                }
            }
            idx = 0;
            byte[] temp = new byte[2];
            char ch_ar = values[idx++];
            ShiftUtils.charToTwoBytes(ch_ar, temp, false);

            int sec	 = temp[0];
            int min  = temp[1];

            ch_ar = values[idx++];
            ShiftUtils.charToTwoBytes(ch_ar, temp, false);
            int hour = temp[0];
            int day	 = temp[1];

            ch_ar = values[idx++];
            ShiftUtils.charToTwoBytes(ch_ar, temp, false);

            int mon	 = temp[0]; // no need to add 1 as calendar's date is zero based
            int year = temp[1] + 1900;

//            Log.i(TAG, String.format("read total %.2f, date %d.%02d.%02d , %d:%d:%d", total, day, mon, year, hour, min, sec));

            Calendar cal = GregorianCalendar.getInstance();
            cal.set(Calendar.SECOND, sec);
            cal.set(Calendar.MINUTE, min);
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.DAY_OF_MONTH, day);
            cal.set(Calendar.MONTH, mon);
            cal.set(Calendar.YEAR, year);

            return new WMCReadResult(total,tot_slot1,tot_slot2,week_tot,week_slot1, week_slot2, cal.getTime(), rssi);
        }

        return null;
    }

    @Override
    public String getConnectionState() {
        return modbus.getModbusStatus();
    }

    @Override
    public boolean sendSysReset() {

        if(!btConnectionProvider.isConnected()){
            return false;
        }

        char[] values = new char[6];

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Log.e(TAG, "error sleeping before sending sync request", e);
        }

        values[0] = SYS_RESET;
        values[1] = (char) 0;
        values[2] = (char) 0;
        values[3] = (char) 0;
        values[4] = (char) 0;
        values[5] = (char) 0;

        try {
            for (int i = 0; i < 3; i++) {
                if (modbus.sendFc16 (btConnectionProvider, (byte) 1, GEN_CMD_ADDR, (char) 6, values)) {
                    Log.i(TAG, "disconnect (sys reset) from device successful");
                    return true;
                }
            }
        } catch (Exception err) {
            Log.e(TAG, "error sending sys reset", err);
        }
        return false;
    }

    @Override
    public boolean syncTime() {

        boolean success = false;

        if(!btConnectionProvider.isConnected()){
            return false;
        }

        char[] values = new char[4];
        try {
            Thread.sleep (100);
        } catch (InterruptedException e) {
            Log.e(TAG, "error sleeping before sending sync request", e);
        }
        final Calendar c = Calendar.getInstance();

        values[0] = RTC_SYNC;
        values[1] = ShiftUtils.twoBytesToChar( (byte) c.get(Calendar.SECOND) , (byte) c.get(Calendar.MINUTE), false);
        values[2] = ShiftUtils.twoBytesToChar( (byte) c.get(Calendar.HOUR_OF_DAY) , (byte) c.get(Calendar.DAY_OF_MONTH) , false);
        values[3] = ShiftUtils.twoBytesToChar( (byte) (c.get(Calendar.MONTH)) , (byte) (c.get(Calendar.YEAR) - 1900) , false);

        try {
            for (int i = 0; i < 3; i++) {
                if (modbus.sendFc16(btConnectionProvider, (byte) 1, GEN_CMD_ADDR, (char) 4, values)) {
                    success = true;
                    break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "error sending sync time request", e);
        }

        return success;
    }

    @Override
    public boolean activateGSM(final boolean on) {

        boolean success = false;

        if(!btConnectionProvider.isConnected()){
            return false;
        }

        char[] values = new char[6];

        try {
            Thread.sleep (100);
        } catch (InterruptedException e) {
            Log.e(TAG, "error sleeping before sending activate gsm request", e);
        }
        if(on) {
            values[0] = START_TEST_RSSI;
        }else{
            values[0] = STOP_TEST_RSSI;
        }
        values[1] = (char) 0;
        values[2] = (char) 0;
        values[3] = (char) 0;
        values[4] = (char) 0;
        values[5] = (char) 0;

        try {
            for (int i = 0; i < 3; i++) {
                if (modbus.sendFc16(btConnectionProvider, (byte) 1, GEN_CMD_ADDR, (char) 6, values)) {
                    success = true;
                    break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "error sending activate gsm request", e);
        }

        return success;
    }

    @Override
    public boolean sendTestSMS(String recipient) {

        boolean success = false;

        if(!btConnectionProvider.isConnected()){
            return false;
        }

        char[] values = new char[10];
        byte[] v = recipient.getBytes();

        try {
            Thread.sleep (100);
        } catch (InterruptedException e) {
            Log.e(TAG, "error sleeping before sending sms test request", e);
        }

        values[0] = SEND_SMS_TEST;
        //TODO test this !
        values[1] = ShiftUtils.twoBytesToChar( v[0],  v[1], true);
        values[2] = ShiftUtils.twoBytesToChar( v[2],  v[3], true);
        values[3] = ShiftUtils.twoBytesToChar( v[4],  v[5], true);
        values[4] = ShiftUtils.twoBytesToChar( v[6],  v[7], true);
        values[5] = ShiftUtils.twoBytesToChar( v[8],  v[9], true);
        values[6] = ShiftUtils.twoBytesToChar( v[10], v[11], true);
        values[7] = (char) ((v[12] & 0x00FF) << 8);

        try {
            for (int i = 0; i < 3; i++) {
                success = modbus.sendFc16(btConnectionProvider, (byte) 1, GEN_CMD_ADDR, (char) 8, values);
                if (success) {
                    break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "error sending sms test request", e);
        }

        return success;
    }

    @Override
    public boolean presetOverallCounter(double preset) {

        boolean success = false;

        if(!btConnectionProvider.isConnected()){
            return success;
        }

        char[] values = new char[20];
        try {
            Thread.sleep (100);
        } catch (InterruptedException e) {
            Log.e(TAG, "error sleeping before sending preset request", e);
        }

        values[0] = PRESET_CNT_OVERALL;

        byte[] presetAsBytes = ShiftUtils.doubleToByteArray(preset);
        ShiftUtils.shiftDoubleBytesIntoChars(1, presetAsBytes, values, true);

        byte[] zeroAsBytes = ShiftUtils.doubleToByteArray(0.0d);
        ShiftUtils.shiftDoubleBytesIntoChars(5, zeroAsBytes, values, true);
        ShiftUtils.shiftDoubleBytesIntoChars(9, zeroAsBytes, values, true);

        try {
            for (int i = 0; i < 3; i++) {
                if (modbus.sendFc16(btConnectionProvider, (byte) 1, GEN_CMD_ADDR, (char) 13, values)) {
                    success = true;
                    break;
                }
            }
        }catch (Exception e) {
            Log.e(TAG, "error sending preset request", e);
        }
        return success;
    }


    @Override
    public boolean clear(int week_day_index) {

        boolean success = false;

        if(!btConnectionProvider.isConnected()){
            return false;
        }

        char[] values = new char[6];

        try {
            Thread.sleep (100);
        } catch (InterruptedException e) {
            Log.e(TAG, "error sleeping before sending clear request", e);
        }

        if (week_day_index == 0) {
            values[0] = PRESET_CNT_TODAY;
        } else {
            values[0] = (char) (PRESET_CNT_WEEK_SUN + (week_day_index - 1));
        }

        values[1] = (char) 0;
        values[2] = (char) 0;
        values[3] = (char) 0;
        values[4] = (char) 0;
        values[5] = (char) 0;

        try {
            for (int i = 0; i < 3; i++){
                if (modbus.sendFc16(btConnectionProvider, (byte) 1, GEN_CMD_ADDR, (char) 6, values)) {
                    success = true;
                    break;
                }
            }
        }catch (Exception e){
            Log.e(TAG, "error sending clear request", e);
        }
        return success;
    }
}
