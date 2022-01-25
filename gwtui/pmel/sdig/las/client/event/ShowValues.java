package pmel.sdig.las.client.event;

import com.google.gwt.event.shared.GwtEvent;

public class ShowValues extends GwtEvent<ShowValuesHandler> {
    public static Type<ShowValuesHandler> TYPE = new Type<ShowValuesHandler>();

    public Type<ShowValuesHandler> getAssociatedType() {
        return TYPE;
    }

    protected void dispatch(ShowValuesHandler handler) {
        handler.onShowValues(this);
    }
}
