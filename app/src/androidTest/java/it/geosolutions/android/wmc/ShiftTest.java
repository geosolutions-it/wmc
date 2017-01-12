package it.geosolutions.android.wmc;

import org.junit.Test;

import it.geosolutions.android.wmc.util.ShiftUtils;

import static org.junit.Assert.assertEquals;

/**
 * Created by Robert Oehler on 10.11.16.
 *
 */

public class ShiftTest {

    private final static double INACCURACY_EPSILON = 0.001d;

    @Test
    public void charShiftTest(){

        // test all chars

        for(int i = 0; i < 0xFFFF; i++) {
            byte[] bytes = new byte[2];

            char test = (char) i;

            ShiftUtils.charToTwoBytes(test, bytes, true);

            final char reconstructed = ShiftUtils.twoBytesToChar(bytes[0], bytes[1], true);

            assertEquals(test, reconstructed);
        }

    }

    @Test
    public void doubleShiftTest(){

        //some doubles
        final double[] toTest = new double[]{123456.789d, 0.0001d, 1.25d, 532245245324d, -53122435243.4532523d };

        for(int k = 0; k < toTest.length; k++) {

            //a double
            final double test = toTest[k];

            //to bytes
            byte[] doubleBytes = ShiftUtils.doubleToByteArray(test);

            // to chars
            char[] chars = new char[4];
            for (int i = 0; i < 4; i++) {

                chars[i] = ShiftUtils.twoBytesToChar(doubleBytes[i * 2], doubleBytes[i * 2 + 1], true);
            }

            //to bytes
            final byte[] reconstructedBytes = ShiftUtils.charArrayToByteArray(chars, 0, true);

            for (int i = 0; i < 8; i++) {
                assertEquals(doubleBytes[i], reconstructedBytes[i]);
            }

            //back to double
            final double reconstructedDouble = ShiftUtils.charArrayToDouble(chars, 0, true);

            assertEquals(test, reconstructedDouble, INACCURACY_EPSILON);
        }

    }
}
