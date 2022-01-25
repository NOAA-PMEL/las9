package pmel.sdig.las.client.widget;

import gwt.material.design.client.ui.MaterialLink;

public class ConstraintLink extends MaterialLink {

    String type;
    String lhs;
    String op;
    String rhs;

    public ConstraintLink () {
        super();
    }
    public ConstraintLink (String type, String lhs, String op, String rhs) {
        this.type = type;
        this.lhs = lhs;
        this.op = op;
        this.rhs = rhs;
        setText(lhs + getOpAsSymbol() + rhs);
        addStyleName("constraintHeight");
    }

    public String getOpAsSymbol() {
        String opString = "";
        if ( op.equals("lt")) {
            opString = "<";
        } else if ( op.equals("le")) {
            opString = "<=";
        } else if (op.equals("eq")) {
            opString = "=";
        } else if (op.equals("ne") ) {
            opString = "!=";
        } else if (op.equals("gt")) {
            opString = ">";
        } else if (op.equals("ge")) {
            opString = ">=";
        } else if ( op.equals("like") ) {
            opString = "=~";
        } else if ( op.equals("is") ) {
            opString = "=~";
        }
        return opString;
    }
}
