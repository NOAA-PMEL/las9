package pmel.sdig.las


class TimeAxis {

    String calendar
    String name
    String title
    String unitsString
    String units
    // These will always, always be full ISO date/time Strings...
    String start
    String end
    // A string of the form PyYmMwWdDThHmMsS (weeks will only appear by itself)
    // The period of the entire time axis
    String period
    // Same as above, the period between two time steps.
    String delta
    String position
    boolean climatology
    // The number of individual time points in the time axis
    long size

    String display_lo;
    String display_hi;

    List nameValuePairs

    static belongsTo = [dataset: Dataset]

    static hasMany = [nameValuePairs: NameValuePair]

    static constraints = {
        unitsString (nullable: true)
        nameValuePairs (nullable: true)
        period (nullable: true)
        position (nullable: true)
        display_hi (nullable: true)
        display_lo (nullable: true)
    }
}
