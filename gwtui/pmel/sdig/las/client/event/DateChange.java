package pmel.sdig.las.client.event;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;


public class DateChange extends GwtEvent<DateChange.Handler> {

	public static final Type<DateChange.Handler> TYPE = new Type<DateChange.Handler>();
	
	String hi;
	String lo;

	public DateChange() {
	}
	
	public DateChange(String lo, String hi) {
		this.lo = lo;
		this.hi = hi;
	}
	public interface Handler extends EventHandler {
        public void onDateChange(DateChange event);
	}
	@Override
	public com.google.gwt.event.shared.GwtEvent.Type<Handler> getAssociatedType() {
		return TYPE;
	}
	@Override
	protected void dispatch(Handler handler) {
		handler.onDateChange(this);	
	}
	
	public String getHi() {
		return hi;
	}
	public void setHi(String hi) {
		this.hi = hi;
	}
	public String getLo() {
		return lo;
	}
	public void setLo(String lo) {
		this.lo = lo;
	}
}
