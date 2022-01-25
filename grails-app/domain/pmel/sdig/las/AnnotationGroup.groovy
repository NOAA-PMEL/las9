package pmel.sdig.las

class AnnotationGroup {

    List annotations;
    String type
    static hasMany = [annotations: Annotation]

    static constraints = {
    }
}
