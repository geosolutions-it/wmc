package it.geosolutions.android.wmc.service.events;

/**
 * Created by Robert Oehler on 21.11.16.
 *
 */

public class WMCConnectionStateChangedEvent {

    public enum ConnectionState
    {
        CONNECTED,
        DISCONNECTED,
        CONNECTION_ERROR
    }

    private ConnectionState state;
    private String wmcName;

    public WMCConnectionStateChangedEvent(final ConnectionState state){

        this.state = state;
    }

    public void setWmcName(String wmcName) {
        this.wmcName = wmcName;
    }

    public String getWmcName() {
        return wmcName;
    }

    public boolean isConnected() {
        return state == ConnectionState.CONNECTED;
    }

    public ConnectionState getState() {
        return state;
    }
}
