package pmel.sdig.las

class FooterLink {

    String url
    String linktext
    int linkindex

    static belongsTo = [Site]

    static constraints = {
    }
}
