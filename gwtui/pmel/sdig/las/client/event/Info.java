package pmel.sdig.las.client.event;

import com.google.gwt.event.shared.GwtEvent;

public class Info extends GwtEvent<InfoHandler> {

    long id;

    public static Type<InfoHandler> TYPE = new Type<InfoHandler>();

    public Type<InfoHandler> getAssociatedType() {
        return TYPE;
    }

    protected void dispatch(InfoHandler handler) {
        handler.onInfo(this);
    }

    public Info(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
