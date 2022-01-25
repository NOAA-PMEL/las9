package pmel.sdig.las

class  MenuOption {
	
	String name
    String title
    String help
    String defaultValue
    List menuItems

	static hasMany = [menuItems: MenuItem]
    static belongsTo = [operation: Operation]

    static constraints = {
        help type: "text"
        defaultValue nullable: true
    }
}
