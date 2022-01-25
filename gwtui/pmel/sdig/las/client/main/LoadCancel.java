package pmel.sdig.las.client.main;

import com.google.gwt.event.shared.GwtEvent;

public class LoadCancel extends GwtEvent<LoadCancelHandler> {
    public static Type<LoadCancelHandler> TYPE = new Type<LoadCancelHandler>();

    public Type<LoadCancelHandler> getAssociatedType() {
        return TYPE;
    }

    protected void dispatch(LoadCancelHandler handler) {
        handler.onLoadCancel(this);
    }
}
