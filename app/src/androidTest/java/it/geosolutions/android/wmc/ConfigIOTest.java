package it.geosolutions.android.wmc;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.util.Pair;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import it.geosolutions.android.wmc.model.Configuration;
import it.geosolutions.android.wmc.util.ConfigIO;
import it.geosolutions.android.wmc.wmc.WMCMock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by Robert Oehler on 07.11.16.
 *
 */

@RunWith(AndroidJUnit4.class)
public class ConfigIOTest {


    /**
     * test writing a configuration to disk
     * parsing and  validating it comparing
     * the result with the original mock config
     * @throws Exception
     */
    @Test
    public void testFileParsing() throws Exception {

        final String testFileName = "test.xml";

        final String testTargetPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + testFileName;

        assertTrue(Environment.getExternalStorageDirectory().canWrite());

        final File testFile = new File(testTargetPath);
        try {

            final Context context = InstrumentationRegistry.getTargetContext();

            assertNotNull(context);

            final ContentResolver contentResolver = context.getContentResolver();

            assertNotNull(contentResolver);

            final Configuration mockConfig = WMCMock.getMockConfiguration();

            Uri uri = Uri.fromFile(testFile);

            boolean writeSuccess = ConfigIO.writeConfigToDisk(uri, contentResolver, mockConfig);

            assertTrue(writeSuccess);

            final Pair<Configuration, String> readResult = ConfigIO.readAndValidateConfig(context, testFile.getAbsolutePath());

            final Configuration readConf = readResult.first;

            assertNotNull(readResult.second, readConf);
            assertNotNull(readResult.second);
            assertEquals(readResult.second, "Configuration read with success");

            //now compare

            assertEquals(mockConfig.siteCode, readConf.siteCode);
            assertEquals(mockConfig.timeZone, readConf.timeZone);
            assertEquals(mockConfig.sensorType, readConf.sensorType);
            assertEquals(mockConfig.sensorLitresRound, readConf.sensorLitresRound);
            assertEquals(mockConfig.sensor_LF_Const, readConf.sensor_LF_Const);
            assertEquals(mockConfig.timerSlot1Start, readConf.timerSlot1Start);
            assertEquals(mockConfig.timerSlot1Stop, readConf.timerSlot1Stop);
            assertEquals(mockConfig.timerSlot2Start, readConf.timerSlot2Start);
            assertEquals(mockConfig.timerSlot2Stop, readConf.timerSlot2Stop);
            assertEquals(mockConfig.pinCode, readConf.pinCode);
            assertEquals(mockConfig.recipientNum, readConf.recipientNum);
            assertEquals(mockConfig.originNum, readConf.originNum);
            assertEquals(mockConfig.ntpAddress, readConf.ntpAddress);
            assertEquals(mockConfig.digits, readConf.digits);
        }finally {
            if(testFile.exists()){
                testFile.delete();
            }
        }
    }

    /**
     * tests parsing a Configuration out of a String
     * what is used when loading from a remote mission
     *
     */
    @Test
    public void testStringParsing(){

        final String xml = "<?xml version='1.0' encoding='UTF-8' standalone='no' ?><Configuration><siteCode>777</siteCode><timeZone>1</timeZone><sensorType>1</sensorType><SensorLitresPerRound>3</SensorLitresPerRound><sensor_LF_Const>1</sensor_LF_Const><TimerSlot1Start>7</TimerSlot1Start><TimerSlot1Stop>8</TimerSlot1Stop><TimerSlot2Start>11</TimerSlot2Start><TimerSlot2Stop>12</TimerSlot2Stop><provider>Mock provider</provider><pinCode>1234</pinCode><Recipient>+391234567890</Recipient><originNum>orig-num</originNum><ntpAddress>191.232.434.31</ntpAddress><digits>8</digits></Configuration>";

        final Context context = InstrumentationRegistry.getTargetContext();

        final Pair<Configuration, String> readResult = ConfigIO.readAndValidateConfig(context, xml);

        assertNotNull(readResult.second);
        assertEquals(readResult.second, "Configuration read with success");
    }
}
