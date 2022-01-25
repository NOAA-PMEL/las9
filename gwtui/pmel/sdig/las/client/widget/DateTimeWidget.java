package pmel.sdig.las.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.Window;
import gwt.material.design.client.constants.Color;
import gwt.material.design.client.ui.MaterialLabel;
import gwt.material.design.client.ui.MaterialListBox;
import gwt.material.design.client.ui.MaterialPanel;
import gwt.material.design.client.ui.MaterialRow;
import org.gwttime.time.Chronology;
import org.gwttime.time.DateTime;
import org.gwttime.time.DateTimeZone;
import org.gwttime.time.Period;
import org.gwttime.time.chrono.GregorianChronology;
import org.gwttime.time.chrono.JulianChronology;
import org.gwttime.time.format.DateTimeFormat;
import org.gwttime.time.format.DateTimeFormatter;
import org.gwttime.time.format.ISODateTimeFormat;
import org.gwttime.time.format.ISOPeriodFormat;
import org.gwttime.time.format.PeriodFormatter;
import pmel.sdig.las.client.event.DateChange;
import pmel.sdig.las.client.main.ClientFactory;
import pmel.sdig.las.client.map.GeoUtil;
import pmel.sdig.las.client.time.AllLeapChronology;
import pmel.sdig.las.client.time.NoLeapChronology;
import pmel.sdig.las.client.time.ThreeSixtyDayChronology;
import pmel.sdig.las.shared.autobean.NameValuePair;
import pmel.sdig.las.shared.autobean.TimeAxis;

import java.util.List;
import java.util.Locale;


/**
 * A pure GWT implementation of the LAS Date Widget.
 * @author rhs
 *
 */
public class DateTimeWidget extends MaterialPanel {

    private DateTimeFormatter longForm;
    private DateTimeFormatter mediumForm;
    private DateTimeFormatter shortForm;
    private DateTimeFormatter shortFerretForm;
    private DateTimeFormatter mediumFerretForm;
    private DateTimeFormatter longFerretForm;
    private DateTimeFormatter isoForm;

    DateTime lo;
    DateTime hi;
    
    String LABEL = "Date/Time: ";
    String LABEL_RANGE = "Date/Time Start/End: ";

    MaterialListBox lo_year = new MaterialListBox();
    MaterialListBox lo_month = new MaterialListBox();
    MaterialListBox lo_day = new MaterialListBox();
    HourListBox lo_hour = new HourListBox();
    MaterialListBox lo_minute = new MaterialListBox();

    MaterialListBox hi_year = new MaterialListBox();
    MaterialListBox hi_month = new MaterialListBox();
    MaterialListBox hi_day = new MaterialListBox();
    HourListBox hi_hour = new HourListBox();
    MaterialListBox hi_minute = new MaterialListBox();

    MaterialRow labelRow = new MaterialRow();
    MaterialLabel heading = new MaterialLabel();

    MaterialRow minRow = new MaterialRow();
    MaterialRow maxRow = new MaterialRow();


    boolean hasYear = false;
    boolean hasMonth = false;
    boolean hasDay = false;
    boolean hasHour = false;
    boolean hasMinute = false;

    boolean climatology;
    boolean range;

    // period of the entire time axis
    Period period;
    // period between time steps
    Period delta;
    // The string that describes the calendar
    String calendar;


    boolean isMenu = false;

    String render;

    String lo_date;
    String hi_date;

    Chronology chrono;

    DateTimeFormatter monthFormat;

    EventBus eventBus;

    private static final String[] MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
	/**
	 * Construct and empty widget and use the init(@TimeAxis) method to initialized the widget.
	 */
	/**
	 *
	 */
	public DateTimeWidget() {
	    setLineHeight(32);
	    setWidth("340px");
	    setFloat(Style.Float.LEFT);
        labelRow.setGrid("s12");
        minRow.setGrid("s12");
        maxRow.setGrid("s12");
        minRow.setPaddingLeft(0);
        maxRow.setPaddingLeft(0);
        minRow.setPaddingRight(0);
        maxRow.setPaddingRight(0);
        minRow.setMarginBottom(0);
        maxRow.setMarginBottom(0);
//        minRow.setHeight("64px");
//        maxRow.setHeight("64px");

		ClientFactory factory = GWT.create(ClientFactory.class);
		eventBus = factory.getEventBus();
		setListeners();

//        heading.setFontSize("1.1em");
        heading.setFontWeight(Style.FontWeight.BOLD);
        heading.setTextColor(Color.BLUE);
        heading.setMargin(0);
        labelRow.add(heading);

		//TODO set the size and maybe button styles
		lo_year.setGrid("s3");
		lo_month.setGrid("s2");
		lo_day.setGrid("s2");
		lo_hour.setGrid("s3");
        lo_minute.setStyleName("s3");

//        lo_year.setFontSize(.8, Style.Unit.EM);
//        lo_month.setFontSize(.8, Style.Unit.EM);
//        lo_day.setFontSize(.8, Style.Unit.EM);
//        lo_hour.setFontSize(.8, Style.Unit.EM);
//        lo_minute.setFontSize(.8, Style.Unit.EM);

        lo_year.setPadding(0);
        lo_month.setPadding(0);
        lo_day.setPadding(0);
        lo_hour.setPadding(0);
        lo_minute.setPadding(0);

        lo_year.setOld(true);
        lo_month.setOld(true);
        lo_day.setOld(true);
        lo_hour.setOld(true);
        lo_minute.setOld(true);

        hi_year.setGrid("s3");
        hi_month.setGrid("s2");
        hi_day.setGrid("s2");
        hi_hour.setGrid("s3");
        hi_minute.setStyleName("s3");

        hi_year.setOld(true);
        hi_month.setOld(true);
        hi_day.setOld(true);
        hi_hour.setOld(true);
        hi_minute.setOld(true);

        hi_year.setPadding(0);
        hi_month.setPadding(0);
        hi_day.setPadding(0);
        hi_hour.setPadding(0);
        hi_minute.setPadding(0);

	}

    public String getFerretDateMin() {
        if ( climatology ) {
            StringBuilder date = new StringBuilder();
            date.append(GeoUtil.format_two(lo.getDayOfMonth()));
            date.append("-"+monthFormat.print(lo.getMillis()));
            date.append("-0001");
            return date.toString();
        } else if ( isMenu ) {
            return lo_day.getValue(0);
        } else {

            if ( hasHour || hasMinute) {
                return mediumFerretForm.print(lo.getMillis());
            } else {
                return shortFerretForm.print(lo.getMillis());
            }
        }
    }
    public String getFerretDateMax() {
        if ( climatology ) {
            StringBuilder date = new StringBuilder();
            date.append(GeoUtil.format_two(hi.getDayOfMonth()));
            date.append("-"+monthFormat.print(hi.getMillis()));
            date.append("-0001");
            return date.toString();
        } else if ( isMenu ) {
            return hi_day.getValue(hi_day.getItemCount()-1);
        } else  {
            if ( hasHour || hasMinute ) {
                return mediumFerretForm.print(hi.getMillis());
            } else {
                return shortFerretForm.print(hi.getMillis());
            }
        }
    }

