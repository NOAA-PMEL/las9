package pmel.sdig.las.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import gwt.material.design.client.ui.MaterialColumn;
import gwt.material.design.client.ui.MaterialLink;
import gwt.material.design.client.ui.MaterialListBox;
import gwt.material.design.client.ui.MaterialPanel;
import gwt.material.design.client.ui.html.Option;
import pmel.sdig.las.client.event.PlotOptionChange;
import pmel.sdig.las.client.main.ClientFactory;
import pmel.sdig.las.shared.autobean.MenuItem;
import pmel.sdig.las.shared.autobean.MenuOption;

import java.util.List;

public class MenuOptionItem extends Composite {

    ClientFactory clientFactory = GWT.create(ClientFactory.class);
    EventBus eventBus = clientFactory.getEventBus();

    @UiField
    MaterialListBox menu;
    @UiField
    MaterialLink title;
    @UiField
    MaterialColumn help;

    MenuOption option;

    HTML helph;

    interface MenuOptionItemUiBinder extends UiBinder<MaterialPanel, MenuOptionItem> {}

    private static MenuOptionItemUiBinder ourUiBinder = GWT.create(MenuOptionItemUiBinder.class);

    public MenuOptionItem() {
        initWidget(ourUiBinder.createAndBindUi(this));
    }
    public MenuOptionItem(MenuOption option) {
        this.option = option;
        initWidget(ourUiBinder.createAndBindUi(this));
        String defaultValue = option.getDefaultValue();
        title.setText(option.getTitle());
        List<MenuItem> menuList = option.getMenuItems();
        for (int i = 0; i < menuList.size(); i++) {
            MenuItem item = menuList.get(i);
            Option menuLink = new Option();
            menuLink.setText(item.getTitle());
            menuLink.setValue(item.getValue());
            menu.add(menuLink);
        }

        if ( defaultValue != null ) {
            menu.setSelectedValue(defaultValue);
        }

        helph = new HTML(option.getHelp());

        help.add(helph);
        menu.addValueChangeHandler(new ValueChangeHandler<String>() {
            @Override
            public void onValueChange(ValueChangeEvent<String> event) {
                eventBus.fireEventFromSource(new PlotOptionChange(), MenuOptionItem.this);
            }
        });

    }
    public MenuOption getOption() {
        return option;
    }
    public String getSelectedValue() {
        return menu.getSelectedValue();
    }
    public void setSelectedValue(String value) {
        int index = menu.getSelectedIndex();
        for (int i = 0; i < menu.getItemCount(); i++) {
            String menuValue = menu.getListBox().getValue(i);
            if ( menuValue.equals(value) ) {
                index = i;
            }
        }
        menu.setSelectedIndex(index);
    }
}