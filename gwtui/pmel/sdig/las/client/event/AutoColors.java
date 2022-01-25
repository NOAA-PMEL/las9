package pmel.sdig.las.client.event;

import com.google.gwt.event.shared.GwtEvent;

public class AutoColors extends GwtEvent<AutoColorsHandler> {
    public static Type<AutoColorsHandler> TYPE = new Type<AutoColorsHandler>();

    private boolean on;

    public AutoColors(boolean on) {
        this.on = on;
    }
    public Type<AutoColorsHandler> getAssociatedType() {
        return TYPE;
    }

    protected void dispatch(AutoColorsHandler handler) {
        handler.onAutoColors(this);
    }

    public void setOn(boolean on) {
        this.on = on;
    }
    public boolean isOn() {
        return on;
    }
}
