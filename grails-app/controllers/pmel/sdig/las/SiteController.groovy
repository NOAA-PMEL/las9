package pmel.sdig.las

import grails.converters.JSON
import org.hibernate.CacheMode
import org.hibernate.Criteria
import org.hibernate.FetchMode
import org.hibernate.FlushMode
import org.hibernate.HibernateException
import org.hibernate.LockMode
import org.hibernate.ScrollMode
import org.hibernate.ScrollableResults
import org.hibernate.criterion.Criterion
import org.hibernate.criterion.Order
import org.hibernate.criterion.Projection
import org.hibernate.sql.JoinType
import org.hibernate.transform.ResultTransformer
import pmel.sdig.las.type.GeometryType

class SiteController {

//    static scaffold = Site

    def show() {
        def sid = params.id
        try {
            sid = Long.parseLong(sid)
        } catch (Exception e) {

            sid = null
        }
        Site site
        if ( sid ) {
            site = Site.get(sid)
        } else {
            def sites = Site.withCriteria{ne('title', 'Private Data')}
            site = sites[0]
        }

        // Don't bother to compute now as we're not going to display them.

        def g = Dataset.createCriteria()
        def grids = g.get {
            eq("variableChildren", true)
            eq("geometry", GeometryType.GRID)
            projections {
                count()
            }
        }
        def traj = Dataset.createCriteria()
        def trajCount = traj.get {
            eq("variableChildren", true)
            eq("geometry", GeometryType.TRAJECTORY)
            projections {
                count()
            }
        }
        def point = Dataset.createCriteria()
        def pointCount = point.get {
            eq("variableChildren", true)
            eq("geometry", GeometryType.POINT)
            projections {
                count()
            }
        }
        def ts = Dataset.createCriteria()
        def tsCount = ts.get{
            eq("variableChildren", true)
            eq("geometry", GeometryType.TIMESERIES)
            projections {
                count()
            }
        }
        def profile = Dataset.createCriteria()
        def profileCount = profile.get{
            eq("variableChildren", true)
            eq("geometry", GeometryType.PROFILE)
            projections {
                count()
            }
        }

        // TODO trajectory profile

        // Attributes to search // TODO should read from list of searchable attributes in site domain
        Map<String, List<String>> attributes = new HashMap<String, List<String>>();

        // This got big fast with UAF.
        // Doing auto complete searches with textboxes.
        // May never use "attributes" or may have some other use for them later.
//        List<String> dataset_title = Dataset.withCriteria {
//            projections {
//                distinct("title")
//                order("title")
//            }
//        }
//        if ( dataset_title )
//            attributes.put("dataset_title", dataset_title)
//
//        List<String> variable_title = Variable.withCriteria {
//            projections {
//                distinct("title")
//                order("title")
//            }
//        }
//        if ( variable_title )
//            attributes.put("variable_title", variable_title)
//
//        List<String> standard_name = Variable.withCriteria {
//            projections {
//                distinct("standard_name")
//                order("standard_name")
//            }
//        }
//
//        if ( standard_name )
//            attributes.put("standard_name", standard_name)

        site.setAttributes(attributes)

        def discrete = trajCount + pointCount + tsCount + profileCount

        def total = grids + discrete
        site.setTotal(total)
        site.setGrids(grids)
        site.setTrajectory(trajCount)
        site.setPoint(pointCount)
        site.setTimeseries(tsCount)
        site.setProfile(profileCount)
        site.setDiscrete(discrete)



        if ( site ) {
            withFormat {
                html { respond site }
                json { respond site}
            }
        } else {
            log.error("No site found for this installation.")
        }
    }
}
