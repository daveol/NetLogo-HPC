package nl.tudelft.tbm.netlogo_hpc.controller

import java.io.File

import nl.tudelft.tbm.netlogo_hpc.scheduler.{Scheduler, SchedulerSettings}
import nl.tudelft.tbm.netlogo_hpc.{BehaviorSpaceUtil, NetworkConnection}

import scala.concurrent.duration.Duration

/** The controller
  *
  * @param network NetworkConnection thread for communication
  * @param model The model to run
  * @param experiment The experiment to run
  * @param table The table (csv file) to save the metrics to
  */
class Controller(
  val network: NetworkConnection,
  val model: File,
  val experiment: String,
  val table: File,
  val time: Duration,
  scheduler: SchedulerSettings => Scheduler,
  concurrent: Int
) extends Runnable {

  /** The LabProtocol representation of the experiment
    */
  private val protocol = BehaviorSpaceUtil.getExperimentFromModel(model, experiment)

  /** The BehaviorSpace distributor
    */
  val behaviorDistributor = new BehaviorDistributor(network, protocol)

  /** The thread for the BehaviorSpace distributor
    */
  val behaviorDistributorThread = new Thread(behaviorDistributor)

  /** The metric data collector
    */
  val remoteProgressListener = new DataCollector(network, table, protocol)

  /** The thread for the metric data collector
    */
  val remoteProgressListenerThread = new Thread(remoteProgressListener)

  val schedulerController = new SchedulerController(model, experiment, time, network, behaviorDistributor, scheduler, concurrent)

  val schedulerControllerThread = new Thread(schedulerController)

  /** Start the threads and join them
    */
  def run(): Unit = {
    behaviorDistributorThread.start()
    remoteProgressListenerThread.start()
    schedulerControllerThread.start()

    schedulerControllerThread.join()
    behaviorDistributorThread.join()
    remoteProgressListenerThread.join()
  }
}
