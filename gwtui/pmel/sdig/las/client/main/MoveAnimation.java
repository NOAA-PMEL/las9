package pmel.sdig.las.client.main;

import com.google.gwt.event.shared.GwtEvent;

public class MoveAnimation extends GwtEvent<MoveAnimationHandler> {

    int direction;

    public MoveAnimation(int direction) {
        this.direction = direction;
    }
    public static Type<MoveAnimationHandler> TYPE = new Type<MoveAnimationHandler>();

    public Type<MoveAnimationHandler> getAssociatedType() {
        return TYPE;
    }

    protected void dispatch(MoveAnimationHandler handler) {
        handler.onMoveAnimation(this);
    }

    public int getDirection() {
        return direction;
    }
    public void setDirection(int direction) {
        this.direction = direction;
    }
}
