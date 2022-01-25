package pmel.sdig.las.client.event;

import com.google.gwt.event.shared.EventHandler;

public interface DownloadHandler extends EventHandler {
    void onDownload(Download event);
}
