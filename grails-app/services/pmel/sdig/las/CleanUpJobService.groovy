package pmel.sdig.las


import com.agileorbit.schwartz.StatefulSchwartzJob
import grails.gorm.transactions.NotTransactional
import grails.util.Holders
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException

@CompileStatic
@Slf4j
class CleanUpJobService implements StatefulSchwartzJob {



	@NotTransactional
	void execute(JobExecutionContext context) throws JobExecutionException {

		log.info("Starting clean up of old files...")
		int old_files = 0;
		List<File> remove_dirs = new ArrayList<>()
		Ferret ferret = Ferret.first()

		def old_scripts = deleteOldFiles(new File(ferret.getTempDir()), "script", ".jnl", false)
		old_files = old_files + (int)old_scripts['old']

		def from_temp = deleteOldFiles(new File(ferret.getTempDir() + File.separator + "temp"), null, null, true)
		old_files = old_files + (int) from_temp['old']
		remove_dirs.addAll((List)from_temp['remove'])

		def from_dynamic = deleteOldFiles(new File(ferret.getTempDir() + File.separator + "dynamic"), null, null, true)
		old_files = old_files + (int) from_dynamic['old']
		remove_dirs.addAll((List)from_dynamic['remove'])

		File outputFile = Holders.grailsApplication.mainContext.getResource("output").file
		def from_output = deleteOldFiles(outputFile, null, null, false)
		old_files = old_files + (int) from_output['old']

		// Actually remove them...
		remove_dirs.each {File file ->
			file.delete()
		}
		log.info(old_files + " old files and " + remove_dirs.size() + " empty directories deleted.")
	}

	def deleteOldFiles(File temp, String start_match, String end_match, boolean directories) {
		int old_files = 0
		temp.eachFileRecurse {File file ->
			long purgeTime = System.currentTimeMillis() - (7l * 24l * 60l * 60l * 1000l);
			long modTime = file.lastModified()
			if ( start_match != null && end_match != null ) {
				if (modTime < purgeTime && file.isFile() && file.getName().startsWith(start_match) && file.getName().endsWith(end_match)) {
					file.delete();
					old_files++;
				}
			} else if ( start_match != null && end_match == null ) {
				if (modTime < purgeTime && file.isFile() && file.getName().startsWith(start_match) ) {
					file.delete();
					old_files++;
				}
			} else if (start_match == null && end_match != null ) {
				if (modTime < purgeTime && file.isFile() && file.getName().endsWith(".jnl")) {
					file.delete();
					old_files++;
				}
			} else {
				if (modTime < purgeTime && file.isFile() ) {
					file.delete();
					old_files++;
				}
			}
		}
		List<File> remove_dirs = new ArrayList<>()
		if ( directories ) {
			temp.eachFileRecurse { File file ->
				if (file.isDirectory() && file.list().length == 0) {
					remove_dirs.add(file)
				}
			}
		}
		['old': old_files, 'remove': remove_dirs]
	}
	void buildTriggers() {

		triggers <<
				factory('trigger_cleanup').
						intervalInDays(1).
						startDelay(1000 * 60 * 25). // 25 minutes
						build()
		
	}
}
