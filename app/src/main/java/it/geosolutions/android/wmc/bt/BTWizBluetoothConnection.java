package it.geosolutions.android.wmc.bt;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import com.btwiz.library.BTSocket;
import com.btwiz.library.BTWiz;
import com.btwiz.library.IDeviceConnectionListener;

import java.io.IOException;

import it.geosolutions.android.wmc.BuildConfig;
import it.geosolutions.android.wmc.wmc.WMCFacade;

/**
 * Created by Robert Oehler on 13.11.16.
 *
 */

public class BTWizBluetoothConnection implements BTConnectionProvider {

    private final static String TAG = "BTWizBtConnection";

    private BTSocket mSocket;
    private Context mContext;

    private boolean isConnected = false;

    public BTWizBluetoothConnection(final Context context){
        this.mContext = context;
    }

    @Override
    public void connect(final BluetoothDevice device, final WMCFacade.ConnectionListener listener) {

        BTWiz.connectAsClientAsync(mContext, device, new IDeviceConnectionListener() {
            @Override
            public void onConnectSuccess(BTSocket btSocket) {

                if(BuildConfig.DEBUG) {
                    Log.i(TAG, "onConnectSuccess");
                }

                if(!isConnected) {

                    isConnected = true;
                    mSocket = btSocket;
                    if (listener != null) {
                        listener.onDeviceConnected(device.getName());
                    }
                }
            }

            @Override
            public void onConnectionError(Exception e, String s) {

                Log.w(TAG, "onConnectionError "+ s);

                if(listener != null){
                    listener.onDeviceConnectionFailed(s);
                }
                isConnected = false;

            }
        });

    }

    @Override
    public void disconnect(WMCFacade.ConnectionListener listener) {

        if(isConnected ){

            BTWiz.cleanup(mContext);
            if(listener != null){
                listener.onDeviceDisconnected();
            }
            isConnected = false;
        }
    }

    @Override
    public void write(byte[] bytes) throws IOException {

        mSocket.write(bytes);
    }

    @Override
    public void write(byte[] bytes, int startIndex, int length) throws IOException {

        mSocket.write(bytes, startIndex, length);
    }

    @Override
    public byte read() throws IOException {
        return (byte) mSocket.read();
    }

    @Override
    public boolean isConnected() {
        return isConnected;
    }
}
