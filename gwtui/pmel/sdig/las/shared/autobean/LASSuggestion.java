package pmel.sdig.las.shared.autobean;

import com.google.gwt.user.client.ui.SuggestOracle;

public class LASSuggestion implements SuggestOracle.Suggestion {
    String suggestion;
    @Override
    public String getDisplayString() {
        return suggestion;
    }

    @Override
    public String getReplacementString() {
        return suggestion;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }
}
