package pmel.sdig.las

class AddRequest {

    List addProperties

    String url
    String type

    static hasMany = [addProperties: AddProperty]

    static mapping = {
        url type: "text"
    }
    static constraints = {
    }
}
