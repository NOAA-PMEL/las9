package pmel.sdig.las

import grails.async.Promise

import static grails.async.Promises.*
import grails.converters.JSON
import grails.gorm.transactions.Transactional

import java.util.regex.Matcher
import java.util.regex.Pattern

class DatasetController {

//    static scaffold = Dataset
    IngestService ingestService
    @Transactional
    def add() {
        String url = params.url
        if ( url ) {
            def dataset = Dataset.findByUrl(url);
            if ( !dataset ) {
                ingestService.ingest(null, url)
                dataset = Dataset.findByUrl(url)
                if ( dataset ) {
                    // request.message and reqeust.info appear to be a reserved key... using my own key...
                    request.data_ingest_info = "Dataset ingested."

                } else {
                    request.data_ingest_info = "Unable to ingest dataset."
                }
            } else {
                request.data_ingest_info = url+"has already been ingested into this server."
            }
            [datasetInstance:dataset]
        }
    }
    def show() {
        def did = params.id
        def dataset
        if ( did ) {
            try {
                // If it passes valueOf, use it as a GORM ID
                long id = Long.valueOf(did)
                dataset = Dataset.get(did)
            } catch (Exception e ) {
                // If not, it's a hash
                dataset = Dataset.findByHash(did)
            }

            if ( dataset.variableChildren && dataset.getStatus().equals(Dataset.INGEST_NOT_STARTED) ) {
                dataset.setStatus(Dataset.INGEST_STARTED)
                dataset.setMessage("This data set has not been ingested by LAS. That process has been started. This may take a while, but you can click again anytime to see if it's finished.")
                Dataset.withNewTransaction {
                    dataset.save(flush: true)
                }
                log.debug("Adding variables to " + dataset.getUrl() + " which has variableChildren = " + dataset.variableChildren)
                Promise p = task {
                    ingestService.addVariablesAndSaveFromThredds(dataset.getUrl(), dataset.getHash(), null, true)
                }
                p.onError { Throwable err ->
                    log.error("An error occured ${err.message}")
                }
                p.onComplete { result ->
                    log.info('Finished ingesting requested data set.')
                }

            } else if ( dataset.variableChildren && (dataset.getStatus().equals(Dataset.INGEST_STARTED) ) ) {
                IngestStatus ingestStatus = IngestStatus.findByHash(dataset.getHash())
                String message = "This the process of ingesting this data set has been started. This may take a while, but you can check back anytime to see if it's finished."
                if (ingestStatus) {
                    message = ingestStatus.getMessage()
                }
                dataset.setMessage(message)
            }
        }
        withFormat {
            html { respond dataset }
            json {
                log.debug("Starting response for dataset " + dataset.id)
                respond dataset // uses the custom templates in views/dataset
            }
        }
    }
    def browse() {
        def browseDatasets = []

        def offset = params.offset
        // If no offset is specified send only the first
        if ( !offset ) {
            offset = 0
            def dlist = Dataset.findAllByVariableChildren(true, [offset: offset, max: 1])
            browseDatasets.add(dlist.get(0))


        } else {
            // Send back the next 10
            browseDatasets = Dataset.findAllByVariableChildren(true, [offset: offset, max: 10])
        }
        log.debug("Starting response for dataset list with " + browseDatasets.size() + " members.")
        render(template: "browse",  model: [datasetList: browseDatasets])
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
                def termResults = Dataset.createCriteria().list {
                    or {
                        ilike(name, lookup.get(i))
                    }
                    and {
                        eq("variableChildren", true)
                    }

                }
                if (termResults) {
                    for (int j = 0; j < termResults.size(); j++) {
                        Dataset d = termResults.get(j)
                        LASSuggestion t = new LASSuggestion();
                        t.setSuggestion(d.getTitle())
                        results.add(t)
                    }
                }
            }
        }
        render results as JSON;
    }
// For debugging the update service method
//    def update() {
//        def id = params.id
//        long did = Long.valueOf(id).longValue()
//        ingestService.updateTime(did)
//    }

}
