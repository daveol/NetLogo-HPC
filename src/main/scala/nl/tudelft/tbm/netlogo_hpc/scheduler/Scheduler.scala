package nl.tudelft.tbm.netlogo_hpc.scheduler

import nl.tudelft.tbm.netlogo_hpc.exception.DiscoveryException

import scala.concurrent.duration.Duration


/**
  *
  */
abstract class Scheduler(
  settings: SchedulerSettings
) {

  /** The name of the scheduler
    */
  val name: String

  /** List of jobs submitted
    */
  def jobs: List[Scheduler.Job]

  /** Count the active jobs
    *
    * @return the amount of active jobs
    */
  def activeJobs: Int

  /** Submit a Job to the cluster
    *
    * @return A representation of the job
    */
  def submitJob: Scheduler.Job

}

object Scheduler {
  /** A representation of a Job
    */
  trait Job {

    /** The ID of the job
      */
    val ID: String

    /** The status of the job
      *
      * @return The status
      */
    def state: Job.State.Value

    /**
      *
      * @return
      */
    def timeLeft: Duration

    /**
      *
      */
    def cancel()
  }

  object Job {
    /**
      *
      */
    object State extends Enumeration {

      /** The task is Queued
        */
      val Queued: Value = Value

      /** The task is Running
        */
      val Running: Value = Value

      /** The task is Failed
        */
      val Failed: Value = Value

      /** The task is Completed
        */
      val Completed: Value = Value

      /** The task is Canceled
        */
      val Canceled: Value = Value
    }
  }

  /** Detect a scheduler and return it
    *
    * @return The scheduler class
    */
  def getScheduler: SchedulerSettings => Scheduler = {
    if (Slurm.detect) return (settings: SchedulerSettings) => new Slurm(settings)
    //if(PBS.detect) return (settings: SchedulerSettings) => new PBS(settings)

    /* Last resort exception */
    throw new DiscoveryException("No scheduler detected")
  }

  /** Get the class for a scheduler
    *
    * @param name The name of the scheduler
    * @return The scheduler class
    */
  def getScheduler(name: String): SchedulerSettings => Scheduler = {
    name match {
      case "Slurm" => return (settings: SchedulerSettings) => new Slurm(settings)
      //case "PBS" => return (settings: SchedulerSettings) => new PBS(settings)
    }

    /* Last resort exception */
    throw new IllegalArgumentException(s"Scheduler '$name' is not implemented")
  }
}
