package pmel.sdig.las.client.main;

import com.google.gwt.event.shared.GwtEvent;
import pmel.sdig.las.shared.autobean.DataConstraint;

public class ChangeConstraint extends GwtEvent<ChangeConstraintHandler> {

    String action; // Should be either "add" or "remove"
    String type;
    String lhs;
    String op;
    String rhs;


    public ChangeConstraint(String action, String type, String lhs, String op, String rhs) {
        this.action = action;
        this.type = type;
        this.lhs = lhs;
        this.op = op;
        this.rhs = rhs;
    }
    public static Type<ChangeConstraintHandler> TYPE = new Type<ChangeConstraintHandler>();

    public Type<ChangeConstraintHandler> getAssociatedType() {
        return TYPE;
    }

    protected void dispatch(ChangeConstraintHandler handler) {
        handler.onAddConstraint(this);
    }


    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLhs() {
        return lhs;
    }

    public void setLhs(String lhs) {
        this.lhs = lhs;
    }

    public String getOp() {
        return op;
    }

    public void setOp(String op) {
        this.op = op;
    }

    public String getRhs() {
        return rhs;
    }

    public void setRhs(String rhs) {
        this.rhs = rhs;
    }
}
