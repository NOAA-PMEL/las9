package pmel.sdig.las.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import gwt.material.design.client.constants.Display;
import gwt.material.design.client.constants.IconType;
import gwt.material.design.client.events.CollapseEvent;
import gwt.material.design.client.events.ExpandEvent;
import gwt.material.design.client.ui.MaterialCollapsible;
import gwt.material.design.client.ui.MaterialCollapsibleBody;
import gwt.material.design.client.ui.MaterialCollapsibleHeader;
import gwt.material.design.client.ui.MaterialCollapsibleItem;
import gwt.material.design.client.ui.MaterialColumn;
import gwt.material.design.client.ui.MaterialIcon;
import gwt.material.design.client.ui.MaterialLabel;
import gwt.material.design.client.ui.MaterialPanel;
import gwt.material.design.client.ui.html.Div;
import pmel.sdig.las.client.main.ClientFactory;
import pmel.sdig.las.client.state.State;
import pmel.sdig.las.client.util.Constants;
import pmel.sdig.las.shared.autobean.Annotation;
import pmel.sdig.las.shared.autobean.AnnotationGroup;
import pmel.sdig.las.shared.autobean.Variable;
import pmel.sdig.las.shared.autobean.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by rhs on 1/6/17.
 */
public class ResultsPanel extends Composite {

    ClientFactory clientFactory = GWT.create(ClientFactory.class);
    EventBus eventBus = clientFactory.getEventBus();

    @UiField
    MaterialColumn panel;

    @UiField
    OutputPanel outputPanel;

    @UiField
    Div chart;

    @UiField
    MaterialPanel breadcrumbs;

    @UiField
    MaterialCollapsibleBody annotations;


    @UiField
    MaterialPanel annotationPanel;

    @UiField
    MaterialCollapsible annotationsCollapse;
    @UiField
    MaterialIcon trigger;

    int index;

    interface ResultsPanelUiBinder extends UiBinder<MaterialColumn, ResultsPanel> {
    }

    private static ResultsPanelUiBinder ourUiBinder = GWT.create(ResultsPanelUiBinder.class);

    public ResultsPanel() {
        initWidget(ourUiBinder.createAndBindUi(this));
        annotationsCollapse.addExpandHandler(new ExpandEvent.ExpandHandler<MaterialCollapsibleItem>() {
            @Override
            public void onExpand(ExpandEvent<MaterialCollapsibleItem> expandEvent) {
                trigger.setIconType(IconType.EXPAND_LESS);
            }
        });
        annotationsCollapse.addCollapseHandler(new CollapseEvent.CollapseHandler<MaterialCollapsibleItem>() {
            @Override
            public void onCollapse(CollapseEvent<MaterialCollapsibleItem> collapseEvent) {
                trigger.setIconType(IconType.EXPAND_MORE);
            }
        });
    }
    public void clearAnnotations() {
        annotationPanel.clear();
    }
    public void addAnnotation(Widget w) {
        annotationPanel.add(w);
    }
    public void setState(State state) {
        String error = state.getPanelState(this.getTitle()).getResultSet().getError();
        annotationPanel.clear();
        if ( error != null && !error.equals("") ) {
            String[] parts = error.split("\n");
            for (int i = 0; i < parts.length; i++) {
                MaterialLabel l = new MaterialLabel();
                if ( !parts[i].toLowerCase().contains("note") ) {
                    l.setText(parts[i]);
                    if (parts[i].toLowerCase().contains("err")) {
                        l.setFontWeight(Style.FontWeight.BOLDER);
                    }
                    annotationPanel.add(l);
                }
            }
            clearPlot();
        } else {
            outputPanel.setState(state);
            List<AnnotationGroup> groups = state.getPanelState(this.getTitle()).getResultSet().getAnnotationGroups();
            for (Iterator<AnnotationGroup> gIt = groups.iterator(); gIt.hasNext(); ) {
                AnnotationGroup ag = gIt.next();
                for (Iterator<Annotation> aIt = ag.getAnnotations().iterator(); aIt.hasNext(); ) {
                    MaterialLabel l = new MaterialLabel();
                    Annotation a = aIt.next();
                    l.setText(a.getValue());
                    annotationPanel.add(l);
                }
            }
        }
        annotationsCollapse.open(1);
        // for some reason the collapse handler fire in this case
        // it's now open so make it visible again after the collapse handler fired
        outputPanel.setVisible(true);
    }
    public void openAnnotations() {
        annotationsCollapse.open(1);
    }
    public OutputPanel getOutputPanel() {
        return outputPanel;
    }
    public Div getChart() { return chart; }

    public void addBreadcrumb(Breadcrumb b) {

        int index = getBreadcrumbs().size();

        if ( index > 0 ) {
            Breadcrumb tail = (Breadcrumb) getBreadcrumbs().get(index - 1);
            Object tailObject = tail.getSelected();
            if ( tailObject instanceof Variable || tailObject instanceof Vector ) {
                removeBreadcrumb(tail);
                breadcrumbs.add(b);
            } else {
                breadcrumbs.add(b);
            }
        } else {
            breadcrumbs.add(b);
        }
    }

    public void removeBreadcrumb(Breadcrumb tail) {
        breadcrumbs.remove(tail);
    }

    public void setGrid(String grid) {
        panel.setGrid(grid);
    }

    public List<Breadcrumb> getBreadcrumbs() {

        List<Breadcrumb> crumbs = new ArrayList<Breadcrumb>();
        for (int i = 0; i < breadcrumbs.getWidgetCount(); i++) {
            Widget w = breadcrumbs.getWidget(i);
            if ( w instanceof Breadcrumb ) {
                crumbs.add((Breadcrumb) w);
            }
        }

        return crumbs;
    }
    public MaterialPanel getBreadcrumbContainer() {
        return breadcrumbs;
    }

    public void scale() {
        outputPanel.scale();
    }
    public void scale(int navWidth) {
        outputPanel.scale(navWidth);
    }
    public void setLayoutPosition(Style.Position postion) {
        panel.setLayoutPosition(postion);
    }
    public void setLeft(double left) {
        panel.setLeft(left);
    }
    public void setTop(double top) {
        panel.setTop(top);
    }
    public void setIndex(int index) {
        this.index = index;
    }
    public double getDataMax() {
        return outputPanel.getDataMax();
    }
    public double getDataMin() {
        return outputPanel.getDataMin();
    }
    public String getLevels_string() {
        return outputPanel.levels;
    }
    public void clearPlot() {
        outputPanel.clearPlot();
        chart.clear();
        chart.setVisible(false);
    }
    public int countAnnotations() {
        int count = 0;
        for (int i = 0; i < annotationPanel.getWidgetCount(); i++) {
            Widget w = annotationPanel.getWidget(i);
            if ( w instanceof MaterialLabel ) {
                count++;
            }
        }
        return count;
    }
    public void padAnnotations(int pad) {
        for (int i = 0; i < pad; i++) {
            MaterialLabel label = new MaterialLabel("blank");
            label.getElement().setInnerHTML("&nbsp;");
            annotationPanel.add(label);
        }
    }
    public List<Widget> getAnnotations() {
        return annotationPanel.getChildrenList();
    }
}
