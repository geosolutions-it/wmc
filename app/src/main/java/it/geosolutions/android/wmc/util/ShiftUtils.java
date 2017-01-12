package it.geosolutions.android.wmc.util;

import java.nio.ByteBuffer;

/**
 * Created by Robert Oehler on 10.11.16.
 *
 *
 */

public class ShiftUtils {

    public static void shiftDoubleBytesIntoChars(final int idx, final byte[] doubleBytes, char[] values, boolean shiftIntoFirst){
        int count = 0;
        for(int i = idx ; i < idx + 4; i++){
            values[i] = twoBytesToChar(doubleBytes[count++], doubleBytes[count++], shiftIntoFirst);
        }
    }

    public static byte[] doubleToByteArray(double value) {
        byte[] bytes = new byte[8];
        ByteBuffer.wrap(bytes).putDouble(value);
        return bytes;
    }

    public static char twoBytesToChar(byte b1, byte b2, boolean shiftFirst){

        if(shiftFirst) {
            return (char) (((b1 & 0x00FF) << 8) + (b2 & 0x00FF));
        }else{
            return (char) (((b2 & 0x00FF) << 8) + (b1 & 0x00FF));
        }
    }

    public static void charToTwoBytes(final char _char, byte[] values, boolean shiftIntoFirst){

        if(shiftIntoFirst){
            values[0] = (byte) ((_char & 0xFF00) >> 8);
            values[1] = (byte) (_char & 0x00FF);
        }else {
            values[0] = (byte) (_char & 0x00FF);
            values[1] = (byte) ((_char & 0xFF00) >> 8);
        }
    }

    public static double charArrayToDouble(char[] values, int counter, boolean shiftFirst){

        return ByteBuffer.wrap(charArrayToByteArray(values, counter, shiftFirst)).getDouble();
    }

    public static byte[] charArrayToByteArray(char[] values, int counter, boolean shiftFirst){
        byte[] bytes = new byte[8];
        for (int i = 0; i < 4; i++) {
            char _char = values[counter++];
            if(shiftFirst) {
                bytes[2 * i] = (byte) ((_char & 0xFF00) >> 8);
                bytes[2 * i + 1] = (byte) (_char & 0x00FF);
            }else{
                bytes[2 * i] = (byte) (_char & 0x00FF);
                bytes[2 * i + 1] = (byte) ((_char & 0xFF00) >> 8);
            }
        }
        return bytes;
    }

    public static float charArrayToFloat(char[] values, int counter, boolean shiftFirst){

        byte[] bytes = new byte[4];
        for (int i = 0; i < 2; i++) {
            char _char = values[counter++];
            if(shiftFirst) {
                bytes[2 * i] = (byte) ((_char & 0xFF00) >> 8);
                bytes[2 * i + 1] = (byte) (_char & 0x00FF);
            }else{
                bytes[2 * i] = (byte) (_char & 0x00FF);
                bytes[2 * i + 1] = (byte) ((_char & 0xFF00) >> 8);
            }

        }
        return ByteBuffer.wrap(bytes).getFloat();
    }
}
