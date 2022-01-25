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
import pmel.sdig.las.shared.autobean.LASSuggestion;
import pmel.sdig.las.shared.autobean.SuggestQuery;

import javax.ws.rs.POST;
import java.util.List;

public class VariableOracle extends MaterialSuggestionOracle {

    Callback callback;
    Request request;

    // The name of the parameter the query will search
    String name;

    public VariableOracle(String name) {
        super();
        ((RestServiceProxy) variableSuggestService).setResource(variableSuggestResource);
        this.name = name;
    }

    public interface VariableSuggestService extends RestService {
        @POST
        public void getVariableSuggestions(SuggestQuery query, MethodCallback<List<LASSuggestion>> suggestions);
    }

    Resource variableSuggestResource = new Resource(Constants.variableSuggestSearch);
    VariableSuggestService variableSuggestService = GWT.create(VariableSuggestService.class);

    public VariableOracle() {
        super();
        ((RestServiceProxy) variableSuggestService).setResource(variableSuggestResource);
    }

    @Override
    public void requestSuggestions(Request request, Callback callback) {
        this.request = request;
        this.callback = callback;
        SuggestQuery sq = new SuggestQuery();
        sq.setQuery(request.getQuery().toLowerCase());
        sq.setName(name);
        variableSuggestService.getVariableSuggestions(sq, processSuggestions);
    }
    MethodCallback<List<LASSuggestion>> processSuggestions = new MethodCallback<List<LASSuggestion>>() {
        @Override
        public void onFailure(Method method, Throwable throwable) {
            Window.alert("Suggest failed");
        }

        @Override
        public void onSuccess(Method method, List<LASSuggestion> lasSuggestions) {
            Response response= new Response();
            response.setSuggestions(lasSuggestions);
            callback.onSuggestionsReady(request, response);
        }
    };
}
