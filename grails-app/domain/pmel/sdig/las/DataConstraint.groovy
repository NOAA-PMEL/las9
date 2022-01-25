package pmel.sdig.las

class DataConstraint {
    String type;
    String lhs;
    String op;
    String rhs;
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
    public String getAsERDDAPString() {
        // Even stuff that looks like a number has to be enclosed in quotes for ERDDAP variables that come is a list of distinct values.
        if ( op.equals("is") || op.equals("like") || rhs.contains("*") || rhs.contains("[") || rhs.contains("]") ) {
            lhs = lhs.replaceAll("_ns_", "|");

            String[] parts = rhs.split("_ns_");
            StringBuilder r = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {

                String p = parts[i]; // Don't use quote pattern... Pattern.quote(parts[i]);
                r.append(p);
                if ( i < parts.length - 1 ) {
                    r.append("|");
                }
            }

            rhs = r.toString();

        }
        return lhs+getOpAsSymbol()+"\""+rhs+"\"";
    }
}
