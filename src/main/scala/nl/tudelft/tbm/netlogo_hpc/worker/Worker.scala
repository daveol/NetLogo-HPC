package nl.tudelft.tbm.netlogo_hpc.worker

import java.io.{File, PrintWriter}

import nl.tudelft.tbm.netlogo_hpc.exception.TimeoutException
import nl.tudelft.tbm.netlogo_hpc.messages._
import nl.tudelft.tbm.netlogo_hpc.{BehaviorSpaceUtil, NetworkConnection, Util}
import org.apache.log4j.Logger
import org.nlogo.api.LabProtocol
import org.nlogo.headless.HeadlessWorkspace
import org.nlogo.lab.TableExporter


/**
  *
  * @param connection
  * @param model
  * @param experiment
  * @param table
  */
class Worker(
  connection: NetworkConnection,
  model: File,
  experiment: String,
  table: File
) {

  /**
    *
    */
  private val logger: Logger = Logger.getLogger(this.getClass)

  /**
    *
    */
  private val tablePrintWriter: PrintWriter = new PrintWriter(table)

  /**
    *
    */
  private val protocol: LabProtocol = BehaviorSpaceUtil.getExperimentFromModel(model, experiment)

  /**
    *
    */
  private val tableWriter = new TableExporter(
    model.getAbsolutePath,
    BehaviorSpaceUtil.getDimensions(model, protocol),
    protocol,
    tablePrintWriter
  )

  /**
    *
    */
  private var active: Boolean = true

  /**
    *
    */
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