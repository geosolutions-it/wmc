package it.geosolutions.android.wmc;

import android.support.test.InstrumentationRegistry;
import android.util.Log;

import org.junit.Test;

import it.geosolutions.android.wmc.wmc.Modbus;

import static org.junit.Assert.assertEquals;

/**
 * Created by Robert Oehler on 09.11.16.
 *
 */

public class ModbusTest {


    @Test
    public void testCRCCreation(){

        final byte[] buf = new byte[] { (byte) 0x01, (byte) 0x04, (byte)0x00, (byte)0x01,(byte)0x00, (byte) 0x01, 0 , 0};

        //the CRC checksum of this byte array is supposed to be 0x60, 0x0A. --> 96, 10

        byte[] CRC = new byte[2];
        new Modbus(InstrumentationRegistry.getTargetContext()).getCRC(buf, CRC);

        assertEquals(CRC[0],0x60);
        assertEquals(CRC[1],0x0A);

    }

    @Test
    public void testBuildMessage(){

        final byte address = 1;
        final byte type = 3;
        final char start = 0;
        final char registers = 50;
        byte[] message = new byte[8];

        new Modbus(InstrumentationRegistry.getTargetContext()).buildMessage(address, type, start, registers, message);

        assertEquals(message[0], address);
        assertEquals(message[1], type);
        assertEquals(message[2], 0);
        assertEquals(message[3], 0);
        assertEquals(message[4], 0);
        assertEquals(message[5], 50);

    }


    @Test
    public void stringLengthTest(){

        String hasSix = "hassix";

        final byte[] sixBytes = hasSix.getBytes();

        Log.i("Sttt", "length (6) : "+sixBytes.length);

        String hasTwelve = "hassixhassix";

        final byte[] twBytes = hasTwelve.getBytes();

        Log.i("Sttt", "length (12) : " + twBytes.length);

        int ttt = twBytes.length;

        int doh = ttt + sixBytes.length;

        assertEquals(doh, 18);
    }
}
