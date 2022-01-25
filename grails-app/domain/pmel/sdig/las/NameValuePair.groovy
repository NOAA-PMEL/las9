package pmel.sdig.las

class NameValuePair {

	String name
	String value
	
	static belongsTo = [timeAxis: TimeAxis]
	
    static constraints = {
    }
}