    /**
     * Initialize using a TimeAxisSerializable object. Range set to false means there is only one widget (or set of
     * widgets in the case of time) visible and the user can only select one
     * point from that axis. Range set to true means that there are two
     * identical coordinated widgets (or set of widgets in the case of time)
     * from which you can select a starting point and an ending point from that
     * axis. The coordination between the widgets is such that you can not
     * select an endpoint that is before the starting point select. The widgets
     * update themselves to prevent this from happening.
     *
     * @param tAxis
     * @param range
     */
    public void init(TimeAxis tAxis, boolean range) {

        heading.setText(LABEL);
        heading.setTitle(LABEL);

        minRow.clear();
        maxRow.clear();
        hasYear = false;
        hasMonth = false;
        hasDay = false;
        hasHour = false;
        hasMinute = false;
        isMenu = false;
        this.range = range;

        List<NameValuePair> pairs = tAxis.getNameValuePairs();
        if ( pairs != null && pairs.size() > 0) {
            initChrono(tAxis.getCalendar());
            isMenu = true;
            lo_day.clear();
            hi_day.clear();



            for(int i = 0; i < pairs.size(); i++ ) {
                NameValuePair pair = tAxis.getNameValuePairs().get(i);
                lo_day.addItem(pair.getName(), pair.getValue());
                hi_day.addItem(pair.getName(), pair.getValue());
            }
            hasYear = false;
            hasMonth = false;
            hasDay = true;
            hasHour = false;

            lo_date = pairs.get(0).getValue();
            hi_date = pairs.get(pairs.size()-1).getValue();
        } else {
            lo_date = tAxis.getStart();
            hi_date = tAxis.getEnd();
            String p = tAxis.getPeriod();
            String d = tAxis.getDelta();
            PeriodFormatter pf = ISOPeriodFormat.standard();
            period = pf.parsePeriod(p);
            if ( d != null ) {
                delta = pf.parsePeriod(d);
            }
            calendar = tAxis.getCalendar();
            climatology = tAxis.isClimatology();
            if ( climatology ) {
                lo_date = lo_date.replace("0000", "0001");
                hi_date = hi_date.replace("0000", "0001");
            }
            init();
        }

        loadWidget();
    }
    public void init() {

        initChrono(calendar);
        DateTimeFormatter isoParse = ISODateTimeFormat.dateTimeParser();
        lo = isoParse.parseDateTime(lo_date);
        hi = isoParse.parseDateTime(hi_date);
        years(lo, hi, lo_year);
        years(lo, hi, hi_year);
        months(lo_month, lo.getYear());
        months(hi_month, hi.getYear());
        days(lo_day, lo.getYear(), lo.getMonthOfYear());
        days(hi_day, hi.getYear(), hi.getMonthOfYear());
        // If the duration is less than an hour this does not work...
        int dm = delta.getMinutes();
        // The first one is if the year, month and day are all the same.
        if (hi.getYear() == lo.getYear() && hi.getMonthOfYear() == lo.getMonthOfYear() && hi.getDayOfMonth() == lo.getDayOfMonth() ) {
            hours(lo_hour, lo.getHourOfDay(), lo.getMinuteOfHour(), 24, 60);
            hours(hi_hour, 0, 0, hi.getHourOfDay(), hi.getMinuteOfHour());
        } else {
            // Both widgets will be the same going from the low date to the high date since it's all in one day
            hours(lo_hour, lo.getHourOfDay(), lo.getMinuteOfHour(), hi.getHourOfDay(), hi.getMinuteOfHour());
            hours(hi_hour, lo.getHourOfDay(), lo.getMinuteOfHour(), hi.getHourOfDay(), hi.getMinuteOfHour());
        }
        hi_year.setSelectedIndex(hi_year.getItemCount() - 1);
        hi_month.setSelectedIndex(hi_month.getItemCount() - 1);
        hi_day.setSelectedIndex(hi_day.getItemCount() - 1);

        loadWidget();

    }
    public String findValue(List<NameValuePair> pairs, String name) {
        for (int i = 0; i < pairs.size(); i++) {
            NameValuePair pair = pairs.get(i);
            if ( name.equals(pair.getName()) ) {
                return pair.getValue();
            }
        }
        return null;
    }
    public void reinit() {

        if ( isMenu ) {
            lo_day.setSelectedIndex(0);
            hi_day.setSelectedIndex(hi_day.getItemCount() - 1);
        } else {
            setLo(longFerretForm.print(lo.getMillis()));
            setHi(longFerretForm.print(hi.getMillis()));
        }
    }
    public void setListeners() {
        lo_year.addValueChangeHandler(loYearHandler);
        lo_month.addValueChangeHandler(loMonthHandler);
        lo_day.addValueChangeHandler(loDayHandler);
        lo_hour.addValueChangeHandler(loHourHandler);
        lo_minute.addValueChangeHandler(loMinuteHandler);

        hi_year.addValueChangeHandler(hiYearHandler);
        hi_month.addValueChangeHandler(hiMonthHandler);
        hi_day.addValueChangeHandler(hiDayHandler);
        hi_hour.addValueChangeHandler(hiHourHandler);
        hi_minute.addValueChangeHandler(hiMinuteHandler);
    }
    private void initChrono(String calendar) {
        chrono = GregorianChronology.getInstance(DateTimeZone.UTC);

        if ( calendar != null && !calendar.equals("") )	{
            if ( calendar.equalsIgnoreCase("proleptic_gregorian") ) {
                chrono = GregorianChronology.getInstance(DateTimeZone.UTC);
            } else if ( calendar.equalsIgnoreCase("noleap") || calendar.equals("365_day") ) {
                chrono = NoLeapChronology.getInstanceUTC();
            } else if (calendar.equals("julian") ) {
                chrono = JulianChronology.getInstanceUTC();
            } else if ( calendar.equals("all_leap") || calendar.equals("366_day") ) {
                chrono = AllLeapChronology.getInstanceUTC();
            } else if ( calendar.equals("360_day") ) {
                chrono = ThreeSixtyDayChronology.getInstanceUTC();
            }
        }
        monthFormat = DateTimeFormat.forPattern("MMM").withChronology(chrono).withZone(DateTimeZone.UTC);
        longForm = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").withChronology(chrono).withZone(DateTimeZone.UTC);
        mediumForm = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm").withChronology(chrono).withZone(DateTimeZone.UTC);
        shortForm = DateTimeFormat.forPattern("yyyy-MM-dd").withChronology(chrono).withZone(DateTimeZone.UTC);
        shortFerretForm = DateTimeFormat.forPattern("dd-MMM-yyyy").withChronology(chrono).withZone(DateTimeZone.UTC);
        mediumFerretForm = DateTimeFormat.forPattern("dd-MMM-yyyy HH:mm").withChronology(chrono).withZone(DateTimeZone.UTC);
        longFerretForm = DateTimeFormat.forPattern("dd-MMM-yyyy HH:mm:ss").withChronology(chrono).withZone(DateTimeZone.UTC);
        isoForm = ISODateTimeFormat.dateTimeNoMillis().withChronology(chrono).withZone(DateTimeZone.UTC);;
    }
//    public void init(String lo_date, String hi_date, String render,  String calendar, boolean climo) {
//        init(lo_date, hi_date, -1, render, calendar, climo);
//    }
//    public void init(String lo_date, String hi_date, int delta, String render, String calendar, boolean climo) {
//        // We are here because time is required.
//
//        // It may be hours or it may be minutes.
//
//        initChrono(calendar);
//        this.render = render;
//        this.climatology = climo;
//        this.delta = delta;
//        lo = parseDate(lo_date);
//        hi = parseDate(hi_date);
//        years(lo, hi, lo_year);
//        years(lo, hi, hi_year);
//        months(lo_month, lo.getYear());
//        months(hi_month, hi.getYear());
//        days(lo_day, lo.getYear(), lo.getMonthOfYear());
//        days(hi_day, hi.getYear(), hi.getMonthOfYear());
//        // This loads hours either as HH:mm or HH depending on the value of hasMinute
//        hours(lo_hour, lo.getHourOfDay(), lo.getMinuteOfHour(), 24, 0);
//        hours(hi_hour, 0, 0, hi.getHourOfDay(), hi.getMinuteOfHour());
//        if ( hasMinute ) {
//            minutes(lo_minute, lo.getMinuteOfHour(), 59);
//
//        }
//        if ( hasMinute ) {
//            minutes(hi_minute, 0, Math.min(hi.getMinuteOfHour()+1, 59));
//        }
//        hi_year.setSelectedIndex(hi_year.getItemCount() - 1);
//        hi_month.setSelectedIndex(hi_month.getItemCount() - 1);
//        hi_day.setSelectedIndex(hi_day.getItemCount() - 1);
//        loadWidget();
//    }
    private void minutes(MaterialListBox mins, int start_minute, int end_minute) {
        mins.clear();
        for (int min = start_minute; min <= end_minute; min++) {
            mins.addItem(GeoUtil.format_two(min), GeoUtil.format_two(min));
        }

    }
    /**
     *
     * @param hours
     * @param start_hour
     * @param start_minute
     * @param end_hour
     * @param end_minute
     */
//    private void hours(HourListBox hours, int start_hour, int start_minute, int end_hour, int end_minute) {
//        hours.clear();
//        if ( delta < 0 || (start_hour == 0 && end_hour == 0) ) {
//            if ( hasMinute ) {
//                hours.addItem("00", "00");
//            } else {
//                hours.addItem("00:00", "00:00");
//            }
//        } else {
//            int current = start_hour*60 + start_minute;
//            int end = end_hour*60 + end_minute;
//            while ( current < end ) {
//                int hr = (int) Math.floor(current/60);
//                int min = current - hr*60;
//                if ( hasMinute ) {
//                    hours.addItem(hr);
//                } else {
//                    hours.addItem(hr, min);
//                }
//                current = current + delta;
//            }
//        }
//    }
    /**
     *
     * @param hours
     * @param start_hour
     * @param start_minute
     * @param end_hour
     * @param end_minute
     */
    private void hours(HourListBox hours, int start_hour, int start_minute, int end_hour, int end_minute) {
        String date;
        if ( hours.equals(lo_hour) ) {
            date = lo_day.getSelectedValue()+"-"+lo_month.getSelectedValue()+"-"+lo_year.getSelectedValue()+" "+"00:00";
        } else {
            date = hi_day.getSelectedValue()+"-"+hi_month.getSelectedValue()+"-"+hi_year.getSelectedValue()+" "+"00:00";
        }
        DateTime current = mediumFerretForm.parseDateTime(date);;
        current = current.plusHours(start_hour).plusMinutes(start_minute);

        DateTime endOfDay = current.toDateMidnight().plusDays(1).toDateTime();
        DateTime end;
        if ( endOfDay.isBefore(hi)) {
            end = endOfDay;
        } else {
            end = hi;
        }

        hours.clear();

        // Always do at least one, if they are equal that's all you'll get
        int hr = current.getHourOfDay();
        int min = current.getMinuteOfHour();
        hours.addItem(hr, min);
        current = current.plus(delta);

        while ( current.isBefore(end) ) {
            hr = current.getHourOfDay();
            min = current.getMinuteOfHour();
            hours.addItem(hr, min);
            current = current.plus(delta);
        }

    }
    private void loadWidget() {
        clear();
        add(heading);
        if ( isMenu ) {
            heading.setText(LABEL);
            minRow.add(lo_day);
            if ( range ) {
                heading.setText(LABEL_RANGE);
                maxRow.add(hi_day);
            }

        } else {

            if ( (period.getYears() > 0 || delta.getYears() > 0) ) {
                // Climatologies don't have years. :-)
                if ( !climatology ) {
                    minRow.add(lo_year);
                    if (range) {
                        maxRow.add(hi_year);
                    }
                    hasYear = true;
                }
            }

            if ( period.getMonths() > 0 || delta.getMonths() > 0 ) {
                if ( !climatology ) {
                    minRow.add(lo_year);
                    if (range) {
                        maxRow.add(hi_year);
                    }
                    hasYear = true;
                }
                minRow.add(lo_month);
                if ( range ) {
                    maxRow.add(hi_month);
                }
                hasMonth = true;
            }

            if ( delta.getDays() > 0 ) {
                if ( !climatology ) {
                    minRow.add(lo_year);
                    if (range) {
                        maxRow.add(hi_year);
                    }
                    hasYear = true;
                }
                minRow.add(lo_month);
                if ( range ) {
                    maxRow.add(hi_month);
                }
                hasMonth = true;
                minRow.add(lo_day);
                if ( range )
                    maxRow.add(hi_day);
                hasDay = true;
            }

            if ( delta.getHours() > 0 || delta.getMinutes() > 0 ) {
                if ( !climatology ) {
                    minRow.add(lo_year);
                    if (range) {
                        maxRow.add(hi_year);
                    }
                    hasYear = true;
                }
                minRow.add(lo_month);
                if ( range ) {
                    maxRow.add(hi_month);
                }
                hasMonth = true;
                minRow.add(lo_day);
                if ( range ) {
                    maxRow.add(hi_day);
                }
                hasDay = true;
                minRow.add(lo_hour);
                if ( range ) {
                    maxRow.add(hi_hour);
                }
                hasHour = true;
            }

            if ( range ) {
                heading.setText(LABEL_RANGE);
            } else {
                heading.setText(LABEL);
            }

        }

        add(minRow);
        if ( range )
            add(maxRow);
    }


