package it.geosolutions.android.wmc.service.events;

import it.geosolutions.android.wmc.model.Configuration;
import it.geosolutions.android.wmc.model.WMCReadResult;

/**
 * Created by Robert Oehler on 21.11.16.
 *
 */

public class WMCCommunicationResultEvent {

    private WMCCommand command;
    private String error;
    private boolean success;

    private WMCReadResult readResult;
    private Configuration configuration;
    private String deviceName;

    public WMCCommunicationResultEvent(final WMCCommand command){
        this.command = command;
        this.success = false;
    }

    public WMCCommand getCommand() {
        return command;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getError() {
        return error;
    }


    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setReadResult(WMCReadResult readResult) {
        this.readResult = readResult;
    }

    public WMCReadResult getReadResult() {
        return readResult;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceName() {
        return deviceName;
    }
}
