package pmel.sdig.las

/**
 * A simple class that holds a command-line argument for running Ferret (or PyFerret)
 * @author rhs
 *
 */
class Argument {
	String value;
	static belongsTo = [ferret:Ferret]
    static constraints = {
		value(blank:false, nullable:false)
    }
}
