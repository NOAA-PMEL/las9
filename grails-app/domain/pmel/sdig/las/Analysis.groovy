package pmel.sdig.las

class Analysis {
    String transformation
    String axes
    String over
    List analysisAxes
    static hasMany = [analysisAxes: AnalysisAxis]
    String hash() {
        String hash = transformation+"_"+axes+"_"
        for (int i = 0; i < analysisAxes.size(); i++) {
            AnalysisAxis a = analysisAxes.get(i)
            hash = hash + a.getType()+"_"+a.getLo()+"_"+a.getHi()
        }
        JDOMUtils.MD5Encode(hash)
    }
}
