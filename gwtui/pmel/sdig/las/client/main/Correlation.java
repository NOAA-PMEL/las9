package pmel.sdig.las.client.main;

import com.google.gwt.event.shared.GwtEvent;

public class Correlation extends GwtEvent<CorrelationHandler> {

    boolean open;
    boolean setX;
    boolean setY;
    boolean setC;
    boolean removeConstraint;

    public Correlation(boolean open, boolean setx, boolean sety, boolean setc, boolean removeConstraint) {
        this.open = open;
        this.setX = setx;
        this.setY = sety;
        this.setC = setc;
        this.removeConstraint = removeConstraint;
    }

    public static Type<CorrelationHandler> TYPE = new Type<CorrelationHandler>();

    public Type<CorrelationHandler> getAssociatedType() {
        return TYPE;
    }

    protected void dispatch(CorrelationHandler handler) {
        handler.onCorrelation(this);
    }

    public boolean isSetX() {
        return this.setX;
    }

    public boolean isSetY() {
        return setY;
    }

    public boolean isSetC() {
        return setC;
    }

    public boolean isOpen() {
        return this.open;
    }

    public boolean isRemoveConstraint() {
        return removeConstraint;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public void setSetX(boolean setX) {
        this.setX = setX;
    }

    public void setSetY(boolean setY) {
        this.setY = setY;
    }

    public void setSetC(boolean setC) {
        this.setC = setC;
    }

    public void setRemoveConstraint(boolean removeConstraint) {
        this.removeConstraint = removeConstraint;
    }
}
