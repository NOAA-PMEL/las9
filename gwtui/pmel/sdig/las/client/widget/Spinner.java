package pmel.sdig.las.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;

public class Spinner extends Composite {
	
	private Widget root;

	private static SpinnerUiBinder uiBinder = GWT.create(SpinnerUiBinder.class);

	interface SpinnerUiBinder extends UiBinder<Widget, Spinner> {
	}

	@UiField
	HTMLPanel message;

	public Spinner() {
		root = uiBinder.createAndBindUi(this);
		initWidget(root);
	}
	
	public void setMessage(String m) {
		message.clear();
		message.add(new HTML(m));
	}

}
