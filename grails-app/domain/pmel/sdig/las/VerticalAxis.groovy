package pmel.sdig.las

class VerticalAxis {
	String type;
	String units;
	String name;
	String title;
	String positive;
	boolean regular;
	double delta;
	double min;
	double max;
    double size;
	
	List zvalues
	
	static hasMany = [zvalues: Zvalue]
	
	static belongsTo = [dataset: Dataset]
	
	public setZV(List zv) {
		zvalues = zv
	}
	static mapping = {
		zvalues cascade:'all-delete-orphan'
	}
    static constraints = {
		units (nullable: true)
    }
}
