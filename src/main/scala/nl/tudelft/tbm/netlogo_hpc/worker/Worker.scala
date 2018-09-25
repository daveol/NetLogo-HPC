package nl.tudelft.tbm.netlogo_hpc.worker

import java.io.{File, PrintWriter}

import nl.tudelft.tbm.netlogo_hpc.exception.TimeoutException
import nl.tudelft.tbm.netlogo_hpc.messages._
import nl.tudelft.tbm.netlogo_hpc.{BehaviorSpaceUtil, NetworkConnection, Util}
import org.apache.log4j.Logger
import org.nlogo.api.LabProtocol
import org.nlogo.lab.TableExporter


/** The worker process that manages requests for runNumbers and the workspace
  *
  * @param connection The networking connection
  * @param model The model file for the experiment
  * @param experiment The experiment to run from the model
  * @param table The table to write metrics to
  */
class Worker(
  connection: NetworkConnection,
  model: File,
  experiment: String,
  table: File
) {

  /** Logger instance
    */
  private val logger: Logger = Logger.getLogger(this.getClass)

  /** The table print writer to write the csv to
    */
  private val tablePrintWriter: PrintWriter = new PrintWriter(table)

  /** The LabProtocol representation for the experiment
    */
  private val protocol: LabProtocol = BehaviorSpaceUtil.getExperimentFromModel(model, experiment)

  /** The Exporter that writes the csv files
    */
  private val tableWriter = new TableExporter(
    model.getAbsolutePath,
    BehaviorSpaceUtil.getDimensions(model, protocol),
    protocol,
    tablePrintWriter
  )

  /** Boolean that decides if the loop needs to run
    */
  private var active: Boolean = true


  def run: Unit = {
    tableWriter.experimentStarted()

    while (this.active) {
      var runNumber: Int = 0

      try {
        runNumber = this.getRunNumber
      } catch {
        case e: TimeoutException => logger.warn(s"Timeout reached: ${e.getMessage}")
      }

      if (runNumber > 0) {
        logger.info(s"Running: runNumber=$runNumber")
        val runner: BehaviorWorkspace = new BehaviorWorkspace(model, protocol, runNumber, connection)

        runner.addListener(tableWriter)
        // run
        runner.run()

        // then cleanup
        runner.dispose()
      } else {
        logger.info(s"no more runNumbers")
        this.active = false
      }
    }

    tableWriter.experimentCompleted()
  }

  /** Request a run-number from the controller
    *
    * @return a runNumber
    */
  private def getRunNumber: Int = {
    val request = new RunNumberRequestMessage(
      connection.applicationId,
      connection.clientId,
      "controller",
      0
    )
    connection.send(request)

    var response: Option[Message] = None
    Util.checkWithTimeout(() => {
      response = connection.receive(Message.Type.RunNumberReplyMessage)

      response.isEmpty
    }, 60)

    response.get.asInstanceOf[RunNumberReplyMessage].runNumber
  }
}
