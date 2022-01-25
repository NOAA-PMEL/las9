package pmel.sdig.las

class GeoAxisY {
	
	String type
	String units
	String name
	String title
	int dimensions
	boolean regular
	double min
	double max
	double delta
	long size

	static belongsTo = [dataset: Dataset]
	
    static constraints = {
    }
}
