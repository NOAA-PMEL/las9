package pmel.sdig.las.client.event;

import com.google.gwt.event.shared.GwtEvent;

public class Download extends GwtEvent<DownloadHandler> {

    boolean open;

    public Download(boolean open) {
        this.open = open;
    }
    public static Type<DownloadHandler> TYPE = new Type<DownloadHandler>();

    public Type<DownloadHandler> getAssociatedType() {
        return TYPE;
    }

    protected void dispatch(DownloadHandler handler) {
        handler.onDownload(this);
    }
    public boolean isOpen() {
        return open;
    }
}
