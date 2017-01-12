package it.geosolutions.android.wmc.util;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.support.v4.util.Pair;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;

import it.geosolutions.android.wmc.R;
import it.geosolutions.android.wmc.model.Configuration;

/**
 * Created by Robert Oehler on 07.11.16.
 *
 */

public class ConfigIO {

    private final static String TAG = "XMLConfigParser";

    private final static String CONFIGURATION = "Configuration";
    private final static String SITECODE = "siteCode";
    private final static String TIMEZONE = "timeZone";
    private final static String SENSORTYPE = "sensorType";
    private final static String SENSOR_LITRES_PER_ROUND = "SensorLitresPerRound";
    private final static String SENSORCONST = "sensor_LF_Const";
    private final static String TIMERSLOT_1_START = "TimerSlot1Start";
    private final static String TIMERSLOT_1_STOP  = "TimerSlot1Stop";
    private final static String TIMERSLOT_2_START = "TimerSlot2Start";
    private final static String TIMERSLOT_2_STOP  = "TimerSlot2Stop";
    private final static String PROVIDER = "provider";
    private final static String PINCODE = "pinCode";
    private final static String RECIPIENT = "Recipient";
    private final static String ORIGINNUM = "originNum";
    private final static String NTPADDRESS = "ntpAddress";
    private final static String DIGITS = "digits";

    /**
     ////////////// CONFIG read/write local/server /////////////////
     */