    /**
     * Range set to false means there is only one widget (or set of widgets in
     * the case of time) visible and the user can only select one point from
     * that axis. isRange set to true means that there are two identical
     * coordinated widgets (or set of widgets in the case of time) from which
     * you can select a starting point and an ending point from that axis. The
     * coordination between the widgets is such that you can not select an
     * ending point that is before the selected starting point. The widgets update
     * themselves to prevent this from happening.
     *
     * @param isRange
     */
    public void setRange(boolean isRange) {
        range = isRange;
        loadWidget();
    }


    /**
     * Range set to false means there is only one widget (or set of widgets in
     * the case of time) visible and the user can only select one point from
     * that axis. Range set to true means that there are two identical
     * coordinated widgets (or set of widgets in the case of time) from which
     * you can select a starting point and an ending point from that axis. The
     * coordination between the widgets is such that you can not select an
     * endpoint that is before the starting point select. The widgets update
     * themselves to prevent this from happening.
     *
     * @return
     */
    public boolean isRange() {
        return range;
    }
    private void days(MaterialListBox day, int year, int month) {
        day.clear();
        int lo_year = lo.getYear();
        int hi_year = hi.getYear();

        int lo_month = lo.getMonthOfYear();
        int hi_month = hi.getMonthOfYear();

        int start = 1;
        int end = maxDays(year, month);

        if ( lo_year == year && lo_month == month ) {
            start = lo.getDayOfMonth();
            end = maxDays(year, month);

        }
        // If it starts and ends in the same month replace with the day of the high month.
        if ( hi_year == year && hi_month == month ) {
            end = hi.getDayOfMonth();
        }
        for ( int i = start; i <= end; i++) {
            day.addItem(GeoUtil.format_two(i), GeoUtil.format_two(i));
        }
    }

