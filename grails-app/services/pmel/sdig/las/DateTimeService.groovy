package pmel.sdig.las



import org.joda.time.Chronology
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.chrono.GregorianChronology
import org.joda.time.chrono.JulianChronology
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.ISODateTimeFormat

import pmel.sdig.las.time.AllLeapChronology
import pmel.sdig.las.time.NoLeapChronology
import pmel.sdig.las.time.ThreeSixtyDayChronology

class DateTimeService {

	Chronology chrono;

	private DateTimeFormatter isoForm;
	private DateTimeFormatter isoFormPrinter;

	private DateTimeFormatter shortFerretForm;
	private DateTimeFormatter mediumFerretForm;
	private DateTimeFormatter longFerretForm;

	DateTime dateTimeFromIso(String iso) {
		return isoForm.parseDateTime(iso)
	}
	DateTime dateTimeFromFerret(String ferret, String calendar) {
		init(calendar)
		DateTime ferretDT = null;
		try {
			ferretDT = shortFerretForm.parseDateTime(ferret);
		} catch (Exception e) {
			try {
				ferretDT = mediumFerretForm.parseDateTime(ferret);
			} catch ( Exception e2 ) {
				try {
					ferretDT = longFerretForm.parseDateTime(ferret);
				} catch (Exception e3) {
					// punt
				}
			}
		}
		ferretDT
	}
	def String isoFromFerret(String ferret, String calendar) {

		init(calendar)

		DateTime isoDT = dateTimeFromFerret(ferret, calendar)

		try {
			return isoFormPrinter.print(isoDT.getMillis());
		} catch (Exception e) {
			return "";
		}


	}
	def String ferretFromIso(String iso, String calendar) {
		init(calendar)
		DateTime ferretDT;
		try {
			ferretDT = isoForm.parseDateTime(iso);
			return longFerretForm.print(ferretDT);
		} catch (Exception e) {
			return ""
		}

	}
	def String isoFromDateTime(DateTime dateTime, String calendar) {
		init(calendar)
		isoFormPrinter.print(dateTime)
	}
	// Make a map of chrono, parsers and printers (most will be the same).
	// Run the init at bootstrap time.
	// get the chrono, parser and printer at runtime from the map.
	private void init(String calendar) {
		chrono = GregorianChronology.getInstance(DateTimeZone.UTC);

		if ( calendar != null && !calendar.equals("") )	{
			if ( calendar.equalsIgnoreCase("proleptic_gregorian") ) {
				chrono = GregorianChronology.getInstance(DateTimeZone.UTC);
			} else if ( calendar.equalsIgnoreCase("noleap") || calendar.equals("365_day") ) {
				chrono = NoLeapChronology.getInstanceUTC();
			} else if (calendar.equals("julian") ) {
				chrono = JulianChronology.getInstanceUTC();
			} else if ( calendar.equals("all_leap") || calendar.equals("366_day") ) {
				chrono = AllLeapChronology.getInstanceUTC();
			} else if ( calendar.equals("360_day") ) {
				chrono = ThreeSixtyDayChronology.getInstanceUTC();
			}
		}

		shortFerretForm = DateTimeFormat.forPattern("dd-MMM-yyyy").withChronology(chrono).withZone(DateTimeZone.UTC);
		mediumFerretForm = DateTimeFormat.forPattern("dd-MMM-yyyy HH:mm").withChronology(chrono).withZone(DateTimeZone.UTC);
		longFerretForm = DateTimeFormat.forPattern("dd-MMM-yyyy HH:mm:ss").withChronology(chrono).withZone(DateTimeZone.UTC);
		isoForm = ISODateTimeFormat.dateTimeParser().withChronology(chrono).withZone(DateTimeZone.UTC);
		isoFormPrinter = ISODateTimeFormat.dateTime().withChronology(chrono).withZone(DateTimeZone.UTC);
	}
}
