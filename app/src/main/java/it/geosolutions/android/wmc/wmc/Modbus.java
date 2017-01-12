package it.geosolutions.android.wmc.wmc;

import android.content.Context;
import android.util.Log;

import it.geosolutions.android.wmc.BuildConfig;
import it.geosolutions.android.wmc.R;
import it.geosolutions.android.wmc.bt.BTConnectionProvider;
import it.geosolutions.android.wmc.util.ShiftUtils;

/**
 * Created by Robert Oehler on 09.11.16.
 *
 * Class which implements modbus protocol functionality
 *
 * consisting mainly in
 *
 *  - reading data
 *  - writing data
 *
 *  for these functionality commands are created
 *  and the data transfer is checked with CRC checks
 */

public class Modbus {

    private final static String TAG = "Modbus";

    private String modbusStatus;
    private Context context;

    public Modbus(final Context pContext){
        this.context = pContext;
    }

    /**
     * Executes a modbus fc16 command - write data -
     * for details see http://www.simplymodbus.ca/FC16.htm
     * @param btConnection the connection to read/write from/to
     * @param address the modbus address to write to
     * @param start data Address of the first register.
     * @param registers number of registers to write
     * @param values data array containing the data to write
     */
    public boolean sendFc16(final BTConnectionProvider btConnection, byte address, char start, char registers, char[] values){

        //Ensure connection is active
        if (btConnection != null && btConnection.isConnected()) {

            //Message is 1 addr + 1 fcn + 2 start + 2 reg + 1 count + 2 * reg vals + 2 CRC
            byte[] message = new byte[9 + 2 * registers];
            //Function 16 response is fixed at 8 bytes
            byte[] response = new byte[8];

            //Add bytecount to message:
            message[6] = (byte) (registers * 2);
            //Put write values into message prior to sending:
            byte[] temp = new byte[2];
            for (int i = 0; i < registers; i++) {
                char value = values[i];
                ShiftUtils.charToTwoBytes(value, temp, true);
                message[7 + 2 * i] = temp[0];
                message[8 + 2 * i] = temp[1];
            }
            //Build outgoing message:
            buildMessage(address, (byte) 16, start, registers, message);

            //Send Modbus message
            try{
                //write message
                btConnection.write(message);
                //receive answer
                for(int i = 0; i < response.length; i++){
                    response[i] = btConnection.read();
                }

            } catch (Exception e){
                Log.e(TAG, context.getString(R.string.modbus_io_error), e);
                modbusStatus = context.getString(R.string.modbus_io_error) + e.getLocalizedMessage();
                return false;
            }

            //Evaluate message:
            if (checkResponse(response)) {
                modbusStatus = context.getString(R.string.modbus_success);
                return true;
            } else {
                Log.w(TAG, "fc16 "+context.getString(R.string.modbus_crc_error));
                modbusStatus = context.getString(R.string.modbus_crc_error);
                return false;
            }
        } else {
            modbusStatus = context.getString(R.string.modbus_no_conn);
            return false;
        }
    }

    /**
     * Executes a modbus fc3 command - read data -
     * for details see http://www.simplymodbus.ca/FC03.htm
     * @param btConnection the connection to read/write from/to
     * @param address the modbus address to write to
     * @param start address of the first register requested
     * @param registers number of registers requested
     * @param values data array to write received data into
     */
    public boolean sendFc3(final BTConnectionProvider btConnection, byte address, char start, char registers, char[] values){

        //Ensure connection is active
        if (btConnection != null && btConnection.isConnected()) {

            //Function 3 request is always 8 bytes:
            byte[] message = new byte[8];
            //Function 3 response buffer:
            byte[] response = new byte[5 + 2 * registers];
            //Build outgoing modbus message:
            buildMessage(address, (byte) 3, start, registers, message);

            //Send modbus message to Serial Port:
            try {

                btConnection.write(message, 0, message.length);

                for(int i = 0; i < response.length; i++){
                    response[i] = btConnection.read();
                }
            } catch (Exception err) {
                Log.e(TAG, context.getString(R.string.modbus_io_error));
                modbusStatus = context.getString(R.string.modbus_io_error) + err.getLocalizedMessage();
                return false;
            }

            //Evaluate message:
            if (checkResponse(response)) {
                //Return requested register values:
                for (int i = 0; i < (response.length - 5) / 2; i++)
                {
                    char value = ShiftUtils.twoBytesToChar(response[2 * i + 3], response[2 * i + 4], true);
                    values[i] = value;
                }
                modbusStatus = context.getString(R.string.modbus_success);
                return true;
            } else {
                Log.w(TAG, "fc3 "+context.getString(R.string.modbus_crc_error));
                modbusStatus = context.getString(R.string.modbus_crc_error);
                return false;
            }
        } else {
            modbusStatus = context.getString(R.string.modbus_no_conn);
            return false;
        }
    }

