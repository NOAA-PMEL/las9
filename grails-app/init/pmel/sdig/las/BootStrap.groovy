package pmel.sdig.las

import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional
import grails.util.Holders
import org.apache.shiro.authc.credential.PasswordService
import org.hibernate.sql.Update
import pmel.sdig.las.*
class BootStrap {

    InitializationService initializationService
    PasswordService credentialMatcher
    GrailsApplication grailsApplication
    UpdateDatasetJobService updateDatasetJobService;

    def init = { servletContext ->
        def v = Holders.grailsApplication.metadata['app.version']

        log.debug("Starting the init bootstrap closure...")

        // Always init the environment so it can be changed
        // between restarts.
        initializationService.initEnvironment()


        // These 3 methods check to see if the objects exist before creating them.
        // Unless reinit is set to true
        def reinit = false;
        def property = grailsApplication.config.getProperty('admin.reinit')
        if ( property ) {
            reinit = property == "true"
        }

        // Pass in reinit = true to remake products and regions
        initializationService.createProducts(reinit)

        initializationService.createDefaultRegions(reinit)

        // Not part of reinit, handled by admin interface
        initializationService.loadDefaultLasDatasets()

        def priv = Site.findByTitle("Private Data")
        if ( !priv ) {
            priv = new Site([title: "Private Data"])
            priv.save();
        }

        def admin_pw = grailsApplication.config.getProperty('admin.password')
        def adminUser = ShiroUser.findByUsername('admin')

        if (!adminUser) {
            ShiroUser.withTransaction {
                def adminRole = new ShiroRole(name: "Admin")
                if (admin_pw) {
                    adminUser = new ShiroUser(username: "admin", passwordHash: credentialMatcher.encryptPassword(admin_pw))
                } else {
                    adminUser = new ShiroUser(username: "admin", passwordHash: credentialMatcher.encryptPassword('default'))
                }
                adminRole.addToPermissions("admin:*")
                adminUser.addToRoles(adminRole)
                if (!adminUser.validate()) {
                    adminUser.errors.each {
                        print(it)
                    }
                }
                adminUser.save(flush: true, failOnError: true)
            }
        } else if ( admin_pw ) {
            ShiroUser.withTransaction {
                adminUser.setPasswordHash(credentialMatcher.encryptPassword(admin_pw))
                adminUser.save()
            }
        }

        List<Dataset> updates = Dataset.createCriteria().listDistinct {
            datasetProperties {
                eq("type", "update")
            }
        }
        for (int i = 0; i < updates.size(); i++) {
            Dataset d = updates.get(i);
            def ds_properties = d.getDatasetProperties();
            Iterator<DatasetProperty> it = ds_properties.iterator();
            while ( it.hasNext() ) {
                DatasetProperty p = it.next();
                if ( p.getType() == "update" && !p.getValue().isEmpty()) {
                    updateDatasetJobService.addDatasetUpdate(d.id, p.getValue())
                }
            }
        }

    }
    def destroy = {
    }
}
