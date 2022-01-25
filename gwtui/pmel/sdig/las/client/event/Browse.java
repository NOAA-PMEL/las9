package pmel.sdig.las.client.event;

import com.google.gwt.event.shared.GwtEvent;

public class Browse extends GwtEvent<BrowseHandler> {

    int offset;

    public Browse(int offset) {
        this.offset = offset;
    }

    public static Type<BrowseHandler> TYPE = new Type<BrowseHandler>();

    public Type<BrowseHandler> getAssociatedType() {
        return TYPE;
    }

    protected void dispatch(BrowseHandler handler) {
        handler.onBrowse(this);
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }
}
