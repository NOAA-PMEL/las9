package pmel.sdig.las.client.widget;

import com.google.gwt.event.shared.GwtEvent;
import pmel.sdig.las.shared.autobean.Region;

public class RegionSelect extends GwtEvent<RegionSelectHandler> {

    Region region;
    int targetPanel;

    public RegionSelect(Region region, int targetPanel ) {
        this.region = region;
        this.targetPanel = targetPanel;
    }
    public static Type<RegionSelectHandler> TYPE = new Type<RegionSelectHandler>();

    public Type<RegionSelectHandler> getAssociatedType() {
        return TYPE;
    }

    protected void dispatch(RegionSelectHandler handler) {
        handler.onRegionSelect(this);
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public int getTargetPanel() {
        return targetPanel;
    }

    public void setTargetPanel(int targetPanel) {
        this.targetPanel = targetPanel;
    }
}
