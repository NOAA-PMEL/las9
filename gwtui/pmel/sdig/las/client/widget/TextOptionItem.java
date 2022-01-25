package pmel.sdig.las.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import gwt.material.design.client.ui.MaterialColumn;
import gwt.material.design.client.ui.MaterialLink;
import gwt.material.design.client.ui.MaterialPanel;
import gwt.material.design.client.ui.MaterialTextBox;
import pmel.sdig.las.client.main.ClientFactory;
import pmel.sdig.las.shared.autobean.TextOption;

/**
 * A this is the widget that holds a TextOption domain object from the server.
 * They are grouped together into the TextOptionWidget
 */

public class TextOptionItem extends Composite {

    interface TextOptionItemUiBinder extends UiBinder<MaterialPanel, TextOptionItem> {}
    private static TextOptionItemUiBinder ourUiBinder = GWT.create(TextOptionItemUiBinder.class);

    TextOption textOption;

    ClientFactory clientFactory = GWT.create(ClientFactory.class);
    EventBus eventBus = clientFactory.getEventBus();

    @UiField
    MaterialTextBox textbox;
    @UiField
    MaterialLink title;
    @UiField
    MaterialColumn help;

    HTML hhelp;

    public TextOptionItem() {
        MaterialPanel rootElement = ourUiBinder.createAndBindUi(this);
        initWidget(rootElement);
    }

    public TextOptionItem(TextOption textOption) {
        MaterialPanel rootElement = ourUiBinder.createAndBindUi(this);
        initWidget(rootElement);
        this.textOption = textOption;
        String hint = textOption.getHint();
        if ( hint != null ) {
            textbox.setPlaceholder(hint);
        }
        title.setText(textOption.getTitle());
        hhelp = new HTML(textOption.getHelp());
        help.add(hhelp);
    }

    public TextOption getOption() {
        return this.textOption;
    }

    public String getValue() {
        return textbox.getValue();
    }
    public void setValue(String value) {
        textbox.setText(value);
    }
}
