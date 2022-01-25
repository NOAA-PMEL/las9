package pmel.sdig.las

class MenuItem {

	String title
	String value
	
	static belongsTo = [menu: MenuOption]
	
    static constraints = {
    }
}
