package nl.tudelft.tbm.netlogo_hpc.scheduler

import java.io.File
import nl.tudelft.tbm.netlogo_hpc.NetworkConnection
import scala.concurrent.duration.Duration


class SchedulerSettings(
  /** Model File to use
    */
  val model: File,

  /** Experiment to run
    */
  val experiment: String,

  /** How long the job can take
    */
  val duration: Duration,

  /** The amount of concurrent tasks to deploy on a cluster
    */
  val concurrentTasks: Int,

  /** The connection to base the URI on
    */
  connection: NetworkConnection
){


  def getMemory: String = {
    "2G"
  }



  /** Get the application ID used for networkConnection
    *
    * @return The application ID
    */
  def getApplicationId: String = {
    connection.applicationId
  }


  /** Get the URI to connect clients to
    *
    * @return The URI to connect to
    */
  def getConnectionURI: String = {
    connection.URI
  }


  /** Get the path to the model file
    *
    * @return The absolute path to the model file
    */
  def getModelPath: String = {
    model.getAbsolutePath
  }

  /** Get the time to set for jobs
    *
    * @return The time to set in [hours]:[minutes]:[seconds]
    */
  def getSetTime: String = {
    s"${duration.toHours}:${Math.abs(duration.toMinutes % 60)}:${Math.abs(duration.toSeconds % 60)}"
  }
}
