package pmel.sdig.las

class DataQualifier {

    String type                 // one of orderBy, orderByClosest, orderByCount, orderByLimit, orderByMax, orderByMin, orderByMinMax
    boolean distinct            // If true adds &distinct() to the query
    List<String> variables      // ERDDAP shortnames
    String timeCount           // Appliest to orderByClosest("variable1, variable2, timeCount timeUnits") eg ("stationID, time, 2 hours")
    String timeUnits

    static constraints = {
        type (nullable: true)
        variables (nullable: true)
        timeCount (nullable: true)
        timeUnits (nullable: true)
    }
}
