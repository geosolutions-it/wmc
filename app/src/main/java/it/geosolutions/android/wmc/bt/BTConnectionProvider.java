package it.geosolutions.android.wmc.bt;

import android.bluetooth.BluetoothDevice;

import java.io.IOException;

import it.geosolutions.android.wmc.wmc.WMCFacade;

/**
 * Created by Robert Oehler on 13.11.16.
 *
 */

public interface BTConnectionProvider {


    void connect(final BluetoothDevice device, final WMCFacade.ConnectionListener listener);

    void disconnect(final WMCFacade.ConnectionListener listener);

    void write(byte[] bytes) throws IOException;

    void write(byte[] bytes, int startIndex, int length) throws IOException;

    byte read() throws IOException;

    boolean isConnected();

}
