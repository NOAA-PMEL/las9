package pmel.sdig.las.shared.autobean;

import java.util.List;

public class DataQualifier {
    String type;                 // one of orderBy, orderByClosest, orderByCount, orderByLimit, orderByMax, orderByMin, orderByMinMax
    boolean distinct;           // If true adds &distinct() to the query
    List<String> variables;      // ERDDAP shortnames
    String timeCount;           // Appliest to orderByClosest("variable1, variable2, timeCount timeUnits") eg ("stationID, time, 2 hours")
    String timeUnits;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isDistinct() {
        return distinct;
    }

    public void setDistinct(boolean distinct) {
        this.distinct = distinct;
    }

    public List<String> getVariables() {
        return variables;
    }

    public void setVariables(List<String> variables) {
        this.variables = variables;
    }

    public String getTimeCount() {
        return timeCount;
    }

    public void setTimeCount(String timeCount) {
        this.timeCount = timeCount;
    }

    public String getTimeUnits() {
        return timeUnits;
    }

    public void setTimeUnits(String timeUnits) {
        this.timeUnits = timeUnits;
    }
}
