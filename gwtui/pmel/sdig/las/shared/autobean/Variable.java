package pmel.sdig.las.shared.autobean;

import java.util.Iterator;
import java.util.Set;

public class Variable implements Comparable {

	long id;
	String url;
	String name;
	String title;
	String hash;
	String intervals;
	String geometry;
	String units;
	Dataset dataset;
	String type;
	Stats stats;
	Set<VariableProperty> variableProperties;
	String thumbnail;
	boolean subset;
	boolean dsgId;

	public boolean isDsgId() {
		return dsgId;
	}

	public void setDsgId(boolean dsgId) {
		this.dsgId = dsgId;
	}

	public boolean isSubset() {
		return subset;
	}

	public void setSubset(boolean subset) {
		this.subset = subset;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setGeometry (String geometry) { this.geometry = geometry; }

	public String getGeometry () {
		return this.geometry;
	}

	public String getHash() {
		return hash;
	}

	public void setHash(String hash) {
		this.hash = hash;
	}

	public Dataset getDataset() {
		return dataset;
	}

	public void setDataset(Dataset dataset) {
		this.dataset = dataset;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Stats getStats() {
		return stats;
	}

	public void setStats(Stats stats) {
		this.stats = stats;
	}

	public String getIntervals() {
		return intervals;
	}

	public void setIntervals(String intervals) {
		this.intervals = intervals;
	}

	public Set<VariableProperty> getVariableProperties() {
		return variableProperties;
	}

	public void setVariableProperties(Set<VariableProperty> variableProperties) {
		this.variableProperties = variableProperties;
	}

	public void setUnits(String units) {
		this.units = units;
	}

	public String getUnits() {
		return this.units;
	}

	public String getThumbnail() {
		return thumbnail;
	}

	public void setThumbnail(String thumbnail) {
		this.thumbnail = thumbnail;
	}

	public String getProperty(String type, String name) {
	    if ( variableProperties != null ) {
            for(Iterator vit = variableProperties.iterator(); vit.hasNext(); ) {
                VariableProperty vp = (VariableProperty) vit.next();
                if ( vp.getType().equals(type) && vp.getName().equals(name) ) {
                    return vp.getValue();
                }
            }
        }
        return null;
    }

	@Override
	public int compareTo(Object o) {
		if ( o instanceof Variable ) {
			Variable v = (Variable) o;
			return this.getTitle().compareTo(v.getTitle());
		} else {
			return 0;
		}
	}

}
