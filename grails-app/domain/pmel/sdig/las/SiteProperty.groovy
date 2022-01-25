package pmel.sdig.las

class SiteProperty {

    String type // "group" is a grails/DB reserved word
    String name
    String value

    static belongsTo = [site: Site]

    static constraints = {
    }
}
