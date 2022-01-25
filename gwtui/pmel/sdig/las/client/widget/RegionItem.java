package pmel.sdig.las.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.EventBus;
import gwt.material.design.client.constants.Color;
import gwt.material.design.client.constants.Display;
import gwt.material.design.client.constants.IconPosition;
import gwt.material.design.client.constants.IconType;
import gwt.material.design.client.ui.MaterialCheckBox;
import gwt.material.design.client.ui.MaterialCollectionItem;
import gwt.material.design.client.ui.MaterialIcon;
import gwt.material.design.client.ui.MaterialLink;
import gwt.material.design.client.ui.MaterialPanel;
import gwt.material.design.client.ui.MaterialRadioButton;
import pmel.sdig.las.client.event.AddVariable;
import pmel.sdig.las.client.event.Info;
import pmel.sdig.las.client.event.NavSelect;
import pmel.sdig.las.client.main.ClientFactory;
import pmel.sdig.las.shared.autobean.Dataset;
import pmel.sdig.las.shared.autobean.Region;
import pmel.sdig.las.shared.autobean.Variable;
import pmel.sdig.las.shared.autobean.Vector;

/**
 * Created by rhs on 12/30/16.
 */
public class RegionItem extends MaterialCollectionItem {

    Region selection;
    int targetPanel;

    // Data sets are links.
    MaterialLink link = new MaterialLink();
    MaterialPanel wrapper = new MaterialPanel();

    // Only radio buttons here
    MaterialRadioButton radio = new MaterialRadioButton();

    // Then variables turn into check boxes
    MaterialCheckBox check = new MaterialCheckBox();

    ClientFactory clientFactory = GWT.create(ClientFactory.class);
    EventBus eventBus = clientFactory.getEventBus();

    public RegionItem(Region selection, int targetPanel) {

        super();
        setLineHeight(22);
        this.selection = selection;
        this.targetPanel = targetPanel;

        radio.setName("regions_" + targetPanel);
        radio.setText(selection.getTitle());
        radio.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> valueChangeEvent) {
                eventBus.fireEventFromSource(new RegionSelect(selection, targetPanel), selection);
            }
        });
        add(radio);

    }

    public void setRadioSelected() {
        radio.setValue(true);
    }

    public boolean isSelected() {

        return radio.getValue();

    }

    public String getTitle() {
        if ( selection != null ) {
            return selection.getTitle();
        } else {
            return "";
        }
    }

    public Region getSelection() {
        return selection;
    }

    public boolean getRadioSelected() {
        return radio.getValue();
    }

    public void setSelected() {
        radio.setValue(true);
    }
}
