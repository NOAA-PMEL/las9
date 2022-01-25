package pmel.sdig.las.client.event;

import com.google.gwt.event.shared.GwtEvent;

public class AnimateAction extends GwtEvent<AnimateActionHandler> {

    boolean cancel = false;
    boolean open = false;
    boolean submit = false;

    public AnimateAction(boolean cancel, boolean open, boolean submit) {
        this.cancel = cancel;
        this.open = open;
        this.submit = submit;
    }

    public static Type<AnimateActionHandler> TYPE = new Type<AnimateActionHandler>();

    public Type<AnimateActionHandler> getAssociatedType() {
        return TYPE;
    }

    protected void dispatch(AnimateActionHandler handler) {
        handler.onSetupAnimate(this);
    }

    public boolean isCancel() {
        return cancel;
    }

    public boolean isOpen() {
        return open;
    }

    public boolean isSubmit() {
        return submit;
    }

    public void setCancel(boolean cancel) {
        this.cancel = cancel;
    }
}
