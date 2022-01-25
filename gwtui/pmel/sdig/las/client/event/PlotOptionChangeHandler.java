package pmel.sdig.las.client.event;

import com.google.gwt.event.shared.EventHandler;

public interface PlotOptionChangeHandler extends EventHandler {
    void onPlotOptionChange(PlotOptionChange event);
}
