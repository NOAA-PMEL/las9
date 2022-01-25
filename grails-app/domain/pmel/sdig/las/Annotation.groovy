package pmel.sdig.las

class Annotation {

    String type
    String value

    static belongsTo = [AnnotationGroup]

    static constraints = {
    }
}
