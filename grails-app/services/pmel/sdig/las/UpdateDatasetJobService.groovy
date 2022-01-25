package pmel.sdig.las


import com.agileorbit.schwartz.StatefulSchwartzJob
import com.agileorbit.schwartz.util.QuartzSchedulerObjects
import grails.gorm.transactions.NotTransactional
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.quartz.JobDataMap
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import org.quartz.Scheduler
import org.quartz.TriggerKey
import org.springframework.beans.factory.annotation.Autowired

@CompileStatic
@Slf4j
class UpdateDatasetJobService implements StatefulSchwartzJob {

	IngestService ingestService
	@Autowired protected Scheduler quartzScheduler

	@NotTransactional
	void execute(JobExecutionContext context) throws JobExecutionException {
		JobDataMap jobDataMap = context.mergedJobDataMap
		def id = jobDataMap.getLong('id')
		try {
			ingestService.updateTime(id)
		} catch (Exception e) {
			log.error("Exception during update of data set ${id} with message ${e.getMessage()}" )
		}
	}

	void addDatasetUpdate(long id, String cron_spec) {
		try {
			JobDataMap jobDataMap = new JobDataMap()
			jobDataMap.put("id", id)
			def trigger_name = "Dataset " + id;
			TriggerKey key = new TriggerKey(String.valueOf(id), "dataset");
			def trigger = factory(trigger_name)
					.jobDataMap(jobDataMap)
					.key(key)
					.cronSchedule(cron_spec)
					.build()
			schedule(trigger)
			log.info("Setting up cron-based updates for data set id=" + id + " for cron spec "+ cron_spec)
		} catch (Exception e ) {
			log.error("Update job setup failed. " + e.getMessage())
		}

	}

	void unscheuleUpdate(long id) {
		log.info("Stopped updates for data set id=" + id)
		TriggerKey key = new TriggerKey(String.valueOf(id), "dataset")
		quartzScheduler.unscheduleJob(key)
	}

	void buildTriggers() {

//		triggers <<
//				factory('trigger_metadata').
//				intervalInMinutes(15).
//				startDelay(1000*60*5).  // 5 minutes
//				build()


		// triggers << factory('cron every second').cronSchedule('0/1 * * * * ?').build()

		// triggers << factory('Repeat3TimesEvery100').intervalInMillis(100).repeatCount(3).build()

		// triggers << factory('repeat every 500ms forever').intervalInMillis(500).build()

		// triggers << factory('repeat every two days forever').intervalInDays(2).build()

		/*
		triggers << factory('trigger1')
				.intervalInMillis(100)
				.startDelay(2000).noRepeat()
				.jobData(foo: 'bar').build()
		*/

		// triggers << factory('run_once_immediately').noRepeat().build()

		// requires this static import:
		// import static com.agileorbit.schwartz.builder.MisfireHandling.NowWithExistingCount
		/*
		triggers << factory('MisfireTrigger2')
				.intervalInMillis(150)
				.misfireHandling(NowWithExistingCount)
				.build()
		*/

		// triggers << factory('trigger1').group('group1').intervalInSeconds(1).build()

		// requires this static import:
		// import static org.quartz.DateBuilder.todayAt
		// triggers << factory('run every day one second before midnight').startAt(todayAt(23,59,59)).intervalInDays(1).build()
	}
}
