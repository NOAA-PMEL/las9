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
import pmel.sdig.las.shared.autobean.Variable;
import pmel.sdig.las.shared.autobean.Vector;

/**
 * Created by rhs on 12/30/16.
 */
public class DataItem extends MaterialCollectionItem {

    String currentSelectionType = "radio";
    Object selection;

    // Data sets are links.
    MaterialLink link = new MaterialLink();
    MaterialIcon badge = new MaterialIcon(IconType.INFO);
    MaterialPanel wrapper = new MaterialPanel();

    // Variables are radio buttons until the plot type allows multiple selections
    MaterialRadioButton radio = new MaterialRadioButton();

    // Then variables turn into check boxes
    MaterialCheckBox check = new MaterialCheckBox();

    ClientFactory clientFactory = GWT.create(ClientFactory.class);
    EventBus eventBus = clientFactory.getEventBus();
    public DataItem(Object selection, int targetPanel) {
        super();
        this.selection = selection;

        if ( selection instanceof Dataset ) {
            Dataset d = (Dataset) selection;

            wrapper.addStyleName("valign-wrapper");
            link.setMarginLeft(8);
            link.setDisplay(Display.FLEX);

            if ( d.hasVariableChildren() ) {
                badge.setIconPosition(IconPosition.RIGHT);
                badge.setDisplay(Display.FLEX);
                badge.setMarginRight(4);
                badge.addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        eventBus.fireEventFromSource(new Info(d.getId()), badge);
                    }
                });
            } else {
                badge.setDisplay(Display.NONE);
            }

            wrapper.add(link);
            wrapper.add(badge);

            link.setText(d.getTitle());

            link.addStyleName("LAS-text-color");
            add(wrapper);

            link.setVerticalAlign(Style.VerticalAlign.MIDDLE);
            link.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent clickEvent) {
                    eventBus.fireEventFromSource(new NavSelect(selection, targetPanel), selection);
                }
            });

        } else if ( selection instanceof Variable) {
            Variable v = (Variable) selection;
            check.setText(v.getTitle());
            check.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
                @Override
                public void onValueChange(ValueChangeEvent<Boolean> valueChangeEvent) {
                    if ( valueChangeEvent.getValue() ) {
                        eventBus.fireEventFromSource(new AddVariable((Variable) selection, targetPanel, true), selection);
                    } else {
                        eventBus.fireEventFromSource(new AddVariable((Variable) selection, targetPanel, false), selection);
                    }
                }
            });
            radio.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
                @Override
                public void onValueChange(ValueChangeEvent<Boolean> valueChangeEvent) {
                    if ( valueChangeEvent.getValue() ) {
                        eventBus.fireEventFromSource(new NavSelect(selection, targetPanel), selection);
                    }
                }
            });

            radio.setName("variable_" + targetPanel);
            radio.setText(v.getTitle());
            add(radio);
        } else if ( selection instanceof Vector) {
            Vector v = (Vector) selection;
            radio.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
                @Override
                public void onValueChange(ValueChangeEvent<Boolean> valueChangeEvent) {
                    eventBus.fireEventFromSource(new NavSelect(selection, targetPanel), selection);
                }
            });
            radio.setName("variable_" + targetPanel);
            radio.setText(v.getTitle());
            add(radio);
        }
    }
    public void setIconColor(Color color) {
        badge.setIconColor(color);
    }
    public void toCheck() {
        remove(radio);
        if ( radio.getValue() ) {
            check.setValue(true);
        }
        add(check);
        radio.setValue(false);
        currentSelectionType = "check";
    }
    public void toRadio() {
        remove(check);
        if ( check.getValue() ) {
            radio.setValue(true);
        }
        check.setValue(false);
        add(radio);
        currentSelectionType = "radio";
    }
    public void setRadioSelected() {
        radio.setValue(true);
    }
    public boolean isSelected() {
        if ( currentSelectionType.equalsIgnoreCase("radio") ) {
            return radio.getValue();
        } else if ( currentSelectionType.equalsIgnoreCase("check") ) {
            return check.getValue();
        }
        return false;
    }
    public String getTitle() {
        if ( selection instanceof Dataset ) {
            Dataset d = (Dataset) selection;
            return d.getTitle();
        } else {
            Variable v = (Variable) selection;
            return v.getTitle();
        }
    }

    public Object getSelection() {
        return selection;
    }
    public void setSelected() {
        if ( radio.isAttached() ) {
            radio.setValue(true);
        } else if ( check.isAttached() ) {
            check.setValue(true);
        }
    }
}
