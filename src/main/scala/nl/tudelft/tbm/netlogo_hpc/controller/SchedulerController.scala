package nl.tudelft.tbm.netlogo_hpc.controller

import java.io.File

import nl.tudelft.tbm.netlogo_hpc.NetworkConnection
import nl.tudelft.tbm.netlogo_hpc.scheduler.{Scheduler, SchedulerSettings}

import scala.concurrent.duration.Duration

class SchedulerController(
  model: File,
  experiment: String,
  duration: Duration,
  connection: NetworkConnection,
  behaviorSpace: BehaviorDistributor,
  scheduler: SchedulerSettings => Scheduler,
  concurrentTasks: Int = 8
) extends Runnable {
  def run(): Unit = {

    val settings = new SchedulerSettings(
      model,
      experiment,
      duration,
      concurrentTasks,
      connection
    )

    val instance = scheduler(settings)

    while(!Thread.currentThread().isInterrupted && behaviorSpace.isNeeded){
      // find out how many jobs are running or queued
      val submitted: Int = instance.activeJobs

      // only submit when there are less tasks
      if (submitted < concurrentTasks && behaviorSpace.runNumbersAvailable > 0) instance.submitJob

      // do not go overboard submitting jobs
      Thread.sleep(1000)
    }

    // jobs are unneeded cancel them
    for(job <- instance.jobs) {
      job.cancel()
    }
  }
}
