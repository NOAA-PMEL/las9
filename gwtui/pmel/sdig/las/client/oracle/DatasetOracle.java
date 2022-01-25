package pmel.sdig.las.client.oracle;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import gwt.material.design.addins.client.autocomplete.base.MaterialSuggestionOracle;
import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;
import org.fusesource.restygwt.client.Resource;
import org.fusesource.restygwt.client.RestService;
import org.fusesource.restygwt.client.RestServiceProxy;
import pmel.sdig.las.client.util.Constants;
import pmel.sdig.las.shared.autobean.SuggestQuery;
import pmel.sdig.las.shared.autobean.LASSuggestion;

import javax.ws.rs.POST;
import java.util.List;

public class DatasetOracle extends MaterialSuggestionOracle {

    Callback callback;
    Request request;

    // The name of the parameter the query will search
    String name;

    public DatasetOracle(String name) {
        super();
        ((RestServiceProxy)datasetSuggestService).setResource(datasetSuggestResource);
        this.name = name;
    }

    public interface DatasetSuggestService extends RestService {
        @POST
        public void getDatasetSuggestions(SuggestQuery query, MethodCallback<List<LASSuggestion>> suggestions);
    }

    Resource datasetSuggestResource = new Resource(Constants.datasetSuggestSearch);
    DatasetSuggestService datasetSuggestService = GWT.create(DatasetSuggestService.class);

    public DatasetOracle() {
        super();
        ((RestServiceProxy)datasetSuggestService).setResource(datasetSuggestResource);
    }

    @Override
    public void requestSuggestions(Request request, Callback callback) {
        this.request = request;
        this.callback = callback;
        SuggestQuery sq = new SuggestQuery();
        sq.setQuery(request.getQuery().toLowerCase());
        sq.setName(name);
        datasetSuggestService.getDatasetSuggestions(sq, processSuggestions);
    }
    MethodCallback<List<LASSuggestion>> processSuggestions = new MethodCallback<List<LASSuggestion>>() {
        @Override
        public void onFailure(Method method, Throwable throwable) {
        }

        @Override
        public void onSuccess(Method method, List<LASSuggestion> lasSuggestions) {
            Response response= new Response();
            response.setSuggestions(lasSuggestions);
            callback.onSuggestionsReady(request, response);
        }
    };
}
