package pmel.sdig.las.client.map;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import gwt.material.design.client.constants.Color;
import gwt.material.design.client.constants.IconSize;
import gwt.material.design.client.constants.IconType;
import gwt.material.design.client.constants.WavesType;
import gwt.material.design.client.ui.MaterialButton;
import gwt.material.design.client.ui.MaterialIcon;

/**
 * Created by rhs on 10/24/17.
 */
public class MapToggleButton extends MaterialIcon {
    private boolean down;
    public MapToggleButton(IconType type, String title) {
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
        addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                down = !down;
                if ( down ) {
                    setBackgroundColor(Color.GREEN);
                } else {
                    setBackgroundColor(Color.BLUE);
                }
            }
        });
    }
    public boolean isDown() {
        return down;
    }
    public void setDown(boolean down) {
        this.down = down;
        if ( down ) {
            setBackgroundColor(Color.GREEN);
        } else {
            setBackgroundColor(Color.BLUE);
        }
    }
}
