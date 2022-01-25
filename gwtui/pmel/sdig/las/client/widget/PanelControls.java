package pmel.sdig.las.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;

public class PanelControls {
    interface PanelControlsUiBinder extends UiBinder<com.google.gwt.dom.client.DivElement, pmel.sdig.las.client.widget.PanelControls> {
    }

    private static PanelControlsUiBinder ourUiBinder = GWT.create(PanelControlsUiBinder.class);

    public PanelControls() {
        com.google.gwt.dom.client.DivElement rootElement = ourUiBinder.createAndBindUi(this);
    }
}