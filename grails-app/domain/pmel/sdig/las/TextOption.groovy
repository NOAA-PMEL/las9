package pmel.sdig.las

class TextOption {

    String name
    String title
    String help
    String value;
    String hint;
    String defaultValue

    static belongsTo = Operation

    static constraints = {
        help nullable: true, type: "text"
        defaultValue nullable: true
        value nullable: true
        hint nullable: true
    }
}
