package pmel.sdig.las

class Result {

    String name
	String type
    String file_type
    String mime_type
	String suffix
    String url
    String filename
    boolean linked = false

    static belongsTo = ResultSet

    static mapping = {
        url type: "text"
    }
    static constraints = {
        url nullable: true
        filename nullable: true
    }


}
