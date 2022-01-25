package pmel.sdig.las.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import gwt.material.design.client.constants.Display;
import gwt.material.design.client.ui.MaterialIcon;
import gwt.material.design.client.ui.MaterialLabel;
import gwt.material.design.client.ui.MaterialPanel;
import gwt.material.design.client.ui.MaterialSwitch;
import gwt.material.design.client.ui.MaterialTextBox;
import pmel.sdig.las.client.event.FeatureModifiedEvent;
import pmel.sdig.las.client.main.ClientFactory;
import pmel.sdig.las.client.main.Correlation;

public class VariableConstraintWidget extends Composite {

    @UiField
    MaterialTextBox lo;
    @UiField
    MaterialTextBox hi;
    @UiField
    MaterialLabel name;
    @UiField
    MaterialSwitch active;
    @UiField
    MaterialIcon remove;

    ClientFactory clientFactory = GWT.create(ClientFactory.class);
    EventBus eventBus = clientFactory.getEventBus();

    interface VariableConstraintUiBinder extends UiBinder<MaterialPanel, VariableConstraintWidget> {
    }

    private static VariableConstraintUiBinder ourUiBinder = GWT.create(VariableConstraintUiBinder.class);

    public VariableConstraintWidget() {
        MaterialPanel rootElement = ourUiBinder.createAndBindUi(this);
        initWidget(rootElement);
        remove.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                eventBus.fireEventFromSource(new Correlation(false, false, false, false, true), VariableConstraintWidget.this);
            }
        });
        active.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> valueChangeEvent) {
                eventBus.fireEventFromSource(new FeatureModifiedEvent(0, 0, 0, 0), this);
            }
        });
        lo.addValueChangeHandler(new ValueChangeHandler<String>() {
            @Override
            public void onValueChange(ValueChangeEvent<String> valueChangeEvent) {
                String text = valueChangeEvent.getValue();
                validDate(text);
            }
        });
        hi.addValueChangeHandler(new ValueChangeHandler<String>() {
            @Override
            public void onValueChange(ValueChangeEvent<String> valueChangeEvent) {
                String text = valueChangeEvent.getValue();
                validDate(text);
            }
        });
    }

    public void validDate(String value) {
        try {
            Double.valueOf(value);
        } catch (Exception e) {
            lo.setText("");
        }
        active.setValue(true);
        eventBus.fireEventFromSource(new FeatureModifiedEvent(0, 0, 0, 0), this);
    }
    public void setName(String name) {
        this.name.setText(name);
    }
    public String getName() {
        return this.name.getText();
    }

    public void setLo(String lo) {
        this.lo.setText(lo);
    }

    public String getLo() {
        return lo.getText();
    }

    public void setHi(String hi) {
        this.hi.setText(hi);
    }

    public String getHi() {
        return this.hi.getText();
    }

    public void setActive(boolean active) {
        this.active.setValue(active);
    }
    public boolean isActive() {
        return active.getValue();
    }
    public void showRemove() {
        remove.setDisplay(Display.INLINE_BLOCK);
    }
}