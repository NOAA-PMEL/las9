package pmel.sdig.las.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import gwt.material.design.client.ui.MaterialColumn;
import gwt.material.design.client.ui.MaterialLink;
import gwt.material.design.client.ui.MaterialPanel;
import gwt.material.design.client.ui.MaterialSwitch;
import pmel.sdig.las.shared.autobean.YesNoOption;

public class YesNoOptionItem extends Composite {
    @UiField
    MaterialSwitch ynswitch;
    @UiField
    MaterialColumn help;
    @UiField
    MaterialLink title;

    HTML hhelp;

    YesNoOption yno;

    interface YesNoOptionItemUiBinder extends UiBinder<MaterialPanel, YesNoOptionItem> {
    }

    private static YesNoOptionItemUiBinder ourUiBinder = GWT.create(YesNoOptionItemUiBinder.class);

    public YesNoOptionItem() {
        MaterialPanel rootElement = ourUiBinder.createAndBindUi(this);
        initWidget(rootElement);
    }
    public YesNoOptionItem(YesNoOption yno) {
        MaterialPanel rootElement = ourUiBinder.createAndBindUi(this);
        this.yno = yno;
        initWidget(rootElement);
        ynswitch.setOffLabel("No");
        ynswitch.setOnLabel("Yes");
        title.setText(yno.getTitle());
        hhelp = new HTML(yno.getHelp(), true);
        help.add(hhelp);
        String yes = yno.getDefaultValue();
        if ( yes.equals("yes") ) {
            ynswitch.setValue(true);
        } else {
            ynswitch.setValue(false);
        }
    }
    public String getValue() {
        boolean b = ynswitch.getValue();
        if ( b ) {
            return "1";
        } else {
            return "0";
        }
    }
    public void setValue(boolean b) {
        ynswitch.setValue(b);
    }
    public YesNoOption getOption() {
        return yno;
    }
}