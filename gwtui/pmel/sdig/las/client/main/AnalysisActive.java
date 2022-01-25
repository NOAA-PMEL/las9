package pmel.sdig.las.client.main;

import com.google.gwt.event.shared.GwtEvent;

public class AnalysisActive extends GwtEvent<AnalysisActiveHandler> {
    public static Type<AnalysisActiveHandler> TYPE = new Type<AnalysisActiveHandler>();

    String type;
    String over;
    boolean active;

    public AnalysisActive(String type, String over, boolean active) {
        this.over = over;
        this.type = type;
        this.active = active;
    }

    public Type<AnalysisActiveHandler> getAssociatedType() {
        return TYPE;
    }

    protected void dispatch(AnalysisActiveHandler handler) {
        handler.onAnalysisActive(this);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOver() {
        return over;
    }

    public void setOver(String over) {
        this.over = over;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
