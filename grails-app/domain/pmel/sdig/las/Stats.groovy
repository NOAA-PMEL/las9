package pmel.sdig.las

import java.text.DecimalFormat

class Stats {

	String stat_min
	String stat_max
	String stat_mean
	String stat_std
	String stat_ngood
	String stat_nbad
	String histogram_lev_min
	String histogram_lev_max
	String histogram_lev_num
	String histogram_lev_del
	String histogram_lev_txt
	
	String palette
	
	static belongsTo = [variable: Variable]

	static constraints = {

		palette(nullable: true)
	
		stat_min (nullable: true)
		stat_max (nullable: true)
		stat_mean (nullable: true)
		stat_std (nullable: true)
		stat_ngood (nullable: true)
		stat_nbad (nullable: true)
		histogram_lev_min (nullable: true)
		histogram_lev_max (nullable: true)
		histogram_lev_num (nullable: true)
		histogram_lev_del (nullable: true)
		histogram_lev_txt (nullable: true)
		
	}

	public Stats() {
		super();
	}
	public Stats(String fileName) {
		File statFile = new File(fileName);
		statFile.eachLine {
			def parts = it.trim().replaceAll("\"", "").split("\\s+")
			if ( parts.length == 2 ) {
				if ( parts[0].equals("STAT_MIN") ) {
					this.stat_min = parts[1]
				} else if ( parts[0].equals("STAT_MAX") ) {
					this.stat_max = parts[1]
				} else if ( parts[0] == "STAT_MEAN" ) {
					this.stat_mean = parts[1]
				} else if ( parts[0] == "STAT_STD" ) {
					this.stat_std = parts[1]
				} else if ( parts[0] == "STAT_NGOOD") {
					this.stat_ngood = parts[1]
				} else if ( parts[0] == "STAT_NBAD") {
					this.stat_nbad = parts[1]
				} else if ( parts[0] == "HISTOGRAM_LEV_MIN") {
					this.histogram_lev_min = parts[1]
				} else if ( parts[0] == "HISTOGRAM_LEV_MAX") {
					this.histogram_lev_max = parts[1]
				} else if ( parts[0] == "HISTOGRAM_LEV_NUM") {
					this.histogram_lev_num = parts[1]
				} else if ( parts[0] == "HISTOGRAM_LEV_DEL") {
					this.histogram_lev_del = parts[1]
				} else if ( parts[0] == "HISTOGRAM_LEV_TXT") {
					this.histogram_lev_txt = parts[1]
				}
			}
		}
		computeDel()		
	}
	public void computeDel() {
		// The delta does not seem to make sense as given, so I can just compute it.
		DecimalFormat format = new DecimalFormat("###############.######");
		double min = Double.valueOf(histogram_lev_min)
		double max = Double.valueOf(histogram_lev_max)
		double num = Double.valueOf(histogram_lev_num)
		double del = ( max - min ) / num
		histogram_lev_del = format.format(del);
	}
}