    private int maxDays(int year, int month) {
        DateTime dt = new DateTime(year, month, 1, 0, 0, 0, chrono);
        return dt.dayOfMonth().getMaximumValue();
    }
    private void months(MaterialListBox month, int year) {
        month.clear();
        int lo_year = lo.getYear();
        int hi_year = hi.getYear();

        int start = 1;
        int end = 12;

        if  ( lo_year == year ) {
            start = lo.getMonthOfYear();
        }
        if ( hi_year == year ) {
            end = hi.getMonthOfYear();
        }

//		DateTime startDate = new DateTime(year, start, 1, 0, 0, DateTimeZone.UTC).withChronology(chrono);
//		DateTime endDate = new DateTime(year, end, 1, 0, 0, DateTimeZone.UTC).withChronology(chrono);

        DateTime startDate = lo.withYear(year).withMonthOfYear(start).withDayOfMonth(1).withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0);
        DateTime endDate = hi.withYear(year).withMonthOfYear(end).withDayOfMonth(1).withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0);

        int stride = delta.getMonths();
        if ( stride <= 0 ) {
            stride = 1;
        }

        while (startDate.isBefore(endDate.getMillis()) || startDate.equals(endDate)) {
            month.addItem(monthFormat.print(startDate.getMillis()));
            startDate = startDate.plusMonths(stride);
        }
    }

    private void years(DateTime lo, DateTime hi, MaterialListBox year) {
        year.clear();
        int start = lo.getYear();
        int end = hi.getYear();
        for ( int y = start; y <= end; y++ ) {
            year.addItem(GeoUtil.format_four(y), GeoUtil.format_four(y));
        }
    }
    public void setEnabled(boolean b) {
        lo_year.setEnabled(b);
        lo_month.setEnabled(b);
        lo_day.setEnabled(b);
        lo_hour.setEnabled(b);

        hi_year.setEnabled(b);
        hi_month.setEnabled(b);
        hi_day.setEnabled(b);
        hi_hour.setEnabled(b);
    }

    public String getISODateLo() {
        String lo = getFerretDateLo();
        DateTime dtlo;
        try {
            dtlo = shortFerretForm.parseDateTime(lo);
        } catch (Exception e) {
            try {
                dtlo = mediumFerretForm.parseDateTime(lo);
            } catch ( Exception e2 ) {
                try {
                    dtlo = longFerretForm.parseDateTime(lo);
                } catch (Exception e3) {
                    // punt
                    return "";
                }
            }
        }
        try {
            return isoForm.print(dtlo.getMillis());
        } catch (Exception e) {
            return "";
        }
    }
    public String getISODateHi() {
        String hi = getFerretDateHi();
        DateTime dthi;
        try {
            dthi = shortFerretForm.parseDateTime(hi);
        } catch (Exception e) {
            try {
                dthi = mediumFerretForm.parseDateTime(hi);
            } catch ( Exception e2 ) {
                try {
                    dthi = longFerretForm.parseDateTime(hi);
                } catch (Exception e3) {
                    // punt
                    return "";
                }
            }
        }
        try {
            return isoForm.print(dthi.getMillis());
        } catch (Exception e) {
            return "";
        }
    }
    public String getFerretDateLo() {
        StringBuffer date = new StringBuffer();
        if ( isMenu ) {
            return lo_day.getValue(lo_day.getSelectedIndex());
        } else {
            if ( hasDay ) {
                date.append(lo_day.getValue(lo_day.getSelectedIndex()));
            } else {
                date.append(GeoUtil.format_two(lo.getDayOfMonth()));
            }
            if ( hasMonth ) {
                date.append("-"+lo_month.getValue(lo_month.getSelectedIndex()));
            } else {
                date.append("-"+monthFormat.print(lo.getMillis()));
            }

            if ( climatology ) {
                date.append("-0001");
            } else {

                if ( hasYear ) {
                    date.append("-"+lo_year.getValue(lo_year.getSelectedIndex()));
                }
            }
            if ( hasHour ) {
                if ( !hasMinute ) {
                    date.append(" "+lo_hour.getValue(lo_hour.getSelectedIndex()));
                } else {
                    date.append(" "+lo_hour.getValue(lo_hour.getSelectedIndex())+":"+lo_minute.getValue(lo_minute.getSelectedIndex()));
                }
            } else {
                date.append(" 00:00");
            }
            return date.toString();
        }
    }

    public String getFerretDateHi() {
        if ( range ) {
            if ( isMenu ) {
                return hi_day.getValue(hi_day.getSelectedIndex());
            } else {
                StringBuffer date = new StringBuffer();
                if ( hasDay ) {
                    date.append(hi_day.getValue(hi_day.getSelectedIndex()));
                } else {
                    date.append(GeoUtil.format_two(lo.getDayOfMonth()));
                }
                if ( hasMonth ) {
                    date.append("-"+hi_month.getValue(hi_month.getSelectedIndex()));
                } else {
                    date.append("-"+monthFormat.print(lo.getMillis()));
                }

                if ( climatology ) {
                    date.append("-0001");
                } else {

                    if ( hasYear ) {
                        date.append("-"+hi_year.getValue(hi_year.getSelectedIndex()));
                    }
                }
                if ( hasHour ) {
                    if ( !hasMinute ) {
                        date.append(" "+hi_hour.getValue(hi_hour.getSelectedIndex()));
                    } else {
                        date.append(" "+hi_hour.getValue(hi_hour.getSelectedIndex())+":"+hi_minute.getValue(hi_minute.getSelectedIndex()));
                    }
                } else {
                    date.append(" 00:00");
                }
                return date.toString();
            }
        } else {
            return getFerretDateLo();
        }
    }
    public void setLo(String tlo) {

        if ( isMenu ) {
            for(int d = 0; d < lo_day.getItemCount(); d++) {
                String value = lo_day.getValue(d).toLowerCase(Locale.ENGLISH);
                String label = lo_day.getItemText(d).toLowerCase(Locale.ENGLISH);
                if ( tlo.toLowerCase(Locale.ENGLISH).contains(value) || tlo.toLowerCase(Locale.ENGLISH).contains(label) ) {
                    lo_day.setSelectedIndex(d);
                    loDayChange();
                }
            }
            int lo_i = lo_day.getSelectedIndex();
            int hi_i = hi_day.getSelectedIndex();
            if ( lo_i > hi_i ) {
                hi_day.setSelectedIndex(lo_i);
            }
        } else {
            DateTime lo = null;
            try {
                lo = parseFerretDate(tlo);
            } catch (Exception e ) {
                // try something else
            }
            if ( lo == null ) {
                lo = parseDate(tlo);
            }

            if ( hasYear ) {
                String year = String.valueOf(lo.getYear());
                int yearIndex = lo_year.getSelectedIndex();
                for ( int y = 0; y < lo_year.getItemCount(); y++ ) {
                    String value = lo_year.getValue(y);
                    if ( value.equals(year) ) {
                        lo_year.setSelectedIndex(y);
                        if ( y != yearIndex ) {
                            loYearChange();
                        }
                    }
                }
            }
            if ( hasMonth ) {
                String month = monthFormat.print(lo.getMillis());
                int monthIndex = lo_month.getSelectedIndex();
                for ( int m = 0; m < lo_month.getItemCount(); m++ ) {
                    String value = lo_month.getValue(m);
                    if ( value.equals(month) ) {
                        lo_month.setSelectedIndex(m);
                        if ( m != monthIndex ) {
                            loMonthChange();
                        }
                    }
                }

            }
            if ( hasDay ) {
                String day = GeoUtil.format_two(lo.getDayOfMonth());
                int dayIndex = lo_day.getSelectedIndex();
                for(int d = 0; d < lo_day.getItemCount(); d++) {
                    String value = lo_day.getValue(d);
                    if ( value.equals(day) ) {
                        lo_day.setSelectedIndex(d);
                        if ( dayIndex != d ) {
                            loDayChange();
                        }
                    }
                }
            }
            if ( hasHour ) {
                int hour = lo.getHourOfDay();
                int min = lo.getMinuteOfHour();
                if ( hasMinute ) {
                    int hourIndex = lo_hour.getSelectedIndex();
                    int minuteIndex = lo_minute.getSelectedIndex();
                    String hours_value = GeoUtil.format_two(hour);
                    for( int h = 0; h < lo_hour.getItemCount(); h++ ) {
                        String value = lo_hour.getValue(h);
                        if ( hours_value.equals(value) ) {
                            lo_hour.setSelectedIndex(h);
                            if ( hourIndex != h ) {
                                loHourChange();
                            }
                        }
                    }
                    String minute_value = GeoUtil.format_two(min);
                    for( int h = 0; h < lo_minute.getItemCount(); h++ ) {
                        String value = lo_minute.getValue(h);
                        if ( minute_value.equals(value) ) {
                            lo_minute.setSelectedIndex(h);
                        }
                    }

                } else {

                    for( int h = 0; h < lo_hour.getItemCount(); h++ ) {
                        int menu_hour = lo_hour.getHour(h);
                        int menu_minute = lo_hour.getMinute(h);
                        if ( hour >= menu_hour && hour < menu_hour + 1 ) {
                            if ( menu_minute >= min && menu_minute < min + delta.getMinutes()) {
                                lo_hour.setSelectedIndex(h);
                            }
                        }
                    }
                }
            }


             // The new value is set.  Check the range (even it it's not visible).
            if ( hasYear ) {
                checkRangeEndYear();
            } else if ( hasMonth ) {
                checkRangeEndMonth();
            } else if ( hasDay ) {
                checkRangeEndDay();
            } else if (hasHour) {
                checkRangeEndHour();
            } else {
                checkRangeEndMinute();
            }
        }
        String l = getISODateLo();
    }
    public void setHi(String thi) {


        if ( isMenu ) {
            for(int d = 0; d < hi_day.getItemCount(); d++) {
                String value = hi_day.getValue(d).toLowerCase(Locale.ENGLISH);
                String label = hi_day.getItemText(d).toLowerCase(Locale.ENGLISH);
                if ( thi.toLowerCase(Locale.ENGLISH).contains(value) || thi.toLowerCase(Locale.ENGLISH).contains(label) ) {
                    hi_day.setSelectedIndex(d);
                }
            }
            int lo_i = lo_day.getSelectedIndex();
            int hi_i = hi_day.getSelectedIndex();
            if ( lo_i > hi_i ) {
                lo_day.setSelectedIndex(hi_i);
            }
        } else {
            DateTime hi = null;
            try {
                hi = parseFerretDate(thi);
            } catch (Exception e ) {
                // try something else
            }
            if ( hi == null ) {
                hi = parseDate(thi);
            }

            if ( hasYear ) {
                String year = String.valueOf(hi.getYear());
                int yearIndex = hi_year.getSelectedIndex();
                for ( int y = 0; y < hi_year.getItemCount(); y++ ) {
                    String value = hi_year.getValue(y);
                    if ( value.equals(year) ) {
                        hi_year.setSelectedIndex(y);
                        if ( y != yearIndex ) {
                            hiYearChange();
                        }
                    }
                }
            }
            if ( hasMonth ) {
                String month = monthFormat.print(hi.getMillis());
                int monthIndex = hi_month.getSelectedIndex();
                for ( int m = 0; m < hi_month.getItemCount(); m++ ) {
                    String value = hi_month.getValue(m);
                    if ( value.equals(month) ) {
                        hi_month.setSelectedIndex(m);
                        if ( m != monthIndex ) {
                            hiMonthChange();
                        }
                    }
                }

            }
            if ( hasDay ) {
                String day = GeoUtil.format_two(hi.getDayOfMonth());
                int dayIndex = hi_day.getSelectedIndex();
                for(int d = 0; d < hi_day.getItemCount(); d++) {
                    String value = hi_day.getValue(d);
                    if ( value.equals(day) ) {
                        hi_day.setSelectedIndex(d);
                        if ( dayIndex != d ) {
                            hiDayChange();
                        }
                    }
                }
            }
            if ( hasHour ) {
                int hour = hi.getHourOfDay();
                int min = hi.getMinuteOfHour();
                if ( hasMinute ) {
                    String hours_value = GeoUtil.format_two(hour);
                    int hoursIndex = hi_hour.getSelectedIndex();
                    int minuteIndex = hi_minute.getSelectedIndex();
                    for( int h = 0; h < hi_hour.getItemCount(); h++ ) {
                        String value = hi_hour.getValue(h);
                        if ( hours_value.equals(value) ) {
                            hi_hour.setSelectedIndex(h);
                            if ( hoursIndex != h ) {
                                hiHourChange();
                            }
                        }
                    }
                    String minute_value = GeoUtil.format_two(min);
                    for( int h = 0; h < hi_minute.getItemCount(); h++ ) {
                        String value = hi_minute.getValue(h);
                        if ( minute_value.equals(value) ) {
                            hi_minute.setSelectedIndex(h);
                        }
                    }

                } else {
                    for( int h = 0; h < hi_hour.getItemCount(); h++ ) {
                        int menu_hour = hi_hour.getHour(h);
                        int menu_minute = hi_hour.getMinute(h);
                        if ( hour >= menu_hour && hour < menu_hour + 1 ) {
                            if ( menu_minute >= min && menu_minute < min + delta.getMinutes()) {
                                hi_hour.setSelectedIndex(h);
                            }
                        }
                    }
                }
            }
            // The new value is set.  Check the range (even it it's not visible).
            if ( hasYear ) {
                checkRangeStartYear();
            } else if ( hasMonth ) {
                checkRangeStartMonth();
            } else if ( hasDay ) {
                checkRangeStartDay();
            } else if ( hasHour ) {
                checkRangeStartHour();
            } else {
                checkRangeStartMinute();
            }
        }
        String h = getISODateHi();
    }

    private void loadAndSetMonthDayHour(MaterialListBox month_list, MaterialListBox day_list, HourListBox hour_list, MaterialListBox minute_list, int year, int month, int day, int hour, int min) {
        // Load the valid months for this year.
        months(month_list, year);

        int low_month_number = monthToInt(month_list.getValue(0));
        int hi_month_number = monthToInt(month_list.getValue(month_list.getItemCount() - 1));

        DateTime requested_instance = new DateTime(year, month, day, hour, min, chrono).withZone(DateTimeZone.UTC);
        String month_name = monthFormat.print(requested_instance.getMillis());

        if ( month < low_month_number ) {
            // If the current month is before the first month in the list
            // set to the first month.
            month_list.setSelectedIndex(0);
        } else if ( month > hi_month_number) {
            // If the current month is after the last month in the list
            // set to the last month.
            month_list.setSelectedIndex(month_list.getItemCount() - 1);
        } else {
            // Else set to that month
            for (int i = 0; i < month_list.getItemCount(); i++) {
                String v = month_list.getValue(i);
                if ( v.equals(month_name) ) {
                    month_list.setSelectedIndex(i);
                }
            }
        }


        int selected_month = monthToInt(month_list.getValue(month_list.getSelectedIndex()));
        loadAndSetDayHour(day_list, hour_list, minute_list, year, selected_month, day, hour, min);
    }

    public void loadAndSetDayHour(MaterialListBox day_list, HourListBox hour_list, MaterialListBox minute_list, int year, int month, int day, int hour, int min) {
        // Load the valid days for this month (which as set above) and year.
        days(day_list, year, month);

        if ( day < Integer.valueOf(day_list.getValue(0)).intValue() ) {
            day_list.setSelectedIndex(0);
        } else if ( day > Integer.valueOf(day_list.getValue(day_list.getItemCount() - 1)).intValue() ) {
            day_list.setSelectedIndex(day_list.getItemCount() - 1);
        } else {

            for (int i = 0; i < day_list.getItemCount(); i++) {
                String v = day_list.getValue(i);
                if ( v.equals(GeoUtil.format_two(day)) ) {
                    day_list.setSelectedIndex(i);
                }
            }
        }

        loadAndSetHour(hour_list, minute_list, year, month, Integer.valueOf(day_list.getValue(day_list.getSelectedIndex())).intValue(), hour, min);

    }
    public void loadAndSetHour(HourListBox hour_list, MaterialListBox minute_list, int year, int month, int day, int hour, int min) {
        int start_year = lo.getYear();
        int start_month = lo.getMonthOfYear();
        int start_day = lo.getDayOfMonth();
        int start_hour = lo.getHourOfDay();
        int start_min = lo.getMinuteOfHour();

        int end_year = hi.getYear();
        int end_month = hi.getMonthOfYear();
        int end_day = hi.getDayOfMonth();
        int end_hour = hi.getHourOfDay();
        int end_min = hi.getMinuteOfHour();
        if ( start_year == year && start_month == month && start_day == day ) {
            hours(hour_list, start_hour, start_min, 24, 0);
        } else if ( end_year == year && end_month == month && end_day == day ) {
            hours(hour_list, 0, 0, end_hour, end_min);
        } else {
            hours(hour_list, 0, 0, 24, 0);
        }
        if ( hasMinute ) {
            loadAndSetMinute(minute_list, year, month, day, hour, min);
        }
    }
    public void loadAndSetMinute(MaterialListBox minute_list, int year, int month, int day, int hour, int min) {
        int start_year = lo.getYear();
        int start_month = lo.getMonthOfYear();
        int start_day = lo.getDayOfMonth();
        int start_hour = lo.getHourOfDay();
        int start_min = lo.getMinuteOfHour();

        int end_year = hi.getYear();
        int end_month = hi.getMonthOfYear();
        int end_day = hi.getDayOfMonth();
        int end_hour = hi.getHourOfDay();
        int end_min = hi.getMinuteOfHour();
        if ( start_year == year && start_month == month && start_day == day && start_hour == hour ) {
            minutes(minute_list, start_min, 59);
        } else if (  end_year == year && end_month == month && end_day == day && end_hour == hour ) {
            minutes(minute_list, 0, end_hour);
        } else {
            minutes(minute_list, 0, 59);
        }

    }
    private void checkRangeEndYear() {
        if ( isMenu ) {
            // This is the start of the cascade to check the end date, but since it's a menu
            // it's the only one you have to check and the values are stored in the "day" widget.
            if ( hi_day.getSelectedIndex() < lo_day.getSelectedIndex() ) {
                hi_day.setSelectedIndex(lo_day.getSelectedIndex());
            }
        } else {
            String current_lo = getFerretDateLo();
            String current_hi = getFerretDateHi();

            DateTime clo = parseFerretDate(current_lo);
            DateTime chi = parseFerretDate(current_hi);

            // Set the hi year to the lo year and check the month...
            if ( clo.isAfter(chi) ) {
                int year = Integer.valueOf(lo_year.getValue(lo_year.getSelectedIndex()));
                hi_year.setSelectedIndex(lo_year.getSelectedIndex());

                if ( hasMinute ) {
                    loadAndSetMonthDayHour(hi_month, hi_day, hi_hour, lo_minute, year, monthToInt(hi_month.getValue(hi_month.getSelectedIndex())), Integer.valueOf(hi_day.getValue(hi_day.getSelectedIndex())).intValue(), hi_hour.getHour(), Integer.valueOf(hi_minute.getSelectedValue()).intValue());
                } else {
                    loadAndSetMonthDayHour(hi_month, hi_day, hi_hour, hi_minute, year, monthToInt(hi_month.getValue(hi_month.getSelectedIndex())), Integer.valueOf(hi_day.getValue(hi_day.getSelectedIndex())).intValue(), hi_hour.getHour(), hi_hour.getMin());
                }
                checkRangeEndMonth();
            }
        }
    }
    private void checkRangeEndMonth() {
        String current_lo = getFerretDateLo();
        String current_hi = getFerretDateHi();

        DateTime clo = parseFerretDate(current_lo);
        DateTime chi = parseFerretDate(current_hi);

        if ( clo.isAfter(chi) ) {
            int ny = Integer.valueOf(hi_year.getValue(hi_year.getSelectedIndex()));
            int month = monthToInt(lo_month.getValue(lo_month.getSelectedIndex()));
            int day = Integer.valueOf(hi_day.getValue(hi_day.getSelectedIndex()));
            hi_month.setSelectedIndex(lo_month.getSelectedIndex());
            int hour = hi_hour.getHour();
            int min;
            if ( hasMinute ) {
                min = intMinute(hi_minute.getSelectedValue());
            } else {
                min = hi_hour.getMin();
            }
            loadAndSetDayHour(hi_day, hi_hour, lo_minute, ny, month, day, hour, min);
            checkRangeEndDay();
        }
    }
    private void checkRangeEndDay() {
        String current_lo = getFerretDateLo();
        String current_hi = getFerretDateHi();

        DateTime clo = parseFerretDate(current_lo);
        DateTime chi = parseFerretDate(current_hi);

        if ( clo.isAfter(chi) ) {
            hi_day.setSelectedIndex(lo_day.getSelectedIndex());
            checkRangeEndHour();
        }
    }
    private void checkRangeEndHour() {
        String current_lo = getFerretDateLo();
        String current_hi = getFerretDateHi();

        DateTime clo = parseFerretDate(current_lo);
        DateTime chi = parseFerretDate(current_hi);

        if ( clo.isAfter(chi) ) {
            hi_hour.setSelectedIndex(lo_hour.getSelectedIndex());
        }
        if ( hasMinute ) {
            checkRangeEndMinute();
        }
    }
    private void checkRangeStartYear() {
        String current_lo = getFerretDateLo();
        String current_hi = getFerretDateHi();

        DateTime clo = parseFerretDate(current_lo);
        DateTime chi = parseFerretDate(current_hi);

        // Set the hi year to the lo year and check the month...
        if ( clo.isAfter(chi) ) {
            lo_year.setSelectedIndex(hi_year.getSelectedIndex());
            checkRangeStartMonth();
        }

    }
    private void checkRangeStartMonth() {
        String current_lo = getFerretDateLo();
        String current_hi = getFerretDateHi();

        DateTime clo = parseFerretDate(current_lo);
        DateTime chi = parseFerretDate(current_hi);

        if ( clo.isAfter(chi) ) {
            lo_month.setSelectedIndex(hi_month.getSelectedIndex());
            checkRangeStartDay();
        }
    }
    private void checkRangeStartDay() {
        String current_lo = getFerretDateLo();
        String current_hi = getFerretDateHi();

        DateTime clo = parseFerretDate(current_lo);
        DateTime chi = parseFerretDate(current_hi);

        if ( clo.isAfter(chi) ) {
            lo_day.setSelectedIndex(hi_day.getSelectedIndex());
            checkRangeStartHour();
        }
    }
    public void checkRangeStartHour() {
        String current_lo = getFerretDateLo();
        String current_hi = getFerretDateHi();

        DateTime clo = parseFerretDate(current_lo);
        DateTime chi = parseFerretDate(current_hi);

        if ( clo.isAfter(chi) ) {
            lo_hour.setSelectedIndex(hi_hour.getSelectedIndex());
        }
        if ( hasMinute ) {
            checkRangeStartMinute();
        }
    }
    public void checkRangeStartMinute() {
        String current_lo = getFerretDateLo();
        String current_hi = getFerretDateHi();

        DateTime clo = parseFerretDate(current_lo);
        DateTime chi = parseFerretDate(current_hi);

        if ( clo.isAfter(chi) ) {
            lo_minute.setSelectedIndex(hi_minute.getSelectedIndex());
        }
    }
    public ValueChangeHandler loYearHandler = new ValueChangeHandler() {
        @Override
        public void onValueChange(ValueChangeEvent event) {
            loYearChange();
            eventBus.fireEventFromSource(new DateChange(), DateTimeWidget.this);
        }
    };
    private void loYearChange() {
        int year = Integer.valueOf(lo_year.getValue(lo_year.getSelectedIndex())).intValue();
        int month = monthToInt(lo_month.getValue(lo_month.getSelectedIndex()));
        int day = Integer.valueOf(lo_day.getValue(lo_day.getSelectedIndex())).intValue();
        int hour = lo_hour.getHour();
        int min;
        if ( hasMinute ) {
            min = intMinute(lo_minute.getSelectedValue());
        } else {
            min = lo_hour.getMin();
        }
        loadAndSetMonthDayHour(lo_month, lo_day, lo_hour, lo_minute, year, month, day, hour, min);
        checkRangeEndYear();
    }
    public ValueChangeHandler loMonthHandler = new ValueChangeHandler() {
        @Override
        public void onValueChange(ValueChangeEvent event) {
            loMonthChange();
            eventBus.fireEventFromSource(new DateChange(), DateTimeWidget.this);
        }
    };
    private void loMonthChange() {
        int year = Integer.valueOf(lo_year.getValue(lo_year.getSelectedIndex())).intValue();
        int month = monthToInt(lo_month.getValue(lo_month.getSelectedIndex()));
        int day = Integer.valueOf(lo_day.getValue(lo_day.getSelectedIndex())).intValue();
        int hour = lo_hour.getHour();
        int min;
        if ( hasMinute ) {
            min = intMinute(lo_minute.getSelectedValue());
        } else {
            min = lo_hour.getMin();
        }
        loadAndSetDayHour(lo_day, lo_hour, lo_minute, year, month, day, hour, min);
        checkRangeEndMonth();
    }
    // The loDayListener and hiDayListener are the only ones that should fire when the Widget contains a menu.
    public ValueChangeHandler loDayHandler = new ValueChangeHandler() {
        @Override
        public void onValueChange(ValueChangeEvent event) {
            loDayChange();
            eventBus.fireEventFromSource(new DateChange(), DateTimeWidget.this);
        }
    };
    private void loDayChange() {
        if ( isMenu ) {
            int lo_i = lo_day.getSelectedIndex();
            int hi_i = hi_day.getSelectedIndex();
            if ( lo_i > hi_i ) {
                hi_day.setSelectedIndex(lo_i);
            }
        } else {
            int year = Integer.valueOf(lo_year.getValue(lo_year.getSelectedIndex())).intValue();
            int month = monthToInt(lo_month.getValue(lo_month.getSelectedIndex()));
            int day = Integer.valueOf(lo_day.getValue(lo_day.getSelectedIndex())).intValue();
            int hour = lo_hour.getHour();
            int min;
            if ( hasMinute ) {
                min = intMinute(lo_minute.getSelectedValue());
            } else {
                min = lo_hour.getMin();
            }

            loadAndSetHour(lo_hour, lo_minute, year, month, day, hour, min);
            checkRangeEndDay();
        }
    }
    private void loHourChange() {
        int year = Integer.valueOf(lo_year.getValue(lo_year.getSelectedIndex())).intValue();
        int month = monthToInt(lo_month.getValue(lo_month.getSelectedIndex()));
        int day = Integer.valueOf(lo_day.getValue(lo_day.getSelectedIndex())).intValue();
        int hour = lo_hour.getHour();
        int min;
        if ( hasMinute ) {
            min = intMinute(lo_minute.getSelectedValue());
        } else {
            min = lo_hour.getMin();
        }
        loadAndSetMinute(lo_minute, year, month, day, hour, min);
        checkRangeEndHour();
    }
    public ValueChangeHandler loHourHandler = new ValueChangeHandler() {
        @Override
        public void onValueChange(ValueChangeEvent event) {
            loHourChange();
            eventBus.fireEventFromSource(new DateChange(), DateTimeWidget.this);
        }
    };
    public ValueChangeHandler loMinuteHandler = new ValueChangeHandler() {
        @Override
        public void onValueChange(ValueChangeEvent event) {
            checkRangeEndMinute();
            eventBus.fireEventFromSource(new DateChange(), DateTimeWidget.this);
        }

    };
    public ValueChangeHandler hiYearHandler = new ValueChangeHandler() {
        @Override
        public void onValueChange(ValueChangeEvent event) {
            hiYearChange();
            eventBus.fireEventFromSource(new DateChange(), DateTimeWidget.this);
        }

    };
    private void hiYearChange() {
        int year = Integer.valueOf(hi_year.getValue(hi_year.getSelectedIndex())).intValue();
        int month = monthToInt(hi_month.getValue(hi_month.getSelectedIndex()));
        int day = Integer.valueOf(hi_day.getValue(hi_day.getSelectedIndex())).intValue();
        int hour = hi_hour.getHour();
        int	min;
        if ( hasMinute ) {
            min = intMinute(hi_minute.getSelectedValue());
        } else {
            min = hi_hour.getMin();
        }
        loadAndSetMonthDayHour(hi_month, hi_day, hi_hour, hi_minute, year, month, day, hour, min);
        checkRangeStartYear();
    }
    protected void checkRangeEndMinute() {

        String current_lo = getFerretDateLo();
        String current_hi = getFerretDateHi();

        DateTime clo = parseFerretDate(current_lo);
        DateTime chi = parseFerretDate(current_hi);

        if ( clo.isAfter(chi) ) {
            hi_minute.setSelectedIndex(lo_minute.getSelectedIndex());
        }

    }

    public ValueChangeHandler hiMonthHandler = new ValueChangeHandler() {
        @Override
        public void onValueChange(ValueChangeEvent event) {
            hiMonthChange();
            eventBus.fireEventFromSource(new DateChange(), DateTimeWidget.this);
        }
    };
    private void hiMonthChange() {
        int year = Integer.valueOf(hi_year.getValue(hi_year.getSelectedIndex())).intValue();
        int month = monthToInt(hi_month.getValue(hi_month.getSelectedIndex()));
        int day = Integer.valueOf(hi_day.getValue(hi_day.getSelectedIndex())).intValue();
        int hour = hi_hour.getHour();
        int	min;
        if ( hasMinute ) {
            min = intMinute(hi_minute.getSelectedValue());
        } else {
            min = hi_hour.getMin();
        }
        loadAndSetDayHour(hi_day, hi_hour, hi_minute, year, month, day, hour, min);
        checkRangeStartMonth();
    }
    public ValueChangeHandler hiDayHandler = new ValueChangeHandler() {
        @Override
        public void onValueChange(ValueChangeEvent event) {
            hiDayChange();
            eventBus.fireEventFromSource(new DateChange(), DateTimeWidget.this);
        }
    };



    private void hiHourChange() {
        int year = Integer.valueOf(hi_year.getValue(hi_year.getSelectedIndex())).intValue();
        int month = monthToInt(hi_month.getValue(hi_month.getSelectedIndex()));
        int day = Integer.valueOf(hi_day.getValue(hi_day.getSelectedIndex())).intValue();
        int hour = hi_hour.getHour();
        int min;
        if ( hasMinute ) {
            min = intMinute(hi_minute.getSelectedValue());
        } else {
            min = hi_hour.getMin();
        }
        loadAndSetMinute(hi_minute, year, month, day, hour, min);
        checkRangeEndHour();
    }
    public ValueChangeHandler hiHourHandler = new ValueChangeHandler() {
        @Override
        public void onValueChange(ValueChangeEvent event) {
            hiHourChange();
            eventBus.fireEventFromSource(new DateChange(), DateTimeWidget.this);
        }
    };




    public void setLoByDouble(double inlo, String time_origin, String unitsString, String calendar) {

        String flo = formatDate(inlo, time_origin, unitsString, calendar);

        setLo(flo);

    }
    public void setHiByDouble(double inhi, String time_origin, String unitsString, String calendar) {

        String fhi = formatDate(inhi, time_origin, unitsString, calendar);

        setHi(fhi);

    }

    public static String formatDate(double in, String time_origin, String unitsString, String calendar) {
        Chronology chrono = GregorianChronology.getInstance(DateTimeZone.UTC);

        if ( calendar != null && !calendar.equals("") ) {
            if ( calendar.equalsIgnoreCase("proleptic_gregorian") ) {
                chrono = GregorianChronology.getInstance(DateTimeZone.UTC);
            } else if ( calendar.equalsIgnoreCase("noleap") || calendar.equals("365_day") ) {
                chrono = NoLeapChronology.getInstanceUTC();
            } else if (calendar.equals("julian") ) {
                chrono = JulianChronology.getInstanceUTC();
            } else if ( calendar.equals("all_leap") || calendar.equals("366_day") ) {
                chrono = AllLeapChronology.getInstanceUTC();
            } else if ( calendar.equals("360_day") ) {
                chrono = ThreeSixtyDayChronology.getInstanceUTC();
            }
        }

        boolean zeroOrigin;
        if (time_origin.indexOf("0000") >= 0) {
            time_origin.replaceFirst("0000", "0001");
            zeroOrigin = true;
        }
        DateTimeFormatter myLongFerretForm = DateTimeFormat.forPattern("dd-MMM-yyyy HH:mm:ss").withChronology(chrono).withZone(DateTimeZone.UTC);
        DateTime baseDT = myLongFerretForm.parseDateTime(time_origin).withChronology(chrono).withZone(DateTimeZone.UTC);

        int insec = 0;

        Period p = new Period(0, 0, 0, 0, 0, 0, 0, 0);

        double f = Math.floor(in);
        double frac = in - f;

        // years, months, days, hours, minutes, seconds
        if ( unitsString.toLowerCase(Locale.ENGLISH).contains("year") ) {

            int years = (int) f;

            int days = (int)(frac*365.25);

            p = new Period(years, 0, 0, days, 0, 0, 0, 0);

        } else if ( unitsString.toLowerCase(Locale.ENGLISH).contains("month") ) {

            int months = (int) f;
            int days = (int) (30.5*frac);

            p = new Period(0, months, 0, days, 0, 0, 0, 0);

        } else if (  unitsString.toLowerCase(Locale.ENGLISH).contains("week")  ) {

            int weeks = (int) f;
            int days = (int) (7*frac);

            p = new Period(0, 0, weeks, days, 0, 0, 0, 0);

        } else if (  unitsString.toLowerCase(Locale.ENGLISH).contains("day")  ) {

            int days = (int) f;
            int hours = (int) (24.*frac);

            p = new Period(0, 0, 0, days, hours, 0, 0, 0);

        } else if (  unitsString.toLowerCase(Locale.ENGLISH).contains("hour") ) {

            int hours = (int) f;
            int minutes = (int)(60.*frac);

            p = new Period(0, 0, 0, 0, hours, minutes, 0, 0);

        } else if ( unitsString.toLowerCase(Locale.ENGLISH).contains("minute") ) {
            int minutes = (int) f;
            int seconds = (int) (60.*frac);
            p = new Period(0, 0, 0, 0, 0, minutes, seconds, 0);
        } else if ( unitsString.toLowerCase(Locale.ENGLISH).contains("second") ) {
            int seconds = (int) f;
            int millis = (int) (1000.d*frac);
            p = new Period(0, 0, 0, 0, 0, 0, seconds, millis);

        }


        if ( p != null ) {

            DateTime target = baseDT.plus(p).withChronology(chrono).withZone(DateTimeZone.UTC);

            String fdate = myLongFerretForm.print(target.getMillis());

            return fdate;
        } else {
            return "0001-Jan-01 00:00:00";
        }
    }
    private void hiDayChange() {
        if ( isMenu ) {
            int lo_i = lo_day.getSelectedIndex();
            int hi_i = hi_day.getSelectedIndex();
            if ( lo_i > hi_i ) {
                lo_day.setSelectedIndex(hi_i);
            }
        } else {
            int year = Integer.valueOf(hi_year.getValue(hi_year.getSelectedIndex())).intValue();
            int month = monthToInt(hi_month.getValue(hi_month.getSelectedIndex()));
            int day = Integer.valueOf(hi_day.getValue(hi_day.getSelectedIndex())).intValue();
            int hour = hi_hour.getHour();
            int	min;
            if ( hasMinute ) {
                min = intMinute(hi_minute.getSelectedValue());
            } else {
                min = hi_hour.getMin();
            }

            loadAndSetHour(hi_hour, hi_minute, year, month, day, hour, min);
            checkRangeStartDay();
        }
    }
    public ValueChangeHandler hiMinuteHandler = new ValueChangeHandler() {
        @Override
        public void onValueChange(ValueChangeEvent event) {
            checkRangeStartMinute();
            eventBus.fireEventFromSource(new DateChange(), DateTimeWidget.this);
        }

    };
    public boolean isContainedBy(String ferretDateLo, String ferretDateHi) {
        DateTime dateLo = parseFerretDate(ferretDateLo);
        DateTime dateHi = parseFerretDate(ferretDateHi);
        DateTime clo = parseFerretDate(getFerretDateLo());
        DateTime chi = parseFerretDate(getFerretDateHi());
        return (clo.isEqual(dateLo.getMillis()) || clo.isAfter(dateLo.getMillis())) && (chi.isEqual(dateHi.getMillis()) || chi.isBefore(dateHi.getMillis()));
    }
    /**
     * Helper method to parse ferret dates.
     * @param date_string of the form 15-Jan-1983, 20-Mar-1997 12:32 or 19-Mar-1962 12:11:03
     * @return Date for the parse date
     */
    private DateTime parseFerretDate(String date_string) {
        DateTime date;
        if ( date_string.length() == 6 ) {
            // A lovely climo date of the form 15-Jan
            date = shortFerretForm.parseDateTime(date_string+"-0001");
        } else if ( date_string.length() == 11 ) {
            date = shortFerretForm.parseDateTime(date_string);
        } else if ( date_string.length() == 17 ) {
            date = mediumFerretForm.parseDateTime(date_string);
        } else {
            date = longFerretForm.parseDateTime(date_string);
        }
        return date;
    }
    /**
     * Helper method to parse date strings
     * @param date_string of the form 1998-11-05, 1998-12-31 11:02 or 1923-11-14 04:13:21
     * @return
     */
    private DateTime parseDate(String date_string) {
        DateTime date;
         try {
                date = longForm.parseDateTime(date_string);
            } catch (IllegalArgumentException e) {
                try {
                     date = mediumForm.parseDateTime(date_string);
                } catch (IllegalArgumentException e1) {
                    try{
                        date = shortForm.parseDateTime(date_string);
                    } catch (IllegalArgumentException e2) {
                        try {
                            date = isoForm.parseDateTime(date_string);
                        } catch (IllegalArgumentException e3){
                            date = null;
                            Window.alert("Date parsing failed for " + date_string);
                        }
                    }
                }
            }
        return date;
    }
    /**
     * Take a date time string of the form,  and reformat it to the long ferret style. Assume a gregorian calendar.
     * @param in
     */
    public static String reformat (String in) {
        Chronology chrono = GregorianChronology.getInstance(DateTimeZone.UTC);
        DateTimeFormatter lForm = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").withChronology(chrono).withZone(DateTimeZone.UTC);
        DateTimeFormatter mForm = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm").withChronology(chrono).withZone(DateTimeZone.UTC);
        DateTimeFormatter sForm = DateTimeFormat.forPattern("yyyy-MM-dd").withChronology(chrono).withZone(DateTimeZone.UTC);
        DateTimeFormatter lFerretForm = DateTimeFormat.forPattern("dd-MMM-yyyy HH:mm:ss").withChronology(chrono).withZone(DateTimeZone.UTC);

        DateTime td;
        try {
            td = lForm.parseDateTime(in).withZone(DateTimeZone.UTC).withChronology(chrono);
        } catch (Exception e) {
            try {
                td = mForm.parseDateTime(in).withZone(DateTimeZone.UTC).withChronology(chrono);
            } catch (Exception e1) {
                td = sForm.parseDateTime(in).withZone(DateTimeZone.UTC).withChronology(chrono);
            }
        }
        if ( td != null ) {
            return lFerretForm.print(td.getMillis());
        } else {
            return null;
        }
    }
    // In the weird calendars, the short names don't work so well so we force the issue a little bit.
    private int monthToInt(String month_name) {
        DateTime dt = monthFormat.parseDateTime(month_name);
        return dt.getMonthOfYear();
    }
    private int intMinute(String value) {
        if ( value.startsWith("0") ) {
            return Integer.valueOf(value.substring(1)).intValue();
        } else {
            return Integer.valueOf(value).intValue();
        }
    }
}
