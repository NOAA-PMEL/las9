package pmel.sdig.las.client.main;

import com.google.gwt.event.shared.GwtEvent;
import pmel.sdig.las.shared.autobean.Variable;

public class SubsetValues extends GwtEvent<SubsetValuesHandler> {

    Variable variable;

    public static Type<SubsetValuesHandler> TYPE = new Type<SubsetValuesHandler>();

    public SubsetValues(Variable variable) {
        this.variable = variable;
    }

    public Type<SubsetValuesHandler> getAssociatedType() {
        return TYPE;
    }

    protected void dispatch(SubsetValuesHandler handler) {
        handler.onSubsetValues(this);
    }

    public Variable getVariable() {
        return variable;
    }

    public void setVariable(Variable variable) {
        this.variable = variable;
    }
}
