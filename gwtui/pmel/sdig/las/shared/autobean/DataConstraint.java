package pmel.sdig.las.shared.autobean;

public class DataConstraint {
    String type;  // type="variable" is for numeric values; type="text" is for exact matches on the value as a string
    String lhs;
    String op;
    String rhs;

    public DataConstraint() {
    }

    public DataConstraint(String type, String lhs, String op, String rhs) {
        this.type = type;
        this.lhs = lhs;
        this.op = op;
        this.rhs = rhs;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLhs() {
        return lhs;
    }

    public void setLhs(String lhs) {
        this.lhs = lhs;
    }

    public String getOp() {
        return op;
    }

    public void setOp(String op) {
        this.op = op;
    }

    public String getRhs() {
        return rhs;
    }

    public void setRhs(String rhs) {
        this.rhs = rhs;
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
    /**
     *
     * @return constraint -- the SQL ready string to go in the WHERE clause
     */
    public String getAsString() {
        String constraintString = "";
        try {
            Float.valueOf(rhs).floatValue();
            constraintString = constraintString + lhs+getOpAsSymbol()+rhs;
        } catch (NumberFormatException e) {
            constraintString = constraintString + lhs+getOpAsSymbol()+"\""+rhs+"\"";
        }
        return constraintString;
    }
}
