package pmel.sdig.las.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import gwt.material.design.client.ui.MaterialPanel;
import pmel.sdig.las.shared.autobean.MenuOption;
import pmel.sdig.las.shared.autobean.RequestProperty;

import java.util.ArrayList;
import java.util.List;

public class MenuOptionsWidget extends Composite {
    @UiField
    MaterialPanel menuoptions;

    interface MenuOptionsWidgetUiBinder extends UiBinder<MaterialPanel, MenuOptionsWidget> {
    }

    private static MenuOptionsWidgetUiBinder ourUiBinder = GWT.create(MenuOptionsWidgetUiBinder.class);

    public MenuOptionsWidget() {
        initWidget(ourUiBinder.createAndBindUi(this));
    }
    public MenuOptionsWidget(List<MenuOption> options) {
        initWidget(ourUiBinder.createAndBindUi(this));
        for (int i = 0; i < options.size(); i++) {
            MenuOption option = options.get(i);
            MenuOptionItem item = new MenuOptionItem(option);
            menuoptions.add(item);
        }
    }
    public List<RequestProperty> getOptions() {
        List<RequestProperty> olist = new ArrayList<>();
        for ( int i = 0; i < menuoptions.getChildrenList().size(); i++ ) {
            MenuOptionItem moi = (MenuOptionItem) menuoptions.getChildrenList().get(i);
            MenuOption option = moi.getOption();
            String value = moi.getSelectedValue();
            String defaultValue = option.getDefaultValue();
            // Always send the land type cause I want it different that the scripts.
            if ( value != null && !value.equals(defaultValue) || option.getName().equals("land_type") ) {
                RequestProperty rp = new RequestProperty();
                rp.setType("ferret");
                rp.setName(option.getName());
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
        for ( int i = 0; i < menuoptions.getChildrenList().size(); i++ ) {
            MenuOptionItem moi = (MenuOptionItem) menuoptions.getChildrenList().get(i);
            MenuOption option = moi.getOption();
            if ( option.getName().equals(property.getName())) {
                return true;
            }
        }
        return false;
    }
    public void setProperty(RequestProperty property) {
        for ( int i = 0; i < menuoptions.getChildrenList().size(); i++ ) {
            MenuOptionItem moi = (MenuOptionItem) menuoptions.getChildrenList().get(i);
            MenuOption option = moi.getOption();
            if (option.getName().equals(property.getName())) {
                moi.setSelectedValue(property.getValue());
            }
        }
    }
}
