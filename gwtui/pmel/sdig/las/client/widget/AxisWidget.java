package pmel.sdig.las.client.widget;


import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.Composite;
import gwt.material.design.client.constants.Color;
import gwt.material.design.client.constants.Display;
import gwt.material.design.client.constants.HeadingSize;
import gwt.material.design.client.ui.MaterialListBox;
import gwt.material.design.client.ui.MaterialPanel;
import gwt.material.design.client.ui.MaterialRow;
import gwt.material.design.client.ui.html.Heading;
import pmel.sdig.las.client.event.PlotOptionChange;
import pmel.sdig.las.client.main.ClientFactory;
import pmel.sdig.las.shared.autobean.VerticalAxis;

/**
 * A generic axis widget suitable for a simple numeric access like x, y, or z.
 * 
 * @author rhs
 * 
 */
public class AxisWidget extends Composite {
    String type;

    MaterialPanel container = new MaterialPanel();
    Heading lo_label = new Heading(HeadingSize.H5);
    Heading hi_label_range = new Heading(HeadingSize.H5);
    MaterialListBox lo_axis = new MaterialListBox();
    MaterialListBox hi_axis = new MaterialListBox();

    MaterialRow lo_label_row = new MaterialRow();
    MaterialRow lo_widget_row = new MaterialRow();

    MaterialRow hi_label_row = new MaterialRow();
    MaterialRow hi_widget_row = new MaterialRow();

    NumberFormat format = NumberFormat.getFormat("###.##");
    boolean range;
    EventBus eventBus;


    /**
     * Construct an empty axis widget.
     */

    public AxisWidget() {

        ClientFactory cf = GWT.create(ClientFactory.class);
        eventBus = cf.getEventBus();
        lo_axis.setOld(true);
        hi_axis.setOld(true);

        lo_label.setFontSize("1.1em");
        lo_label.setFontWeight(Style.FontWeight.BOLD);
        lo_label.setTextColor(Color.BLUE);

        hi_label_range.setFontSize("1.1em");
        hi_label_range.setFontWeight(Style.FontWeight.BOLD);
        hi_label_range.setTextColor(Color.BLUE);

        lo_label.setGrid("s12");
        lo_axis.setGrid("s6");
        lo_axis.setMarginTop(0);
        lo_label_row.setMarginBottom(0);
        lo_widget_row.setMarginBottom(0);
        lo_label.setMarginTop(0);
        lo_label.setMarginBottom(8);

        lo_label_row.add(lo_label);
        lo_widget_row.add(lo_axis);

        container.setMarginBottom(0);

        container.add(lo_label_row);
        container.add(lo_widget_row);

        hi_label_range.setGrid("s12");
        hi_axis.setGrid("s6");
        hi_axis.setMarginTop(0);
        hi_label_row.setMarginBottom(0);
        hi_widget_row.setMarginBottom(0);
        hi_label_range.setMarginTop(0);
        hi_label_range.setMarginBottom(8);

        hi_label_row.add(hi_label_range);
        hi_widget_row.add(hi_axis);

        initWidget(container);
    }

    /**
     * Initialize an axis using a AxisSerializable object and boolean range
     * switch. Range set to false means there is only one widget (or set of
     * widgets in the case of time) visible and the user can only select one
     * point from that axis. Range set to true means that there are two
     * identical coordinated widgets (or set of widgets in the case of time)
     * from which you can select a starting point and an ending point from that
     * axis. The coordination between the widgets is such that you can not
     * select an endpoint that is before the starting point select. The widgets
     * update themselves to prevent this from happening.
     * 
     * @param ax
     * @param range
     */
    public void init(VerticalAxis ax, boolean range) {
        this.range = range;
        initialize(ax);
    }

