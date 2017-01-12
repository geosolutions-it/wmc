package it.geosolutions.android.wmc.service.events;

/**
 * Created by Robert Oehler on 21.11.16.
 *
 */

public class RequestWMCDataEvent {

    private WMCCommand mCommand;
    private Object mArg;

    public RequestWMCDataEvent(final WMCCommand command, final Object arg){

        this.mCommand = command;
        this.mArg = arg;
    }

    public WMCCommand getCommand() {
        return mCommand;
    }

    public Object getArg() {
        return mArg;
    }
}
