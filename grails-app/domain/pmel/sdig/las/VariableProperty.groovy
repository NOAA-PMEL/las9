package pmel.sdig.las

class VariableProperty {

    String type // "group" is a grails/DB reserved word
    String name
    String value

    static belongsTo = [variable: Variable]

    static constraints = {
    }

}
