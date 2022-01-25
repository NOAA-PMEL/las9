package pmel.sdig.las.shared.autobean;

import java.util.List;

public class TimeAxis {

	String calendar;
	String name;
	String title;
	String units;
	// These will always, always be full ISO date/time Strings...
	String start;
	String end;
	// A string of the form PyYmMwWdDThHmMsS (weeks will only appear by itself)
	// The period of the entire time axis
	String period;
	// Same as above, the period between two time steps.
	String delta;
	String position;
	boolean climatology;
	long size;

	String display_hi;
	String display_lo;

	List<NameValuePair> nameValuePairs;

    public String getCalendar() {
        return calendar;
    }

    public void setCalendar(String calendar) {
        this.calendar = calendar;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getDelta() {
        return delta;
    }

    public void setDelta(String delta) {
        this.delta = delta;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public boolean isClimatology() {
        return climatology;
    }

    public void setClimatology(boolean climatology) {
        this.climatology = climatology;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public List<NameValuePair> getNameValuePairs() {
        return nameValuePairs;
    }

    public void setNameValuePairs(List<NameValuePair> nameValuePairs) {
        this.nameValuePairs = nameValuePairs;
    }

    public String getDisplay_hi() {
        return this.display_hi;
    }

    public void setDisplay_hi(String display_hi) {
        this.display_hi = display_hi;
    }

    public String getDisplay_lo() {
        return display_lo;
    }

    public void setDisplay_lo(String display_lo) {
        this.display_lo = display_lo;
    }
}
