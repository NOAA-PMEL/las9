package pmel.sdig.las.client.event;

import com.google.gwt.event.shared.EventHandler;

public interface AnimateActionHandler extends EventHandler {
    void onSetupAnimate(AnimateAction event);
}
