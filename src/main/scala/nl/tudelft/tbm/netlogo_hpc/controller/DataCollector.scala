package nl.tudelft.tbm.netlogo_hpc.controller

import java.io.{File, PrintWriter}

import nl.tudelft.tbm.netlogo_hpc.messages.{Message, MetricReportMessage}
import nl.tudelft.tbm.netlogo_hpc.{BehaviorSpaceUtil, NetworkConnection}
import org.apache.log4j.Logger
import org.nlogo.api.{CSV, Dump, LabProtocol}

import scala.collection.mutable.HashMap


/** A Collector that writes to a file
  *
  * @param network   the network connection thread to receive messages from
  * @param tableFile the file to store the csv table
  * @param protocol  the LabProtocol
  */
class DataCollector(
  network: NetworkConnection,
  tableFile: File,
  protocol: LabProtocol
) extends Runnable {

  /** Logger for DataCollector
    */
  private val logger: Logger = Logger.getLogger(this.getClass)

  /** CSV transformer for data
    */
  private val csv = new CSV({
    case i: java.lang.Integer => i.toString
    case x => Dump.logoObject(x.asInstanceOf[AnyRef], false, true)
  })

  /** PrintWriter to write the csv data to
    */
  private val table: PrintWriter = new PrintWriter(tableFile)

  /** A HashMap containing the settings for every runNumber
    */
  private val settings: HashMap[Int, List[(String, Any)]] = BehaviorSpaceUtil.getAllSettings(protocol)

  /** The main loop for writing data
    */
  def run: Unit = {
    var flushCounter: Int = 0

    val header = "[sender id]" :: "[run number]" :: protocol.valueSets.map(_.variableName) ::: "[step]" :: protocol.metrics
    table.println(header.map(csv.header).mkString(","))

    while (!Thread.currentThread().isInterrupted) {
      val message = network.receive(Message.Type.MetricReportMessage)

      if (message.nonEmpty) {
        val metric = message.get.asInstanceOf[MetricReportMessage]

        //logger.debug(s"received message from: client=${metric.senderId} runNumber=${metric.runNumber} data.length=${metric.data.length.toString}")

        val data: List[Any] = metric.senderId :: metric.runNumber :: settings(metric.runNumber).map(_._2) ::: metric.step :: metric.data

        table.println(data.map(csv.data).mkString(","))

        flushCounter += 1

        // flush to file every thousand lines
        if (flushCounter > 1000) {
          logger.debug("Flushing to file")
          table.flush()
          flushCounter = 0
        }
      }
    }

    table.flush()
    table.close()
  }
}