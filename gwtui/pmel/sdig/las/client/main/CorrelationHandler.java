package pmel.sdig.las.client.main;

import com.google.gwt.event.shared.EventHandler;

public interface CorrelationHandler extends EventHandler {
    void onCorrelation(Correlation event);
}
