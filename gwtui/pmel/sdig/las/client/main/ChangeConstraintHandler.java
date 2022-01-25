package pmel.sdig.las.client.main;

import com.google.gwt.event.shared.EventHandler;

public interface ChangeConstraintHandler extends EventHandler {
    void onAddConstraint(ChangeConstraint event);
}
