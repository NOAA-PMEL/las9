package pmel.sdig.las.client.util;

import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Window;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Util {

	public Util() {

	}
	/**
	 * Stolen from Bob S.
	 * This converts an angle (in degrees) into the range &gt;=0 to
	 *   &lt;360.
	 * <UL>
	 * <LI> If isMV(angle), it returns 0.
	 * </UL>
	 */
	public static final double angle0360(double degrees) {
		if (!Double.isFinite(degrees))
			return 0;

		while (degrees < 0)
			degrees += 360;
		while (degrees >= 360)
			degrees -= 360;

		//causes slight bruising
		//degrees = frac(degrees / 360) * 360;
		//now it is -360..360
		//if (degrees < 0)
		//    degrees += 360;

		return degrees;
	}
	/**
	 * Stolen from Bob S.
	 *
	 * This converts an angle (in degrees) into the range &gt;=-180 to &lt;180
	 * (180 becomes -180).
	 * @param degrees an angle (in degrees)
	 * @return the angle (in degrees) in the range &gt;=-180 to &lt;180.
	 *   If isMV(angle), it returns 0.
	 */
	public static final double anglePM180(double degrees) {
		if (!Double.isFinite(degrees))
			return 0;

		while (degrees < -180) degrees += 360;
		while (degrees >= 180) degrees -= 360;

		return degrees;
	}
	public static String padRight(String s, int n) {
		int pad = n = s.length();
		for ( int i = 0; i < pad; i++ ) {
			s = s + " ";
		}
		return s;
	}
	public static String format_two(int i) {
		// Really an error for i<10 and i>99, but these are 1<days<31 and 0<hours<23.
		if ( i < 10 ) {
			return "0"+i;
		} else {
			return String.valueOf(i);
		}
	}
	public static String format_four (int i) {
		// Really an error for i<100 and i>9999, but these are years which start at 0001 or at worst 0000.
		if ( i < 10 ) {
			return "000"+i;
		} else if ( i >= 10 && i < 100 ) {
			return "00"+i;
		} else if ( i >= 100 && i < 1000 ) {
			return "0"+i;
		} else {
			return String.valueOf(i);
		}
	}

	public static String format_two(double d) {
		NumberFormat dFormat = NumberFormat.getFormat("########.##");
		return dFormat.format(d);
	}
	public static String format_four(double d) {
		NumberFormat dFormat = NumberFormat.getFormat("########.####");
		return dFormat.format(d);
	}
	public static String[] getParameterStrings(String name) {
		Map<String, List<String>> parameters = Window.Location.getParameterMap();
		List param = parameters.get(name);
		if ( param != null ) {
			int i = 0;
			String[] ps = new String[param.size()];
			for (Iterator paramIt = param.iterator(); paramIt.hasNext(); ) {
				String p = (String) paramIt.next();
				ps[i] = p;
				i++;
			}
			return ps;
		}
		return null;
	}
	public static String getParameterString(String name) {
		Map<String, List<String>> parameters = Window.Location.getParameterMap();
		List param = parameters.get(name);
		if ( param != null ) {
			return (String) param.get(0);
		}
		return null;
	}
	public static List<String> getLonConstraint(double xloDbl, double xhiDbl, boolean hasLon360, String lon_domain, String lonname) {
		StringBuilder query = new StringBuilder();
		StringBuilder query2 = new StringBuilder();
		if ( (xloDbl < xhiDbl) && Math.abs(xhiDbl - xloDbl) < 355.0 || ((xloDbl > xhiDbl) && Math.abs(xhiDbl - xloDbl) > 5.0 )) {
			if (lon_domain.contains("180")) {
				if (xloDbl < xhiDbl) {
					// Going west to east does not cross dateline, normal constraint
					if ( xloDbl <= 180.0d && xloDbl >= -180.0d && xhiDbl <= 180.0d && xhiDbl >= -180.0d ||
							( xloDbl >= 180.0d && xhiDbl >= 180.0d && !hasLon360 ) ) {
						query.append("&" + lonname + ">=" + anglePM180(xloDbl));
						query.append("&" + lonname + "<=" + anglePM180(xhiDbl));
						// Crosses 180 two parts since we don't have lon360
					} else if (xloDbl <= 180.0d && xhiDbl >= 180.0d && !hasLon360) {
						query.append("&" + lonname + ">=" + anglePM180(xloDbl));
						query.append("&" + lonname + "<=180.0");
						query2.append("&" + lonname + ">=-180.0");
						query2.append("&" + lonname + "<=" + anglePM180(xhiDbl));
						// Going east to west does not cross Greenwich, normal lon360 constraint from lon360 input
						// lon360 boolean indicates data set has has such a variable. If not, don't constrain on lon where lon360 is needed
					} else if (xloDbl <= 360.0d && xloDbl >= 0.0d && xhiDbl <= 360.0d && xhiDbl >= 0.0d && hasLon360) {
						query.append("&lon360>=" + xloDbl);
						query.append("&lon360<=" + xhiDbl);
					}
				} else if (xloDbl > xhiDbl) {
					// Going west to east
					// lon360 boolean varifies data has has such a varaible. If not, don't constrain on lon where lon360 is needed
					if (xloDbl <= 180.0d && xloDbl >= -180.0d && xhiDbl <= 180.0d && xhiDbl >= -180.0d) {
						// Going west to east over dateline, but not greenwich, convert to lon360 from -180 to 180 input
						if (xloDbl > 0 && xhiDbl < 0 && hasLon360) {
							xhiDbl = xhiDbl + 360;
							query.append("&lon360>=" + xloDbl);
							query.append("&lon360<=" + xhiDbl);
						} else {
							query.append("&" + lonname + ">=" + xloDbl);
							query.append("&" + lonname + "<180.0");
							query2.append("&" + lonname + ">=-180.0");
							query2.append("&" + lonname + "<=" + xhiDbl);
						}
					} else if (xloDbl <= 360.0d && xloDbl >= 0.0d && xhiDbl <= 360.0d && xhiDbl >= 0.0d) {
						// Going west to east does not cross dateline, from 360 input, just normal -180 to 180
						if (xloDbl > 180.0d && xhiDbl < 180.0d) {
							xloDbl = anglePM180(xloDbl);
							xhiDbl = anglePM180(xhiDbl);
							query.append("&" + lonname + ">=" + xloDbl);
							query.append("&" + lonname + "<=" + xhiDbl);
						} else if ( xloDbl > 180.0d && xhiDbl > 180.0d ) {
							query.append("&" + lonname + ">=" + anglePM180(xloDbl));
							query.append("&" + lonname + "<180.0");
							query2.append("&" + lonname + ">=-180.0");
							query2.append("&" + lonname + "<=" + anglePM180(xhiDbl));
						}
					}
				}
			} else {
				if (xloDbl < xhiDbl) {
					// Going west to east does not cross 180, normal constraint
					// Going east to west does not cross Greenwich, normal lon360
					// with values normalized to 0 360 since that's what the data are
					if ((xloDbl <= 0.0d && xloDbl >= -180.0d && xhiDbl <= 0.0d && xhiDbl >= -180.0d) ||
							(xloDbl <= 180.0d && xloDbl >= 0.0d && xhiDbl <= 180.0d && xhiDbl >= 0.0d) ||
							(xloDbl <= 360.0d && xloDbl >= 0.0d && xhiDbl <= 360.0d && xhiDbl >= 0.0d)) {
						query.append("&" + lonname + ">=" + angle0360(xloDbl));
						query.append("&" + lonname + "<=" + angle0360(xhiDbl));
					} else if ( xhiDbl > 0.0 ) {
						query.append("&" + lonname + ">=" + angle0360(xloDbl));
						query.append("&" + lonname + "<360.0");
						query2.append("&" + lonname + ">=0.0");
						query2.append("&" + lonname + "<=" + angle0360(xhiDbl));
					}
				} else if (xloDbl > xhiDbl) {
					// Going west to east
					// lon360 boolean data has has such a varaible. If not, don't constrain on lon where lon360 is needed
					if (xloDbl <= 180.0d && xloDbl >= -180.0d && xhiDbl <= 180.0d && xhiDbl >= -180.0d) {
						// Going west to east over dateline, but not greenwich, convert to lon360 from -180 to 180 input
						if (xloDbl > 0.0d && xhiDbl < 0.0d) {
							query.append("&" + lonname + ">=" + angle0360(xloDbl));
							query.append("&" + lonname + "<=" + angle0360(xhiDbl));
						} else if ( xloDbl <= 0.0d && xhiDbl <= 0.0d ) {
							query.append("&" + lonname + ">=" + angle0360(xloDbl));
							query.append("&" + lonname + "<360.0");
							query2.append("&" + lonname + ">=0.0");
							query2.append("&" + lonname + "<=" + angle0360(xhiDbl));
						}
					} else if (xloDbl <= 360.0d && xloDbl >= 0.0d && xhiDbl <= 360.0d && xhiDbl >= 0.0d) {
						// Going west to east from 360 input and 360 data get in two chunks
						query.append("&" + lonname + ">=" + angle0360(xloDbl));
						query.append("&" + lonname + "<360.0");
						query2.append("&" + lonname + ">=0.0");
						query2.append("&" + lonname + "<=" + angle0360(xhiDbl));
					}
				}
			}
		}
		List<String> q = new ArrayList<>();
		if ( !query.toString().isEmpty() ) q.add(query.toString());
		if ( !query2.toString().isEmpty() ) q.add(query2.toString());
		return q;
	}
}
