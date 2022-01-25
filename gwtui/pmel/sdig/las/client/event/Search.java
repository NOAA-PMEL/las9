package pmel.sdig.las.client.event;

import com.google.gwt.event.shared.GwtEvent;
import pmel.sdig.las.shared.autobean.DatasetProperty;
import pmel.sdig.las.shared.autobean.SearchRequest;
import pmel.sdig.las.shared.autobean.VariableProperty;

import java.util.List;

public class Search extends GwtEvent<SearchHandler> {

    SearchRequest searchRequest;

    public Search(SearchRequest searchRequest) {
        this.searchRequest = searchRequest;
    }

    public static Type<SearchHandler> TYPE = new Type<SearchHandler>();

    public Type<SearchHandler> getAssociatedType() {
        return TYPE;
    }

    protected void dispatch(SearchHandler handler) {
        handler.onSearch(this);
    }

    public SearchRequest getSearchRequest() {
        return searchRequest;
    }

    public void setSearchRequest(SearchRequest searchRequest) {
        this.searchRequest = searchRequest;
    }
}
