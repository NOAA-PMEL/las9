package pmel.sdig.las.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

public class LabelAndControls extends Composite {
	
	interface LabelAndControlsUiBinder extends UiBinder<Widget, LabelAndControls> {} {}
	private static LabelAndControlsUiBinder pigBinder = GWT.create(LabelAndControlsUiBinder.class);

	@UiField HTMLPanel labels;
	@UiField HTMLPanel controls;

	private Widget root;
	
	public LabelAndControls() {
		root = pigBinder.createAndBindUi(this);
		initWidget(root);
	}


	public void addLabel(Widget widget) {
		labels.add(widget);
	}

	public void addControls(Widget widget) {
		controls.add(widget);
	}

	public void clear() {
		labels.clear();
		controls.clear();
	}

}
