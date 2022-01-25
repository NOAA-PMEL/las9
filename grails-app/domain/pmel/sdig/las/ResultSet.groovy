package pmel.sdig.las

class ResultSet {

    // These are in the definitions that are persisted on the server
    String name
    List<Result> results

    // These only show up in the traffic between client and server.
    String product
    MapScale mapScale
    List<AnnotationGroup> annotationGroups
    int targetPanel;
    String error;
    Animation animation

    static hasMany = [results: Result, annotationGroups: AnnotationGroup]
    static belongsTo = [operation: Operation]

    static constraints = {
        operation nullable: true
        mapScale nullable: true
        annotationGroups nullable: true
        error nullable: true
        animation nullable: true
        product nullable: true
    }
    static mapping = {
        results lazy: false
    }
}
