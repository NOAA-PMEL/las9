package pmel.sdig.las.client.map;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import gwt.material.design.client.constants.Color;
import gwt.material.design.client.constants.IconSize;
import gwt.material.design.client.constants.IconType;
import gwt.material.design.client.constants.WavesType;
import gwt.material.design.client.ui.MaterialIcon;

/**
 * Created by rhs on 10/24/17.
 */
public class MapPushButton extends MaterialIcon {
    private boolean down;
    public MapPushButton(IconType type, String title) {
        mapButton(type, title);
    }
    private void mapButton(IconType type, String title) {
        setWaves(WavesType.DEFAULT);
        setIconSize(IconSize.SMALL);
        setBackgroundColor(Color.BLUE);
        setIconColor(Color.WHITE);
        setShadow(2);
//        setHeight("19px");
//        setWidth("19px");
        setMargin(0);
        setPadding(0);
        setTitle(title);
        setIconType(type);
        setCircle(true);
    }

}
