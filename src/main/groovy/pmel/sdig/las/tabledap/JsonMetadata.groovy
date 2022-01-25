package pmel.sdig.las.tabledap

import grails.converters.JSON
import org.grails.web.json.JSONArray
import org.grails.web.json.JSONObject
import pmel.sdig.las.LASProxy

class JsonMetadata {

    LASProxy lasProxy = new LASProxy();

    JSONObject metadata

    JsonMetadata(String json) {
        metadata = JSON.parse(json);
    }
    String getAssociatedVariable(String attribute, String value) {
        JSONArray rows = metadata.get("table").get("rows")
        for (int i = 0; i < rows.size(); i++) {
            JSONArray row = rows.get(i)
            if ( row.get(0).toString().equals("attribute") && row.get(2).toString().equals(attribute) && row.get(4).toString().equalsIgnoreCase(value) ) {
                return row.get(1).toString()
            }
        }
        return null
    }
    String getAttributeValue(String variable, String name) {
        JSONArray rows = metadata.get("table").get("rows")
        for (int i = 0; i < rows.size(); i++) {
            JSONArray row = rows.get(i)
            if ( row.get(0).toString().equals("attribute") && row.get(1).toString().equals(variable) && row.get(2).equals(name) ) {
                return row.get(4).toString()
            }
        }
        return null
    }
    String getVariableWithCf_role() {
        JSONArray rows = metadata.get("table").get("rows")
        for (int i = 0; i < rows.size(); i++) {
            JSONArray row = rows.get(i)
            if ( row.get(0).toString().equals("attribute") && row.get(2).equals("cf_role") ) {
                return row.get(1).toString()
            }
        }
        return null
    }
    String getVariableWithCf_role(String role) {
        JSONArray rows = metadata.get("table").get("rows")
        for (int i = 0; i < rows.size(); i++) {
            JSONArray row = rows.get(i)
            if ( row.get(0).toString().equals("attribute") && row.get(2).equals("cf_role") ) {
                if ( row.get(4).equals(role) ) {
                    return row.get(1).toString()
                }
            }
        }
        return null
    }
    List<String> getVariables() {
        List<String> variables = new ArrayList<>()
        JSONArray rows = metadata.get("table").get("rows")
        for (int i = 0; i < rows.size(); i++) {
            JSONArray row = rows.get(i)
            if ( row.get(0).toString().equals("variable")) {
                variables.add(row.get(1))
            }
        }
        variables
    }
}
