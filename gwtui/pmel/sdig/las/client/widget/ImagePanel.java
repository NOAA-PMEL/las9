package pmel.sdig.las.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import gwt.material.design.client.ui.MaterialCollapsible;
import gwt.material.design.client.ui.MaterialCollapsibleBody;
import gwt.material.design.client.ui.MaterialCollapsibleHeader;
import gwt.material.design.client.ui.MaterialColumn;
import gwt.material.design.client.ui.MaterialImage;
import gwt.material.design.client.ui.MaterialLabel;
import gwt.material.design.client.ui.MaterialPanel;
import gwt.material.design.client.ui.html.Div;
import pmel.sdig.las.client.main.ClientFactory;
import pmel.sdig.las.client.state.State;
import pmel.sdig.las.shared.autobean.Annotation;
import pmel.sdig.las.shared.autobean.AnnotationGroup;
import pmel.sdig.las.shared.autobean.Variable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by rhs on 1/6/17.
 */
public class ImagePanel extends Composite {

    ClientFactory clientFactory = GWT.create(ClientFactory.class);
    EventBus eventBus = clientFactory.getEventBus();

    @UiField
    MaterialColumn panel;

    @UiField
    MaterialImage image;

    @UiField
    MaterialCollapsibleHeader breadcrumbs;

    @UiField
    MaterialCollapsibleBody annotations;


    @UiField
    MaterialPanel annotationPanel;

    @UiField
    MaterialCollapsible annotationsCollapse;

    int index;

    interface ResultsPanelUiBinder extends UiBinder<MaterialColumn, ImagePanel> {
    }

    private static ResultsPanelUiBinder ourUiBinder = GWT.create(ResultsPanelUiBinder.class);

    public ImagePanel() {
        initWidget(ourUiBinder.createAndBindUi(this));
    }

    public void setVisibility(boolean visibility) {
        this.setVisible(visibility);
    }
    public void clearAnnotations() {
        annotationPanel.clear();
    }
    public void addAnnotation(Widget w) {
        annotationPanel.add(w);
    }
    public void setState(State state) {
        String error = state.getPanelState(this.getTitle()).getResultSet().getError();
        if ( error != null && !error.equals("") ) {
            annotationPanel.clear();
            MaterialLabel l = new MaterialLabel();
            l.setText(error);
            annotationPanel.add(l);
        } else {
            List<AnnotationGroup> groups = state.getPanelState(this.getTitle()).getResultSet().getAnnotationGroups();
            annotationPanel.clear();
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
    }
    public void openAnnotations() {
        annotationsCollapse.open(1);
    }
    public void addBreadcrumb(Breadcrumb b) {

        int index = getBreadcrumbs().size();

        if ( index > 0 ) {
            Breadcrumb tail = (Breadcrumb) getBreadcrumbs().get(index - 1);
            Object tailObject = tail.getSelected();
            if ( tailObject instanceof Variable) {
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
    public MaterialCollapsibleHeader getBreadcrumbContainer() {
        return breadcrumbs;
    }

    public void setImage(String url) {
        image.setUrl(url);
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
    public MaterialImage getImage() {
        return image;
    }

    /**
     * Put a label object in the breadcrumb header (removing any existing one). Used in the animator.
     */
    public void setLabel(String text) {
        List<Widget> bclist = breadcrumbs.getChildrenList();
        MaterialLabel remove = null;
        for (int i = 0; i < bclist.size(); i++) {
            Widget w = bclist.get(i);
            if ( w instanceof MaterialLabel) {
                remove = (MaterialLabel) w;
            }
        }
        if ( remove != null ) {
            breadcrumbs.remove(remove);
        }
        MaterialLabel l = new MaterialLabel(text);
        breadcrumbs.add(l);
    }
}