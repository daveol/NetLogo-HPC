package nl.tudelft.tbm.netlogo_hpc.scheduler

import java.io.{File, PrintWriter}

import nl.tudelft.tbm.netlogo_hpc.Util
import nl.tudelft.tbm.netlogo_hpc.exception.MissingBinaryException
import nl.tudelft.tbm.netlogo_hpc.scheduler.Scheduler.Job
import org.apache.log4j.Logger

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.Duration
import scala.sys.process.Process
import scala.util.matching.Regex.Match

/** Slurm interaction based on scheduler
  *
  * @param settings The settings for the scheduler
  */
class Slurm (
  settings: SchedulerSettings
) extends Scheduler(settings) {

  /** Logger instance
    */
  private val logger = Logger.getLogger(this.getClass)

  /** The logging directory for slurm it self
    */
  private val logDir = new File(settings.model.getParent, "logs")
  if(!logDir.isDirectory) logDir.mkdirs()

  /** The name of the scheduler
    */
  val name: String = "Slurm"

  /** A list of jobs that are submitted
    */
  private val jobsList: ListBuffer[Job] = ListBuffer.empty[Job]

  /** Return a list of submitted jobs to slurm
    *
    * @return the list of jobs
    */
  def jobs: List[Job] = {
    jobsList.synchronized {
      jobsList.toList
    }
  }

  /** Count the amount of active jobs
    *
    * @return the amount of active jobs
    */
  def activeJobs: Int = {
    jobsList.synchronized {
      jobsList.count((job: Job) => {
        Seq(
          Job.State.Running,
          Job.State.Queued
        ).contains(job.state)
      })
    }
  }

  /** Submit a new Job to slurm
    *
    * @return
    */
  def submitJob: Job = {
    // run sbatch

    val file = File.createTempFile("netlogo-", ".sbatch")
    file.deleteOnExit() //cleanup after ourselves

    val sbatch = new PrintWriter(file)

    sbatch.println( "#!/usr/bin/bash")
    sbatch.println()
    sbatch.println(s"#SBATCH --time=${settings.getSetTime}") //limit time
    sbatch.println( "#SBATCH --signal=USR1@180") //signal 3 minutes before end for cleanup
    sbatch.println(s"#SBATCH --mem=${settings.getMemory}")
    sbatch.println(s"#SBATCH -o ${new File(logDir, "slurm-%j.out").getAbsolutePath}")
    sbatch.println()
    sbatch.println(s"export NETLOGO_MODEL=${settings.getModelPath}")
    sbatch.println(s"export NETLOGO_EXPERIMENT=${settings.experiment}")
    sbatch.println( "export NETLOGO_WORKER=true")
    sbatch.println(s"export NETLOGO_URI=${settings.getConnectionURI}")
    sbatch.println( "export NETLOGO_METRIC_TABLE=\"$(mktemp -d)/backup.csv\"")
    sbatch.println(s"export NETLOGO_APP_ID=${settings.getApplicationId}")
    sbatch.println( "export NETLOGO_CLIENT_ID=\"$(hostname -f)-slurm-${SLURM_JOB_ID}\"")
    sbatch.println( "export NETLOGO_BACKUP_DIR=\"$(dirname \"$NETLOGO_MODEL\")/backups\"")
    sbatch.println()
    sbatch.println(s"srun ${Util.getStartupScript}")
    sbatch.println( "[ -d \"$NETLOGO_BACKUP_DIR\" ] || mkdir -p \"$NETLOGO_BACKUP_DIR\"")
    sbatch.println( "cp \"$NETLOGO_METRIC_TABLE\" \"${NETLOGO_BACKUP_DIR}/${NETLOGO_CLIENT_ID}.csv\"")


    sbatch.flush()
    sbatch.close()

    val proc: String = Process(s"sbatch ${file.getAbsolutePath}").!!
    val matches: Option[Match] = " ([0-9]+)$".r.findFirstMatchIn(proc)

    if (matches.isEmpty) throw new RuntimeException("Failed to submit job")

    // use the jobid that sbatch returned to create,
    // add it to ListBuffer and return the job object
    val job = new SlurmJob(matches.get.group(1))

    jobsList.synchronized {
      jobsList += job
    }

    job
  }

  /** A Class representing a slurm job for interaction with it
    *
    * @param ID The Job ID that slurm assigned
    */
  private class SlurmJob(val ID: String) extends Job {

    /** Get the job state from slurm
      *
      * @return The job state
      */
    def state: Job.State.Value = {
      // ref; https://slurm.schedmd.com/squeue.html#lbAG

      val proc: String = Process(s"""squeue -h -j $ID -o "%t" --states=all""").!!.trim

      proc match {
        case stat if stat.contains("CA") => Job.State.Canceled
        case stat if stat.contains("CD") => Job.State.Completed
        case stat if stat.contains("CG") => Job.State.Completed
        case stat if stat.contains("PD") => Job.State.Queued
        case stat if stat.contains("F")  => Job.State.Failed
        case stat if stat.contains("R")  => Job.State.Running
        case _ => {
          logger.debug(s"Job status unknown: status=$proc")
          Job.State.Canceled
        }
      }
    }

    /** Cancel the job
      */
    def cancel(): Unit = {
      // run scancel ${ID}
      // ref: https://slurm.schedmd.com/scancel.html
      Process(s"scancel $ID").!
    }

    /** Get the time left for the job
      *
      * @return A duration object
      */
    def timeLeft: Duration = {
      // run squeue -j ${ID} -o "%L"
      // ref: https://slurm.schedmd.com/squeue.html#OPT_%L
      val proc = Process(s"""squeue -h -j $ID -o "%L" """).!!
      val time = "([0-9]{2}):([0-9]{2}):([0-9]{2})".r.findFirstMatchIn(proc)

      if(time.isEmpty) throw new RuntimeException("time not matched")

      Duration(time.get.group(1).toInt, "h") + Duration(time.get.group(2).toInt, "m") + Duration(time.get.group(3).toInt, "s")
    }
  }

}

/** Slurm scheduler object for static methods
  */
object Slurm {

  /** Check if Slurm is available on the system
    *
    * @return True if Slurm is available
    */
  def detect: Boolean = {
    try {
      /* look for binaries in path */
      Util.which("sbatch")
      Util.which("scancel")
      Util.which("squeue")

      /* no exceptions, seems everything is in order */
      true
    } catch {
      case e: MissingBinaryException => false // we are clearly missing binaries the code requires
    }
  }
}
