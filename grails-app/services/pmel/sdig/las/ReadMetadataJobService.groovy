package pmel.sdig.las


import com.agileorbit.schwartz.StatefulSchwartzJob
import grails.gorm.transactions.NotTransactional
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import org.quartz.Scheduler
import org.quartz.TriggerKey
import org.springframework.beans.factory.annotation.Autowired

@CompileStatic
@Slf4j
class ReadMetadataJobService implements StatefulSchwartzJob {

	IngestService ingestService
	@Autowired protected Scheduler quartzScheduler

	@NotTransactional
	void execute(JobExecutionContext context) throws JobExecutionException {
		ingestService.addVariablesToAll()
//		ingestService.cleanup()
	}
	void unscheuleUpdate() {
		log.info("Stopping updates for all unprocessed THREDDS catalogs")
		TriggerKey key = new TriggerKey("ALL_METADATA", "dataset");
		quartzScheduler.unscheduleJob(key)
	}

	void buildTriggers() {
		log.info("Starting updates on unprocessed THREDDS catalogs")
		TriggerKey key = new TriggerKey("ALL_METADATA", "dataset");
		triggers <<
				factory('trigger_metadata').
				key(key).
				intervalInMinutes(15).
				startDelay(1000*60*5).  // 5 minutes
				build()


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
