package pmel.sdig.las.client.event;

import com.google.gwt.event.shared.GwtEvent;

public class PlotOptionChange extends GwtEvent<PlotOptionChangeHandler> {
    public static Type<PlotOptionChangeHandler> TYPE = new Type<PlotOptionChangeHandler>();

    public Type<PlotOptionChangeHandler> getAssociatedType() {
        return TYPE;
    }

    protected void dispatch(PlotOptionChangeHandler handler) {
        handler.onPlotOptionChange(this);
    }
}
