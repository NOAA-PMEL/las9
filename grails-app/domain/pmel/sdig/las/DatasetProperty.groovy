package pmel.sdig.las

class DatasetProperty {

    String type // "group" is a grails/DB reserved word
    String name
    String value

    static belongsTo = [dataset: Dataset]
    static mapping = {
        value type: 'text'
    }
    static constraints = {
    }

}
