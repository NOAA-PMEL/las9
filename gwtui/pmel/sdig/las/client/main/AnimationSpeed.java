package pmel.sdig.las.client.main;

import com.google.gwt.event.shared.GwtEvent;

public class AnimationSpeed extends GwtEvent<AnimationSpeedHandler> {

    int speed;

    public AnimationSpeed(int speed) {
        this.speed = speed;
    }
    public static Type<AnimationSpeedHandler> TYPE = new Type<AnimationSpeedHandler>();

    public Type<AnimationSpeedHandler> getAssociatedType() {
        return TYPE;
    }

    protected void dispatch(AnimationSpeedHandler handler) {
        handler.onAnimationSpeed(this);
    }

    public int getSpeed() {
        return speed;
    }
    public void setSpeed(int speed) {
        this.speed = speed;
    }
}