    /**
     * Initialize an axis using a AxisSerializable object. isRange is false by
     * default.
     * 
     * @param ax
     */
    public void init(VerticalAxis ax) {
        this.range = false;
        
        initialize(ax);
    }
    protected void initialize(VerticalAxis ax) {
        container.clear();
        lo_axis.clear();
        hi_axis.clear();
        lo_axis.addValueChangeHandler(loAxisChangeHandler);
        hi_axis.addValueChangeHandler(hiAxisChangeHandler);

        String units = ax.getUnits();
        if (ax.getTitle() != null && !ax.getTitle().equals("")) {
            if (units != null && !units.equals("") && !units.equals("null") ) {
                lo_label.setText("Start " + ax.getTitle() + "  (" + units + "):");
                hi_label_range.setText("End " + ax.getTitle() + "  (" + units + "):");
            } else {
                lo_label.setText("Start " + ax.getTitle() + ":");
                hi_label_range.setText("End " + ax.getTitle() + ":");
            }
        } else {
            if (units != null && !units.equals("")) {
                lo_label.setText("Start (" + units + "): ");
                hi_label_range.setText("End (" + units + "): ");
            } else {
                lo_label.setText("Start :");
                hi_label_range.setText("End :");
            }
        }

        if (ax.getZvalues() != null && ax.getZvalues().size() > 0) {
            this.type = ax.getType();
            lo_axis.setName(type);

            for (int i = 0; i < ax.getZvalues().size(); i++) {
                lo_axis.addItem(String.valueOf(ax.getZvalues().get(i).getValue()), String.valueOf(ax.getZvalues().get(i).getValue()));
                hi_axis.addItem(String.valueOf(ax.getZvalues().get(i).getValue()), String.valueOf(ax.getZvalues().get(i).getValue()));
            }
            lo_axis.setSelectedIndex(0);
            hi_axis.setSelectedIndex(ax.getZvalues().size() - 1);
        } else {
            this.type = ax.getType();
            lo_axis.setName(type);

            int size = (int)(ax.getSize());
            double start = ax.getMin();
            double step = (ax.getMax()-ax.getMin())/ax.getSize();
            for (int i = 0; i < size; i++) {
                double value = start + i * step;
                String v = format.format(value);
                lo_axis.addItem(v);
                hi_axis.addItem(v);
            }
            lo_axis.setSelectedIndex(0);
            hi_axis.setSelectedIndex(hi_axis.getItemCount() - 1);
        }

        load_layout();
    }

    private void load_layout() {
        container.clear();
        if (range) {
            container.add(lo_label_row);
            container.add(lo_widget_row);
            container.add(hi_label_row);
            container.add(hi_widget_row);
        } else {
            container.add(lo_label_row);
            container.add(lo_widget_row);
            container.remove(hi_label_row);
            container.remove(hi_widget_row);
        }
    }

    public int getSelectedIndex() {
        return lo_axis.getSelectedIndex();
    }

    public String getValue(int i) {
        return lo_axis.getValue(i);
    }

    public int getItemCount() {
        return lo_axis.getItemCount();
    }
    public void setEnabled(boolean b) {
        lo_axis.setEnabled(b);
    }

    public String getLo() {
        return lo_axis.getValue(lo_axis.getSelectedIndex());
    }

    public String getHi() {
        if (range) {
            return hi_axis.getValue(hi_axis.getSelectedIndex());
        } else {
            return getLo();
        }
    }

    public String getType() {
        return this.type;
    }

    public void setLo(String lo) {
        for (int i = 0; i < lo_axis.getItemCount(); i++) {
            String value = lo_axis.getValue(i);
            if (lo.equals(value)) {
                lo_axis.setSelectedIndex(i);
            }
        }
        checkOrderLo();
    }

