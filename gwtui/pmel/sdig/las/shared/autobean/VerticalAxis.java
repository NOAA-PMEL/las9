package pmel.sdig.las.shared.autobean;

import java.util.List;

public class VerticalAxis {

	String type;
	String units;
	String name;
	String positive;
	String title;
	String regular;
	double min;
	double max;
	double size;
	List<Zvalue> zvalues;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getUnits() {
		return units;
	}

	public void setUnits(String units) {
		this.units = units;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPositive() {
		return positive;
	}

	public void setPositive(String positive) {
		this.positive = positive;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getRegular() {
		return regular;
	}

	public void setRegular(String regular) {
		this.regular = regular;
	}

	public double getMin() {
		return min;
	}

	public void setMin(double min) {
		this.min = min;
	}

	public double getMax() {
		return max;
	}

	public void setMax(double max) {
		this.max = max;
	}

	public double getSize() {
		return size;
	}

	public void setSize(double size) {
		this.size = size;
	}

	public List<Zvalue> getZvalues() {
		return zvalues;
	}

	public void setZvalues(List<Zvalue> zvalues) {
		this.zvalues = zvalues;
	}
}