    /**
     * @return the current state of this modbus connection
     */
    public String getModbusStatus(){
        return modbusStatus;
    }

    /**
     * checks that the CRC created on the modbus slave being situated in
     * the last two bytes of @param message are the same
     * as when creating the CRC on this system, excluding errors during data transfer
     * @param response the data to check
     * @return if the check was successful
     */
    private boolean checkResponse(byte[] response) {
        //Perform a basic CRC check:
        byte[] CRC = new byte[2];
        getCRC(response, CRC);
        if (CRC[0] == response[response.length - 2] && CRC[1] == response[response.length - 1]) {
            return true;
        }else {
            if(BuildConfig.DEBUG) {
                Log.i(TAG, "CRC failed, expected " + response[response.length - 2] + " was " + CRC[0] + ", " + response[response.length - 1] + " was " + CRC[1]);
            }
            return false;
        }
    }

    /**
     * Java modbus CRC check implementation
     * source http://ideone.com/PrBXVh
     * @param message the message to check - last two bytes exclusive
     * @param CRC the array to write the result to
     */
    public void getCRC(byte[] message, byte[] CRC){
        //Function expects a modbus message of any length as well as a 2 byte CRC array in which to
        //return the CRC values:

        int crc = 0xFFFF;

        for (int i = 0; i < message.length - 2; i++) {
            crc ^= (int)  message[i] & 0xFF;

            for (int j = 0; j < 8; j++) {
                if ((crc & 0x0001) != 0) { // If the LSB is set
                    crc >>= 1;             // Shift right and XOR 0xA001
                    crc ^= 0xA001;
                } else {            // Else LSB is not set
                    crc >>= 1;      // Just shift right
                }
            }
        }

        byte crc0 =  (byte) ((crc & 0xFF00) >> 8);
        byte crc1 =  (byte)  (crc & 0x00FF);

        // Note, this number has low and high bytes swapped, swap
        CRC[1] = crc0;
        CRC[0] = crc1;
    }

    /**
     * Build a modbus message, editing the first six and the last two bytes of the message
     * when the bytes regarding the modbus protocol are written
     * the entire message array is used two create two crc bytes, these are written at the end
     * @param address written to the first byte
     * @param type written to the second byte
     * @param start is split into two bytes and written to the 3rd and 4th byte
     * @param registers is also split into two bytes and written to the 5th and 6th byte
     * @param message the message to edit
     */
    public void buildMessage(byte address, byte type, char start, char registers, byte[] message){
        //Array to receive CRC bytes:
        byte[] CRC = new byte[2];
        byte[] temp = new byte[2];
        message[0] = address;
        message[1] = type;
        ShiftUtils.charToTwoBytes(start, temp, true);
        message[2] = temp[0];
        message[3] = temp[1];
        ShiftUtils.charToTwoBytes(registers, temp, true);
        message[4] = temp[0];
        message[5] = temp[1];

        getCRC(message, CRC);
        message[message.length - 2] = CRC[0];
        message[message.length - 1] = CRC[1];

    }

    /**
     * can be used to log byte arrays as hex strings
     * @param bytes the data to log
     * @param title a title for the data
     */
    private void logByteArray(byte[] bytes, String title){

        final char[] hexArray = "0123456789ABCDEF".toCharArray();

        String byteString = "";
        for(int i = 0; i < bytes.length; i++){
            int v = bytes[i] & 0xFF;
            byteString += String.format("%c%c ", hexArray[v >> 4],hexArray[v & 0x0F]);
        }
        Log.d(TAG, title + " " + byteString);
    }
}
