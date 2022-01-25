package pmel.sdig.las.client.event;

import com.google.gwt.event.shared.EventHandler;

public interface AddVariableHandler extends EventHandler {
    void onAddVariable(AddVariable event);
}
