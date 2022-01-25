package pmel.sdig.las.client.event;

import com.google.gwt.event.shared.EventHandler;
import pmel.sdig.las.client.event.Search;

public interface SearchHandler extends EventHandler {
    void onSearch(Search event);
}
