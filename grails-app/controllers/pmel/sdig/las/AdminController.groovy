package pmel.sdig.las


import grails.converters.JSON
import grails.gorm.transactions.NotTransactional
import grails.gorm.transactions.Transactional
import groovy.json.JsonSlurper
import org.apache.commons.io.FileUtils
import org.grails.web.json.JSONArray
import org.grails.web.json.JSONObject

import java.text.SimpleDateFormat

@Transactional
class AdminController {
    IngestService ingestService
    UpdateDatasetJobService updateDatasetJobService;
    ReadMetadataJobService readMetadataJobService;
    LASProxy lasProxy = new LASProxy();
    def jsonSlurper = new JsonSlurper()
    def index() {

    }
    def signOut() {
        redirect(uri: '/auth/signOut')
    }
    def saveSite() {
        def requestJSON = request.JSON
        def map = requestJSON as Map
        def sites = Site.withCriteria{ne('title', 'Private Data')}
        def site = sites[0]
        site.properties = map;
        site.save(flush: true, failOnError: true);
        JSON.use("deep") {
            render site as JSON
        }
    }
    def status() {
        List<Dataset> failedDatasets = Dataset.withCriteria{
            eq("variableChildren", true)
            eq("status", Dataset.INGEST_FAILED)
            isEmpty("variables")
        }
        render view: "status", model: [failedDatasets: failedDatasets]
    }
    def resetFailed() {
        List<Dataset> failedDatasets = Dataset.withCriteria{
            eq("variableChildren", true)
            eq("status", Dataset.INGEST_FAILED)
            isEmpty("variables")
        }
        failedDatasets.each {Dataset d ->
            d.setStatus(Dataset.INGEST_NOT_STARTED)
            d.save(flush: true)
        }
        render view: "status", model: [failedDatasets: failedDatasets]
    }
    def saveDatasetUpdateSpec() {
        def requestJSON = request.JSON
        def map = requestJSON as Map;
        /*
         This payload is a map with the following keys:
                dataset - is a the ID of the data set to be modified.
                property - is a object that looks like a DatasetProperty that should be of the form:
                type: "update"
                name: "cron"
                value: some string of the cron spec for frequency of updates e.g. "30 * * * *" thirty past the hour, every hour
         */
        def id = map.get("dataset")
        def cron_spec = map.get("cron_spec")

        Dataset d = Dataset.get(id)
        def done = false;
        if ( d.datasetProperties ) {
            Iterator<DatasetProperty> it = d.datasetProperties.iterator()
            while (it.hasNext()) {
                def p = it.next()
                if ( p.type == "update") {
                    done = true
                    p.type = "update"
                    p.name = "cron_spec"
                    p.value = cron_spec
                    p.save(flush: true)
                    d.save(flush: true)
                    if ( !p.value.isEmpty() ) {
                        updateDatasetJobService.addDatasetUpdate(d.id, p.value)
                    } else {
                        updateDatasetJobService.unscheuleUpdate(d.id)
                    }
                }
            }
        }
        if ( !done ) {
            DatasetProperty p = new DatasetProperty()
            p.type = "update"
            p.name = "cron_spec"
            p.value = cron_spec
            d.addToDatasetProperties(p)
            d.save(flush: true)
            if ( !p.value.isEmpty() ) {
                updateDatasetJobService.addDatasetUpdate(d.id, p.value)
            } else {
                updateDatasetJobService.unscheuleUpdate(d.id)
            }
        }

        def parent
        if ( d.parent == null ) {
            parent = Site.get(1)
        } else {
            parent = Dataset.get(d.parent.id);
        }
        JSON.use("deep") {
            render parent as JSON
        }
    }
    def saveDataset() {
        def requestJSON = request.JSON
        def map = requestJSON as Map;

        /*
         This payload is a map with the following keys:
                dataset - is a map of the changes primitive properties (string and numbers) it should include the
                          which is needed to get the data set to modify
                variables - is a map of maps, each map modifies one of the variables in this data set. id is included
                            as they outer key
                geoAxisX - similar to variables as a map of maps where the key of the outer map is the id
                geoAxisY - same as X
                verticalAxis - same as X
                timeAxis - same as X
         */

        def parent

        // Should only be at most one data set
        if ( map.has("dataset") ) {
            def datasetChanges = map.get("dataset")
            def id = datasetChanges.id
            log.debug("Saving a change for data set " + id)
            Dataset d = Dataset.get(id)
            d.properties = datasetChanges;
            d.save(failOnError: true)
            if ( d.parent == null ) {
                parent = Site.get(1)
            } else {
                parent = Dataset.get(d.parent.id);
            }
        }


        if ( map.has("variables") ) {
            def variablesToChange = map.get("variables")
            variablesToChange.each {
                def vid = it.key
                def vproperties = it.value
                Variable v = Variable.get(vid)
                v.properties = vproperties
                v.save(flush: true, failOnError: true)
            }
        }

        if ( map.has("geoAxisX") ) {
            def geoAxisXToChange = map.get("geoAxisX")
            geoAxisXToChange.each {
                def xid = it.key
                def xproperties = it.value
                GeoAxisX x = GeoAxisX.get(xid)
                x.properties = xproperties
                x.save(flush: true, failOnError: true)
            }
        }



        if ( map.has("geoAxisY") ) {
            def geoAxisYToChange = map.get("geoAxisY")
            geoAxisYToChange.each {
                def yid = it.key
                def yproperties = it.value
                GeoAxisY y = GeoAxisY.get(yid)
                y.properties = yproperties
                y.save(flush: true, failOnError: true)
            }
        }

        if ( map.has("verticalAxis") ) {
            def verticalAxisToChange = map.get("verticalAxis")
            verticalAxisToChange.each {
                def zid = it.key
                def zproperties = it.value
                VerticalAxis z = VerticalAxis.get(zid)
                z.properties = zproperties
                z.save(flush: true, failOnError: true)
            }
        }

        if ( map.has("timeAxis") ) {
            def timeAxisToChange = map.get("timeAxis")
            timeAxisToChange.each {
                def tid = it.key
                def tproperties = it.value
                TimeAxis t = TimeAxis.get(tid)
                t.properties = tproperties
                t.save(flush: true, failOnError: true)
            }
        }

        if ( !parent ) parent = Site.get(1);
        JSON.use("deep") {
            render parent as JSON
        }
    }
    def moveDataset() {
        def requestJSON = request.JSON

        AddRequest addReq = new AddRequest(requestJSON)

        def move_to_id
        def move_from_id
        def move_to_type

        List<AddProperty> props = addReq.getAddProperties()
        for (int i = 0; i < props.size(); i++) {
            AddProperty property = props.get(i)
            if (property.name == "move_to_id") {
                move_to_id = property.value;
            } else if (property.name == "move_from_id") {
                move_from_id = property.value;
            } else if ( property.name == 'move_to_type' ) {
                move_to_type = property.value
            }
        }
        if ( move_to_id && move_from_id ) {
            def destination
            if ( move_to_type == 'site' ) {
                destination = Site.get(move_to_id)
            } else {
                destination = Dataset.get(move_to_id)
            }

            def move = Dataset.get(move_from_id);
            if ( destination && move ) {
                def move_from_parent
                if ( addReq.getType() == 'show' ) {
                    // The the moment all hidden data sets are just stashed in a second site...
                    move_from_parent = Site.get(2)
                } else {
                    move_from_parent = move.getParent();
                    // If parent is null, parent is site
                    if (!move_from_parent) {
                        move_from_parent = Site.get(1)
                    }
                }

                move_from_parent.removeFromDatasets(move);
                if ( move_to_type == 'site') move.setParent(null)
                destination.addToDatasets(move)
                destination.save(flush: true, failOnError: true)
                move_from_parent.save(flush: true, failOnError: true)

                def both
                if ( addReq.getType() == 'show') {
                    both = [destination: move_from_parent, origin: destination]
                } else {
                    both = [destination: destination, origin: move_from_parent]
                }
                JSON.use("deep") {
                    render both as JSON
                }
            }
        }
        //TODO render error
    }
    def addVectors() {
        def did = params.id
        Dataset dataset = Dataset.get(did)
        ingestService.addVectors(dataset)
        JSON.use('deep') {
            render dataset as JSON
        }
    }
    def addUAF() {
        AddRequest addRequest = new AddRequest()
        def turl = "https://data.pmel.noaa.gov/uaf/thredds/CleanCatalog.xml"
//        def turl = "https://data.pmel.noaa.gov/uaf/thredds/CleanCatalogs/psl.noaa.gov/thredds/catalog/Datasets/20thC_ReanV2c/Monthlies/gaussian_sprd/subsurface/catalog.xml"
        turl = "https://data.pmel.noaa.gov/uaf/thredds/CleanCatalogs/oceanwatch.pfeg.noaa.gov/thredds/catalog/FNMOC/NAVGEM/onedegree/10mWindU/catalog.xml"
        addRequest.setUrl(turl)
        addRequest.setType("thredds")
        def addprops = []
        AddProperty ap1 = new AddProperty()
        ap1.setName("parent_type")
        ap1.setValue("site")
        AddProperty ap2 = new AddProperty()
        ap2.setName("parent_id")
        ap2.setValue("1")
        addprops.add(ap1)
        addprops.add(ap2)
        def parent = Site.get(1)
        addRequest.setAddProperties(addprops)
        Dataset dataset = ingestService.processRequest(addRequest, parent);
        parent.addToDatasets(dataset)
        parent.save(flush: true, failOnError: true);
        log.info("Finished UAF ingest, Site saved");
    }
    def testGriddapURL() {
        String url = params.url
        if (url == null)
            url = "https://upwell.pfeg.noaa.gov/erddap/griddap/noaa_psl_faa8_a0e9_a4c8"
        Dataset dataset = ingestService.datasetFromGriddapInfo(url)
        def parent = Site.get(1)
        if ( dataset != null )
            parent.addToDatasets(dataset)
        parent.save(flush: true, failOnError: true)
        log.info("Finished test ingest from " + url)
    }
    @NotTransactional
    def addDataset() {

        def requestJSON = request.JSON

        AddRequest addReq = new AddRequest(requestJSON)
        List<AddProperty> props = addReq.getAddProperties()

        def parent_type;
        def parent_id;
        def empty_name;
        def empty_inst_regex;
        boolean use_source_url = false;

        def parent;

        for (int i = 0; i < props.size(); i++) {
            AddProperty property = props.get(i)
            if ( property.name == "parent_type") {
                parent_type = property.value;
            } else if ( property.name == "parent_id" ) {
                parent_id = property.value;
            } else if ( property.name == "name" ) {
                empty_name = property.value;
            } else if ( property.name == "inst_regex") {
                empty_inst_regex = property.value
            } else if ( property.name == "use_source_url") {
                use_source_url = Boolean.valueOf("use_source_url").booleanValue()
            }
        }

        if ( parent_type == "site" ) {
            parent = Site.get(parent_id);
        } else if ( parent_type == "dataset" ) {
            parent = Dataset.get(parent_id);
        }

        if ( addReq.url && addReq.type ) {

            /*
            So I crammed the ability to use a search URL to add a bunch of ERDDAP
            data sets to a particular parent. Instead of changing the stuff downstream
            I'm going make it happen right here.
             */

            if ( addReq.url.contains("erddap/search") ) {
                def surl = addReq.url;
                surl = surl.replace(".html", ".json")
                String json = lasProxy.executeGetMethodAndReturnResult(surl)
                JSONObject list =  JSON.parse(json)
                JSONArray rows = list.get("table").get("rows")
                for (int i = 0; i < rows.size(); i++) {
                    JSONArray row = rows.get(i)
                    def durl;
                    AddRequest searchResultRequest;
                    if ( addReq.getType().equals("tabledap") ) {
                        durl = row.getString(2)
                        searchResultRequest = new AddRequest(addReq.properties) // Make a copy
                        searchResultRequest.setUrl(durl)
                        searchResultRequest.setType("dsg")
                    } else {
                        durl = row.getString(0)
                        searchResultRequest = new AddRequest(addReq.properties) // Make a copy
                        searchResultRequest.setUrl(durl)
                        searchResultRequest.setType("griddap")
                    }
                    Dataset dataset = ingestService.processRequest(searchResultRequest, parent);
                    if ( dataset != null ) {
                        if (dataset.status == Dataset.INGEST_FAILED) {
                            JSON.use("deep") {
                                render dataset as JSON
                            }
                        }

                        dataset.setStatus(Dataset.INGEST_FINISHED)
                        if (!dataset.validate()) {
                            dataset.errors.each {
                                log.debug(it.toString())
                            }
                        }

                        parent.addToDatasets(dataset)
                    }
                }
            } else {
                if (addReq.getType().equals("griddap")) {
                    ingestService.griddapDirect(addReq.getUrl(), parent, use_source_url)
                } else {

                    Dataset dataset = ingestService.processRequest(addReq, parent);
                    if (dataset.status == Dataset.INGEST_FAILED) {
                        JSON.use("deep") {
                            render dataset as JSON
                        }
                    }

                    dataset.setStatus(Dataset.INGEST_FINISHED)
                    if (!dataset.validate()) {
                        dataset.errors.each {
                            log.debug(it.toString())
                        }
                    }

                    parent.addToDatasets(dataset)
                }
            }

        } else if ( addReq.type && addReq.type == "empty" ) {
            Dataset newd = new Dataset([title: empty_name, hash: IngestService.getDigest(empty_name)])
            parent.addToDatasets(newd)
        }
        if ( parent instanceof Site) {
            Site.withTransaction {
                parent.save(flush: true, failOnError: true);
            }
        } else if ( parent instanceof Dataset) {
            Dataset.withTransaction {
                parent.save(flush: true, failOnError: true);
            }
        }

        if ( parent instanceof Site ) {
            withFormat {
                json {respond parent}
            }
        } else {
            JSON.use("deep") {
                render parent as JSON
            }
        }

    }
    def deleteVariable() {
        def vid = params.id
        def parent;
        Variable v = Variable.get(vid)
        Dataset d = v.getDataset()
        List<Vector> vectors = d.getVectors();
        if ( vectors ) {
            for (int i = 0; i < vectors.size(); i++) {
                Vector vc = vectors.get(i)
                Variable u = vc.getU()
                Variable vv = vc.getV()
                Variable w = vc.getW()
                if ( (u && u.id == v.id) || ( vv && vv.id == v.id ) || w && w.id == v.id ) {
                    def message = 'The variable ' + v.getTitle() + ' is part of the vector ' + vc.getTitle() + '. Delete the vector first.'
                    response.sendError(500, message)
                }
            }
        }
        d.removeFromVariables(v)
        v.delete(flush: true)
        d.save(flush: true)
        JSON.use("deep") {
            render d as JSON
        }
    }
    def deleteVector() {
        def vid = params.id
        def parent;
        Vector v = Vector.get(vid)
        v.setU(null)
        v.setV(null)
        v.setW(null)
        Dataset d = v.getDataset()
        d.removeFromVectors(v)
        v.delete(flush: true)
        d.save(flush: true)
        JSON.use("deep") {
            render d as JSON
        }
    }
    def deleteDataset() {
        def did = params.id
        def parent;
        if ( did ) {
            Dataset dead = Dataset.get(did)
            log.debug("Found data set to remove: " + dead.getHash())
            if (!dead.parent) {
                parent = Site.get(1)
                parent.removeFromDatasets(dead)
                log.debug("Removing data set from site and saving")
                parent.save(flush: true)
                log.debug("Site saved")
            } else {
                parent = dead.getParent()
                log.debug("Removing data set " + dead.id + "from parent and saving")
                parent.removeFromDatasets(dead)
                parent.save(flush: true)
                log.debug("Parent " + parent.id + " saved.")
            }
            updateDatasetJobService.unscheuleUpdate(dead.id)
            log.debug("Deleting data set." + dead.id)
            dead.delete(flush: true)
            log.debug("Dataset deleted.")
        }
        JSON.use("deep") {
            log.debug("Rendering parent response for deleted data set.")
            render parent as JSON
        }
    }
    def start() {
        readMetadataJobService.buildTriggers()
    }
    def stop() {
        readMetadataJobService.unscheuleUpdate()
    }
    def ferret() {
        Ferret ferret = Ferret.first()
        if ( ferret ) {
            JSON.use("deep") {
                render ferret as JSON
            }
        } else {
            log.error("No site found for this installation.")
        }
    }
    def listBackups() {
        def backups = backup_list();
        render backups as JSON
    }
    private def backup_list() {
        def backups = []
        Ferret ferret = Ferret.first()
        def tmp = ferret.tempDir;
        def backupDir = new File(tmp + File.separator + "backups")
        if ( backupDir.exists() ) {
            backupDir.eachFile { file ->
                if (file.isDirectory()) {
                    Backup b = new Backup();
                    b.setDirectory(file.getAbsolutePath())
                    backups.add(b)
                }
            }
        }
        backups
    }
    def deleteBackup() {
        def requestJSON = request.JSON
        Backup bkup = new Backup(requestJSON)
        def f = bkup.getDirectory()
        Ferret ferret = Ferret.first()
        def tmp = ferret.tempDir;
        def backupDir
        if ( f.startsWith("/") ) {
            backupDir = new File(f)
        } else {
            backupDir = new File(tmp + File.separator + "backups" + File.separator + f)
        }
        if ( !backupDir.exists() ) {
            log.debug("Could not find backup directory." + backupDir.getAbsolutePath())
        } else {
            FileUtils.deleteDirectory(backupDir)
        }
        def backups = backup_list()
        render backups as JSON
    }
    def restore() {
        def requestJSON = request.JSON
        Backup bkup = new Backup(requestJSON)
        def f = bkup.getDirectory()
        Ferret ferret = Ferret.first()
        def tmp = ferret.tempDir;
        def backupDir
        if ( f.startsWith("/") ) {
            backupDir = new File(f)
        } else {
            backupDir = new File(tmp + File.separator + "backups" + File.separator + f)
        }
        if ( !backupDir.exists() ) {
            log.debug("Could not find backup directory." + backupDir.getAbsolutePath())
        } else {
            def sites = Site.withCriteria{ne('title', 'Private Data')}
            Site site = sites[0]
            Site nsite
            backupDir.eachFile { file ->
                if (file.isFile() && file.getName().contains("site_")) {
                    def siteJsonObj = jsonSlurper.parse(file)
                    removeIds(siteJsonObj)
                    nsite = new Site(siteJsonObj)
                    nsite.footerLinks = null;
                    nsite.datasets = null;
                }
            }
            // Find datasets and see which ones have children
            backupDir.eachFile { file ->
                if ( file.isDirectory() && file.getName().contains("dataset_")) {
                    def dsJsonFile = new File(file.getAbsolutePath() + File.separator + "dataset.json")
                    def json = jsonSlurper.parse(dsJsonFile)
                    removeIds(json)
                    Dataset ds = new Dataset(json)
                    ds.datasets = null;
                    nsite.addToDatasets(ds)
                    ds.save(flush: true);
                    log.debug("Added top level data set: " + file.getName())
                    addChildren(ds, file)
                } else if (file.isFile() && file.getName().contains("link_") ) {
                    def json = jsonSlurper.parse(file)
                    removeIds(json)
                    FooterLink footerLink = new FooterLink(json)
                    nsite.addToFooterLinks(footerLink)
                }
            }
            if ( !nsite.validate() ) {
                log.debug("Site read from disk did not validate.")
                nsite.errors.allErrors.each {
                    println it
                }
            } else {
                if ( site ) site.delete(flush: true)
                nsite.save(flush: true)
            }
        }
    }
    private def removeIds(def jsonObject) {
        if ( jsonObject instanceof Map ) {
            if ( jsonObject.containsKey("id") ) {
                jsonObject.remove('id')
            }
            jsonObject.keySet().each {
                removeIds(jsonObject.get(it))
            }
        } else if ( jsonObject instanceof Collection) {
            jsonObject.each {
                removeIds(it)
            }
        }
    }
    private def addChildren(Dataset dataset, File datasetDirectory) {
        datasetDirectory.eachFile { file ->
            if ( file.isDirectory() && file.getName().contains("dataset_") ) {
                def dsJsonFile = new File(file.getAbsolutePath() + File.separator + "dataset.json")
                def json = jsonSlurper.parse(dsJsonFile)
                removeIds(json)
                Dataset ds = new Dataset(json)
                if ( ds.getVariableChildren() ) {
                    ds.variables = null;
                    ds.vectors = null; // TODO save and add these
                    processVariables(ds, file)
                } else {
                    ds.datasets = null;
                }
                dataset.addToDatasets(ds)
                ds.save(flush: true)
                addChildren(ds, file)
            }
        }
    }
    private def processVariables(Dataset dataset, File datasetDirectory) {
        datasetDirectory.eachFile { file ->
            if ( file.isFile() && file.getName().contains("variable_") ) {
                def json = jsonSlurper.parse(file)
                removeIds(json)
                Variable variable = new Variable(json)
                if ( variable.getGeoAxisX() ) {
                    variable.getGeoAxisX().setVariable(variable)
                }
                if ( variable.getGeoAxisY() ) {
                    variable.getGeoAxisY().setVariable(variable)
                }
                if ( variable.getTimeAxis() ) {
                    variable.getTimeAxis().setVariable(variable)
                }
                if ( variable.getVerticalAxis() ) {
                    variable.getVerticalAxis().setVariable(variable)
                }
                dataset.addToVariables(variable)
                variable.setDataset(dataset)
                variable.save(flush: true)
            }
        }
    }
    def backup() {
        def backups = backup_list();
        def sites = Site.withCriteria{ne('title', 'Private Data')}
        Site site = sites[0]
        Ferret ferret = Ferret.first()
        def tmp = ferret.tempDir;
        def backupDir = new File(tmp + File.separator + "backups" + File.separator + "db-backup-" + new SimpleDateFormat("yyyy-MM-dd-HHmm").format(new Date()))
        if ( !backupDir.exists() ) backupDir.mkdirs()
        def siteFile = new File(backupDir.getAbsolutePath() + File.separator + "site_" + site.id + ".json")
        def siteJson = site as JSON
        siteFile.write(siteJson.toString(true))
        List<FooterLink> footers = site.getFooterLinks()
        if ( footers ) {
            footers.each { FooterLink link ->
                def linkJson = link as JSON
                def linkFile = new File(backupDir.getAbsolutePath() + File.separator + "link_" + link.id + ".json")
                linkFile.write(linkJson.toString(true))
            }
        }
        Map<String, List<String>> attributes = site.getAttributes()
        if ( attributes ) {
            def attrJson = attributes as JSON
            def attrFile = new File(backupDir.getAbsolutePath() + File.separator + "attributes.json")
            attrFile.write(attrJson.toString(true))
        }
        def properties = site.getSiteProperties()
        if ( properties ) {
            properties.each{ SiteProperty prop ->
                def propJson = prop as JSON
                def propFile = new File(backupDir.getAbsolutePath() + File.separator + "prop_" + prop.id + ".json")
                propFile.write(propJson.toString(true))
            }
        }

        def datasets = site.getDatasets()
        if ( datasets ) {
            backupDatasetTree(datasets, backupDir.getAbsolutePath())
        }

        def ferretFile = new File(backupDir.getAbsolutePath() + File.separator + "ferret_" + ferret.id + ".json")
        JSON.use("deep") {
            def ferretJson = ferret as JSON
            ferretFile.write(ferretJson.toString(true))
        }
        Backup backup = new Backup(['directory':backupDir, 'highlight': true])
        backups.add(backup)
        render backups as JSON
    }
    private def backupDatasetTree(def datasets, String backupDir){
        datasets.each { Dataset dataset ->
            backupDataset(dataset, backupDir)
            def children = dataset.getDatasets();
            if ( children ) {
                backupDatasetTree(children, backupDir + File.separator + "dataset_"+dataset.id)
            }
        }
    }
    private def backupDataset(Dataset dataset, String backupDir) {
        def datasetJson = dataset as JSON
        def datasetDir = new File(backupDir + File.separator + "dataset_" + dataset.id)
        if (!datasetDir.exists()) {
            datasetDir.mkdirs();
        }
        def datasetFile = new File(backupDir + File.separator + "dataset_" + dataset.id + File.separator + "dataset.json")
        datasetFile.write(datasetJson.toString(true))
        // Write the member domain objects separately
        if (dataset.datasetProperties) {

            dataset.datasetProperties.each { DatasetProperty dp ->

                def dpJson = dp as JSON
                def dpFile = new File(backupDir + File.separator + "dataset_" + dataset.id + File.separator + "dataset_property_" + dp.id + ".json")
                dpFile.write(dpJson.toString(true))
            }
        }
        if ( dataset.variableChildren ) {
            // A variable is the most complicated hierarchy, but it only ever goes one level deep so this should work fine
            dataset.variables.each {Variable variable ->
                def variableJson
                JSON.use("deep") {
                    variableJson = variable as JSON
                    // Don't need the either of these and this keeps them out of the toString !!! Sweet
                    variableJson.excludes = ['parent', 'dataset']
                }
                def varFile = new File(backupDir + File.separator + "dataset_" + dataset.id + File.separator + "variable_" + variable.id + ".json")
                if ( variableJson )
                    varFile.write(variableJson.toString(true))
            }
        }
    }
}