    /**
     * writes the current configuration to a file on disk
     * the target is selected by the user @param uri
     * @param uri target file
     */
    public static boolean writeConfigToDisk(final Uri uri, final ContentResolver contentResolver, final Configuration configuration){

        //activity checked before that config != null
        final String config = configToXml(configuration);

        if(config == null){
            return false;
        }

        OutputStream os = null;
        try {
            os = contentResolver.openOutputStream(uri);
            if(os == null){
                return false;
            }
            os.write(config.getBytes());
            os.flush();
            os.close();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "error writing file", e);
            return false;
        } finally {
            if(os != null){
                try {
                    os.close();
                } catch (IOException e) {
                    Log.e(TAG, "error closing outputStream", e);
                }
            }
        }
    }

    public static Pair<Configuration, String> readAndValidateConfig(final Context context, final String path2File){

        final Configuration newConfig = xmlToConfig(path2File);

        if(newConfig == null){
            //not a configuration
            return new Pair<Configuration, String>(null, context.getString(R.string.state_read_failure));
        }

        final Pair<Boolean, String> validationResult = validateConfig(context, newConfig);
        if(validationResult.first){
            //success
            return new Pair<Configuration, String>(newConfig, context.getString(R.string.state_read_success));
        }

        if(validationResult.second != null){
            //if we have an error message use it
            return new Pair<Configuration, String>(null, validationResult.second);
        }else{
            //else report a generic read error
            return new Pair<Configuration, String>(null, context.getString(R.string.state_read_failure));
        }


    }

    public static Pair<Boolean, String> validateConfig(final Context context, final Configuration configuration){

        if(configuration.recipientNum == null || TextUtils.isEmpty(configuration.recipientNum)){
            return new Pair<Boolean, String>(false, context.getString(R.string.state_recipient_null));
        } else if(configuration.originNum == null || TextUtils.isEmpty(configuration.originNum)){
            return new Pair<Boolean, String>(false, context.getString(R.string.state_origin_null));
        } else if(configuration.ntpAddress == null || TextUtils.isEmpty(configuration.ntpAddress) || configuration.ntpAddress.length() < Configuration.NTP_MIN_LENGTH){
            return new Pair<Boolean, String>(false, context.getString(R.string.state_time_server_invalid));
        } else if(configuration.pinCode == null || TextUtils.isEmpty(configuration.pinCode) || configuration.pinCode.length() != 4){
            return new Pair<Boolean, String>(false, context.getString(R.string.state_pin_code_invalid));
        } else if(ConfigIO.parseInt(configuration.pinCode) < 0 || ConfigIO.parseInt(configuration.pinCode) > 9999){
            return new Pair<Boolean, String>(false, context.getString(R.string.state_pin_code_not_numeric));
        } else if(configuration.provider == null || TextUtils.isEmpty(configuration.provider)){
            return new Pair<Boolean, String>(false, context.getString(R.string.state_provider_null));
        } else if(configuration.timerSlot1Start < 0){
            return new Pair<Boolean, String>(false, context.getString(R.string.state_time_slot_start_null, context.getString(R.string.wmc_ts_1)));
        } else if(configuration.timerSlot1Start > 23){
            return new Pair<Boolean, String>(false, context.getString(R.string.state_time_slot_start_oob, context.getString(R.string.wmc_ts_1)));
        } else if(configuration.timerSlot1Stop < 0){
            return new Pair<Boolean, String>(false, context.getString(R.string.state_time_slot_end_null, context.getString(R.string.wmc_ts_1)));
        } else if(configuration.timerSlot1Stop > 23){
            return new Pair<Boolean, String>(false, context.getString(R.string.state_time_slot_end_oob, context.getString(R.string.wmc_ts_1)));
        } else if(configuration.timerSlot2Start < 0){
            return new Pair<Boolean, String>(false, context.getString(R.string.state_time_slot_start_null, context.getString(R.string.wmc_ts_2)));
        }  else if(configuration.timerSlot2Start > 23){
            return new Pair<Boolean, String>(false, context.getString(R.string.state_time_slot_start_oob, context.getString(R.string.wmc_ts_2)));
        } else if(configuration.timerSlot2Stop < 0){
            return new Pair<Boolean, String>(false, context.getString(R.string.state_time_slot_end_null, context.getString(R.string.wmc_ts_2)));
        } else if(configuration.timerSlot2Stop > 23){
            return new Pair<Boolean, String>(false, context.getString(R.string.state_time_slot_end_oob, context.getString(R.string.wmc_ts_2)));
        } else if(configuration.sensorLitresRound < 0){
            return new Pair<Boolean, String>(false, context.getString(R.string.state_hf_litres_null));
        } else if(configuration.sensorLitresRound > 1000){
            return new Pair<Boolean, String>(false, context.getString(R.string.state_hf_litres_oob));
        } else if(configuration.sensor_LF_Const < 0){
            return new Pair<Boolean, String>(false, context.getString(R.string.state_lf_const_null));
        } else if(configuration.sensorLitresRound > 1000){
            return new Pair<Boolean, String>(false, context.getString(R.string.state_lf_const_oob));
        }

        //TODO the windows code does not validate :

        //digits
        //sensorType

        return new Pair<Boolean, String>(true, null);

    }

    public static String configToXml(final Configuration config){

        XmlSerializer serializer = Xml.newSerializer();
        StringWriter writer = new StringWriter();
        String xml = null;
        try {
            serializer.setOutput(writer);
            serializer.startDocument("UTF-8", false);
            serializer.startTag("", CONFIGURATION);


            //TODO what are invalid values ? 0 for all, also timezone ?
            serializer.startTag("", SITECODE);
            if(config.siteCode != 0) {
                serializer.text(Integer.toString(config.siteCode));
            }
            serializer.endTag("", SITECODE);

            serializer.startTag("", TIMEZONE);
            serializer.text(Integer.toString(config.timeZone));
            serializer.endTag("", TIMEZONE);

            serializer.startTag("", SENSORTYPE);
            if(config.sensorType != 0) {
                serializer.text(Integer.toString(config.sensorType));
            }
            serializer.endTag("", SENSORTYPE);

            serializer.startTag("", SENSOR_LITRES_PER_ROUND);
            if(config.sensorLitresRound != 0) {
                serializer.text(Integer.toString(config.sensorLitresRound));
            }
            serializer.endTag("", SENSOR_LITRES_PER_ROUND);

            serializer.startTag("", SENSORCONST);
            if(config.sensor_LF_Const != 0) {
                serializer.text(Integer.toString(config.sensor_LF_Const));
            }
            serializer.endTag("", SENSORCONST);

            serializer.startTag("", TIMERSLOT_1_START);
            if(config.timerSlot1Start != 0) {
                serializer.text(Integer.toString(config.timerSlot1Start));
            }
            serializer.endTag("", TIMERSLOT_1_START);

            serializer.startTag("", TIMERSLOT_1_STOP);
            if(config.timerSlot1Stop != 0) {
                serializer.text(Integer.toString(config.timerSlot1Stop));
            }
            serializer.endTag("", TIMERSLOT_1_STOP);

            serializer.startTag("", TIMERSLOT_2_START);
            if(config.timerSlot2Start != 0) {
                serializer.text(Integer.toString(config.timerSlot2Start));
            }
            serializer.endTag("", TIMERSLOT_2_START);

            serializer.startTag("", TIMERSLOT_2_STOP);
            if(config.timerSlot2Stop != 0) {
                serializer.text(Integer.toString(config.timerSlot2Stop));
            }
            serializer.endTag("", TIMERSLOT_2_STOP);

            serializer.startTag("", PROVIDER);
            if(config.provider != null) {
                serializer.text(config.provider);
            }
            serializer.endTag("", PROVIDER);

            serializer.startTag("", PINCODE);
            if(config.pinCode != null) {
                serializer.text(config.pinCode);
            }
            serializer.endTag("", PINCODE);

            serializer.startTag("", RECIPIENT);
            if(config.recipientNum != null) {
                serializer.text(config.recipientNum);
            }
            serializer.endTag("", RECIPIENT);

            serializer.startTag("", ORIGINNUM);
            if(config.originNum != null) {
                serializer.text(config.originNum);
            }
            serializer.endTag("", ORIGINNUM);

            serializer.startTag("", NTPADDRESS);
            if(config.ntpAddress != null) {
                serializer.text(config.ntpAddress);
            }
            serializer.endTag("", NTPADDRESS);

            serializer.startTag("", DIGITS);
            if(config.digits != 0) {
                serializer.text(Integer.toString(config.digits));
            }
            serializer.endTag("", DIGITS);

            serializer.endTag("", CONFIGURATION);
            serializer.endDocument();

            xml = writer.toString();
        } catch (Exception e) {
            Log.e(TAG,"error writing config to xml", e);
        }
        Log.i(TAG, "written : \n" + xml);

        return xml;
    }

    /**
     * converts a XML file or a String containing XML into a configuration object
     * @param xml the path to the file or the String containing the XML
     * @return the configuration or null if the String was not conform to the
     */
    private static Configuration xmlToConfig(final String xml){

        if(xml == null){
            return null;
        }

        Configuration newConfig = null;

        final File file = new File(xml);
        final boolean isFile = file != null && file.exists();

        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(false);
            XmlPullParser xpp = factory.newPullParser();

            if(isFile){
                xpp.setInput(new FileInputStream(new File(xml)), null);
            }else{
                xpp.setInput(new StringReader(xml));
            }

            int eventType = xpp.getEventType();
            String text = null;

            while (eventType != XmlPullParser.END_DOCUMENT) {

                final String tag = xpp.getName();

                if(eventType == XmlPullParser.START_TAG) {

                    if(tag.equalsIgnoreCase(CONFIGURATION)){
                        newConfig = new Configuration();
                    }

                } else if(eventType == XmlPullParser.END_TAG) {

                    if(newConfig == null){
                        //this is not a configuration file if the start tag did not contain a Configuration tag
                        break;
                    }

                    if (tag.equalsIgnoreCase(SITECODE)){
                        if(text != null && !TextUtils.isEmpty(text)){
                            int value = parseInt(text);
                            if(value != - 1 && newConfig != null) {
                                newConfig.siteCode = value;
                            }
                        }
                    } else if (tag.equalsIgnoreCase(TIMEZONE)){
                        if(text != null && !TextUtils.isEmpty(text)){
                            int value = parseInt(text);
                            if(value != - 1 && newConfig != null) {
                                newConfig.timeZone = value;
                            }
                        }
                    } else if (tag.equalsIgnoreCase(SENSORTYPE)){
                        if(text != null && !TextUtils.isEmpty(text)){
                            int value = parseInt(text);
                            if(value != - 1 && newConfig != null) {
                                newConfig.sensorType = value;
                            }
                        }
                    } else if (tag.equalsIgnoreCase(SENSOR_LITRES_PER_ROUND)){
                        if(text != null && !TextUtils.isEmpty(text)){
                            int value = parseInt(text);
                            if(value != - 1 && newConfig != null) {
                                newConfig.sensorLitresRound = value;
                            }
                        }
                    } else if (tag.equalsIgnoreCase(SENSORCONST)){
                        if(text != null && !TextUtils.isEmpty(text)){
                            int value = parseInt(text);
                            if(value != - 1 && newConfig != null) {
                                newConfig.sensor_LF_Const = value;
                            }
                        }
                    } else if (tag.equalsIgnoreCase(TIMERSLOT_1_START)){
                        if(text != null && !TextUtils.isEmpty(text)){
                            int value = parseInt(text);
                            if(value != - 1 && newConfig != null) {
                                newConfig.timerSlot1Start = value;
                            }
                        }
                    } else if (tag.equalsIgnoreCase(TIMERSLOT_1_STOP)){
                        if(text != null && !TextUtils.isEmpty(text)){
                            int value = parseInt(text);
                            if(value != - 1 && newConfig != null) {
                                newConfig.timerSlot1Stop = value;
                            }
                        }
                    } else if (tag.equalsIgnoreCase(TIMERSLOT_2_START)){
                        if(text != null && !TextUtils.isEmpty(text)){
                            int value = parseInt(text);
                            if(value != - 1 && newConfig != null) {
                                newConfig.timerSlot2Start = value;
                            }
                        }
                    } else if (tag.equalsIgnoreCase(TIMERSLOT_2_STOP)){
                        if(text != null && !TextUtils.isEmpty(text)){
                            int value = parseInt(text);
                            if(value != - 1 && newConfig != null) {
                                newConfig.timerSlot2Stop = value;
                            }
                        }
                    } else if (tag.equalsIgnoreCase(PROVIDER)){
                        if(text != null && !TextUtils.isEmpty(text) && newConfig != null){

                            newConfig.provider = text;
                        }
                    } else if (tag.equalsIgnoreCase(PINCODE)){
                        if(text != null && !TextUtils.isEmpty(text) && newConfig != null){

                            newConfig.pinCode = text;
                        }
                    } else if (tag.equalsIgnoreCase(RECIPIENT)){
                        if(text != null && !TextUtils.isEmpty(text) && newConfig != null){

                            newConfig.recipientNum = text;
                        }
                    }else if (tag.equalsIgnoreCase(ORIGINNUM)){
                        if(text != null && !TextUtils.isEmpty(text) && newConfig != null){

                            newConfig.originNum = text;
                        }
                    }else if (tag.equalsIgnoreCase(NTPADDRESS)){
                        if(text != null && !TextUtils.isEmpty(text) && newConfig != null){

                            newConfig.ntpAddress = text;
                        }
                    }else if (tag.equalsIgnoreCase(DIGITS)){
                        if(text != null && !TextUtils.isEmpty(text)){
                            int value = parseInt(text);
                            if(value != - 1 && newConfig != null) {
                                newConfig.digits = value;
                            }
                        }
                    }

                } else if(eventType == XmlPullParser.TEXT) {
                    text = xpp.getText();
                }
                eventType = xpp.next();
            }

        } catch (Exception e) {
            Log.e(TAG,"error parsing xml file", e);
        }

        return newConfig;
    }

    private static int parseInt(final String text){
        try{
            return Integer.parseInt(text);
        }catch (NumberFormatException e){
            Log.e(TAG,"error parsing "+ text);
            return -1;
        }
    }

}
