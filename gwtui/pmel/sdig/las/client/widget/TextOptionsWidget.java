package pmel.sdig.las.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import gwt.material.design.client.ui.MaterialPanel;
import pmel.sdig.las.shared.autobean.RequestProperty;
import pmel.sdig.las.shared.autobean.TextOption;

import java.util.ArrayList;
import java.util.List;

public class TextOptionsWidget extends Composite {

    @UiField
    MaterialPanel textoptions;

    interface TextOptionsWidgetUiBinder extends UiBinder<MaterialPanel, TextOptionsWidget> {
    }

    private static TextOptionsWidgetUiBinder ourUiBinder = GWT.create(TextOptionsWidgetUiBinder.class);

    public TextOptionsWidget() {
        MaterialPanel rootElement = ourUiBinder.createAndBindUi(this);
        initWidget(rootElement);
    }
    public TextOptionsWidget(List<TextOption> textOptions) {
        MaterialPanel rootElement = ourUiBinder.createAndBindUi(this);
        initWidget(rootElement);
        for (int i = 0; i < textOptions.size(); i++) {
            TextOption to = textOptions.get(i);
            TextOptionItem ti = new TextOptionItem(to);
            textoptions.add(ti);
        }
    }
    public List<RequestProperty> getOptions() {
        List<RequestProperty> olist = new ArrayList<>();
        for ( int i = 0; i < textoptions.getChildrenList().size(); i++ ) {
            TextOptionItem toi = (TextOptionItem) textoptions.getChildrenList().get(i);
            String value = toi.getValue();
            TextOption to = toi.getOption();
            if ( value != null && !value.equals("") ) {
                RequestProperty rp = new RequestProperty();
                rp.setType("ferret");
                rp.setName(to.getName());
                rp.setValue(value);
                olist.add(rp);
            }
        }
        return olist;
    }
    public boolean contains(RequestProperty property) {
        // Not a ferret property, not a plot option
        if ( !property.getType().equals("ferret") ) {
            return false;
        }
        for ( int i = 0; i < textoptions.getChildrenList().size(); i++ ) {
            TextOptionItem toi = (TextOptionItem) textoptions.getChildrenList().get(i);
            TextOption option = toi.getOption();
            if ( option.getName().equals(property.getName())) {
                return true;
            }
        }
        return false;
    }
    public void setProperty(RequestProperty property) {
        for ( int i = 0; i < textoptions.getChildrenList().size(); i++ ) {
            TextOptionItem toi = (TextOptionItem) textoptions.getChildrenList().get(i);
            TextOption option = toi.getOption();
            if (option.getName().equals(property.getName())) {
                toi.setValue(property.getValue());
            }
        }
    }
}