    /**
     * @param hi
     */
    public void setHi(String hi) {
        for (int i = 0; i < hi_axis.getItemCount(); i++) {
            String value = hi_axis.getValue(i);
            if (hi.equals(value)) {
                hi_axis.setSelectedIndex(i);
            }
        }
        checkOrderHi();
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
        if ( range ) {
            container.add(hi_label_row);
            container.add(hi_widget_row);
        } else {
            container.remove(hi_label_row);
            container.remove(hi_widget_row);        }
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

    public void checkOrderLo() {
        if (hi_axis.getSelectedIndex() < lo_axis.getSelectedIndex()) {
            if (lo_axis.getSelectedIndex() < lo_axis.getItemCount() - 2) {
                hi_axis.setSelectedIndex(lo_axis.getSelectedIndex() + 1);
            } else {
                hi_axis.setSelectedIndex(lo_axis.getSelectedIndex());
            }
        }
    }

    public void checkOrderHi() {
        if (hi_axis.getSelectedIndex() < lo_axis.getSelectedIndex()) {
            if (hi_axis.getSelectedIndex() > 1) {
                lo_axis.setSelectedIndex(hi_axis.getSelectedIndex() - 1);
            } else {
                lo_axis.setSelectedIndex(hi_axis.getSelectedIndex());
            }
        }
    }
    public boolean isContainedBy(String lo, String hi) {
        double dlo = Double.valueOf(lo);
        double dhi = Double.valueOf(hi);
        double cl = Double.valueOf(getLo());
        double ch = Double.valueOf(getHi());
        return cl >= dlo && cl <= dhi && ch >= dlo && ch <= dhi;
    }
    public ValueChangeHandler loAxisChangeHandler = new ValueChangeHandler() {
        @Override
        public void onValueChange(ValueChangeEvent event) {
            checkOrderLo();
            eventBus.fireEvent(new PlotOptionChange());
        }
    };
    public ValueChangeHandler hiAxisChangeHandler = new ValueChangeHandler() {
        @Override
        public void onValueChange(ValueChangeEvent event) {
            checkOrderHi();
            eventBus.fireEvent(new PlotOptionChange());
        }
    };
    public void setNearestLo(double zlo) {
        String top = lo_axis.getValue(0);
        String bottom = lo_axis.getValue(lo_axis.getItemCount() - 1);
        double t = Double.valueOf(top);
        double b = Double.valueOf(bottom);
        if ( t > b ) {
            if ( zlo >= t ) {
                lo_axis.setSelectedIndex(0);
                return;
            }
            if ( zlo <= b ) {
                lo_axis.setSelectedIndex(lo_axis.getItemCount() - 1);
                return;
            }
            for ( int i = 0; i < lo_axis.getItemCount() - 1; i++ ) {
                String v1 = lo_axis.getValue(i);
                String v2 = lo_axis.getValue(i+1);
                double dv1 = Double.valueOf(v1);
                double dv2 = Double.valueOf(v2);
                if ( zlo < dv1 && zlo >= dv2  ) {
                    lo_axis.setSelectedIndex(i+1);
                    return;
                }
            }
        } else {
            if ( zlo <= t ) {
                lo_axis.setSelectedIndex(0);
                return;
            }
            if ( zlo >= b ) {
                lo_axis.setSelectedIndex(lo_axis.getItemCount() - 1);
                return;
            }
            for ( int i = 0; i < lo_axis.getItemCount() - 1; i++ ) {
                String v1 = lo_axis.getValue(i);
                String v2 = lo_axis.getValue(i+1);
                double dv1 = Double.valueOf(v1);
                double dv2 = Double.valueOf(v2);
                if ( zlo > dv1 && zlo <= dv2 ) {
                    lo_axis.setSelectedIndex(i);
                    return;
                }
            }
        }
    }
    public void setNearestHi(double zhi) {
        String top = hi_axis.getValue(0);
        String bottom = hi_axis.getValue(hi_axis.getItemCount() - 1);
        double t = Double.valueOf(top);
        double b = Double.valueOf(bottom);
        if ( t > b ) {
            if ( zhi >= t ) {
                hi_axis.setSelectedIndex(0);
                return;
            }
            if ( zhi <= b ) {
                hi_axis.setSelectedIndex(hi_axis.getItemCount() - 1);
                return;
            }
            for ( int i = 0; i < hi_axis.getItemCount() - 1; i++ ) {
                String v1 = hi_axis.getValue(i);
                String v2 = hi_axis.getValue(i+1);
                double dv1 = Double.valueOf(v1);
                double dv2 = Double.valueOf(v2);
                if ( zhi < dv1 && zhi >= dv2  ) {
                    hi_axis.setSelectedIndex(i+1);
                    return;
                }
            }
        } else {
            if ( zhi <= t ) {
                hi_axis.setSelectedIndex(0);
                return;
            }
            if ( zhi >= b ) {
                hi_axis.setSelectedIndex(hi_axis.getItemCount() - 1);
                return;
            }
            for ( int i = 0; i < hi_axis.getItemCount() - 1; i++ ) {
                String v1 = hi_axis.getValue(i);
                String v2 = hi_axis.getValue(i+1);
                double dv1 = Double.valueOf(v1);
                double dv2 = Double.valueOf(v2);
                if ( zhi > dv1 && zhi <= dv2 ) {
                    hi_axis.setSelectedIndex(i+1);
                }
            }
        }
    }
    public void setDisplay(Display display) {
        container.setDisplay(display);
    }
}
