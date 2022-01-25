package pmel.sdig.las

import grails.converters.JSON

import java.util.regex.Matcher
import java.util.regex.Pattern

class VariableController {
//    static scaffold = Variable
    def json() {
        def vid = params.id
        if ( vid ) {

            def var = Variable.get(vid)
            JSON.use("deep") {
                render var as JSON
            }

        }
    }
    def oracle() {

        def lookup = []
        // This matcher and regex grabs either blank separated words or groups of works in quotes.
        // https://stackoverflow.com/questions/3366281/tokenizing-a-string-but-ignoring-delimiters-within-quotes
        String regex = "\"([^\"]*)\"|(\\S+)";

        def queryJSON = request.JSON
        SuggestQuery vq = new SuggestQuery(queryJSON)
        String query = vq.getQuery()
        String name = vq.getName();
        List<LASSuggestion> suggestions = new ArrayList<LASSuggestion>()
        Set results = []
        if (query) {

            Matcher m = Pattern.compile(regex).matcher(query);
            while (m.find()) {
                if (m.group(1) != null) {
                    lookup.add("%" + m.group(1) + "%");
                } else {
                    lookup.add("%" + m.group(2) + "%");
                }
            }

            for (int i = 0; i < lookup.size(); i++) {
                def termResults = Variable.createCriteria().list {
                    or {
                        ilike(name, lookup.get(i))
                    }
                }
                if (termResults) {
                    Set<String> values = []
                    for (int j = 0; j < termResults.size(); j++) {
                        Variable v = termResults.get(j)
                        values.add(v.properties.get(name))
                    }
                    values.each{
                        LASSuggestion t = new LASSuggestion();
                        t.setSuggestion(it)
                        results.add(t)
                    }
                }
            }
        }
        render results as JSON;
    }
}
