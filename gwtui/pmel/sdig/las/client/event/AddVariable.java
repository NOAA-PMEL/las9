package pmel.sdig.las.client.event;

import com.google.gwt.event.shared.GwtEvent;
import pmel.sdig.las.shared.autobean.Variable;

public class AddVariable extends GwtEvent<AddVariableHandler> {

    Variable variable;
    int targetPanel;
    boolean add;

    public AddVariable(Variable variable, int targetPanel, boolean add) {
        this.variable = variable;
        this.targetPanel = targetPanel;
        this.add = add;
    }

    public static Type<AddVariableHandler> TYPE = new Type<AddVariableHandler>();

    public Type<AddVariableHandler> getAssociatedType() {
        return TYPE;
    }

    protected void dispatch(AddVariableHandler handler) {
        handler.onAddVariable(this);
    }

    public Variable getVariable() {
        return variable;
    }

    public void setVariable(Variable variable) {
        this.variable = variable;
    }

    public int getTargetPanel() {
        return targetPanel;
    }

    public void setTargetPanel(int targetPanel) {
        this.targetPanel = targetPanel;
    }

    public boolean isAdd() {
        return add;
    }

    public void setAdd(boolean add) {
        this.add = add;
    }

}
