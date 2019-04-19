package nl.tudelft.tbm.netlogo_hpc

import java.io.File
import java.net.InetAddress
import java.util.UUID.randomUUID

import nl.tudelft.tbm.netlogo_hpc.controller.Controller
import nl.tudelft.tbm.netlogo_hpc.scheduler.Scheduler
import nl.tudelft.tbm.netlogo_hpc.worker.Worker
import org.apache.log4j.{BasicConfigurator, Logger}

import scala.concurrent.duration.Duration
import scala.util.Properties


object Main {

  private val logger: Logger = Logger.getLogger(this.getClass)

  def main(args: Array[String]): Unit = {

    // Set up a simple configuration that logs on the console.
    BasicConfigurator.configure()

    /* common settings */
    var model: Option[String] = Properties.envOrNone("NETLOGO_MODEL")
    var experiment: Option[String] = Properties.envOrNone("NETLOGO_EXPERIMENT")
    var table: Option[String] = Properties.envOrNone("NETLOGO_METRIC_TABLE")
    /* controller settings */
    var controllerRole: Boolean = Properties.envOrElse("NETLOGO_CONTROLLER", "false").toBoolean
    var scheduler = Scheduler.getScheduler
    var concurrent = 8
    var duration = Duration("1 hour")
    /* worker settings */
    var workerRole: Boolean = Properties.envOrElse("NETLOGO_WORKER", "false").toBoolean
    /* network settings */
    var listen: Boolean = Properties.envOrElse("NETLOGO_LISTEN", "true").toBoolean
    var uri: String = Properties.envOrElse("NETLOGO_URI", s"tcp://${InetAddress.getLocalHost.getHostAddress}")
    var clientId: String = Properties.envOrElse("NETLOGO_CLIENT_ID", randomUUID().toString)
    var applicationId: String = Properties.envOrElse("NETLOGO_APP_ID", randomUUID().toString)

    val iterator = args.iterator
    while (iterator.hasNext) {
      iterator.next() match {
        /* common settings */
        case "--model" => model = Some(Util.nextOrExit(iterator, "--model"))
        case "--experiment" => experiment = Some(Util.nextOrExit(iterator, "--experiment"))
        /* controller settings */
        case "--controller" => {
          clientId = "controller"
          controllerRole = true
          listen = true
        }
        case "--table" => table = Some(Util.nextOrExit(iterator, "--table"))
        /* worker settings */
        case "--worker" => workerRole = true
        /* network connection */
        case "--connect" => {
          listen = false
          uri = Util.nextOrExit(iterator, "--connect")
        }
        case "--listen" => {
          listen = true
          uri = Util.nextOrExit(iterator, "--listen")
        }
        case "--client-id" => clientId = Util.nextOrExit(iterator, "--client-id")
        case "--application-id" => applicationId = Util.nextOrExit(iterator, "--application-id")
        case "--concurrent" => concurrent = Util.nextOrExit(iterator, "--concurrent").toInt
        case "--help" => {
          print(
            """
              | netlogo-hpc
              |
              | Arguments:
              |  --help
              |      shows this help information
              |
              |  --model [file]
              |      Specify the NetLogo model file
              |
              |  --experiment [name]
              |      Specify the experiment to run
              |
              |  --worker or --controller
              |      Specify the role of the instance
              |        Controller: distributes and submits tasks
              |        Worker:     processes the simulations
              |
              |  --connect [uri] or --listen [uri]
              |      Connect or Bind to a URI in the style of 'tcp://[ip]:[port]'
              |
              |  --table [file]
              |      Table to write the metrics from simulations to
              |
              |  --application-id [string] (optional)
              |      Specifies the application id to check the network against, it will default to a random uuid
              |
              |  --client-id [string] (optional)
              |      Specifies the client id to use for receiving and sending messages, will override to 'controller'
              |      when the controller role is specified otherwhise it will default to a random uuid
              |
              | Scheduler arguments:
              |  --concurrent [number of allowed tasks] (optional)
              |      Specifies the amount of tasks that are allowed to be concurrently deployed on the cluster, it defaults to eight tasks.
              |
            """.stripMargin)
          sys.exit(0)
        }
      }
    }

    if (model.isEmpty) {
      logger.error("NetLogo model file is not given")
      System.exit(1)
    }

    if (experiment.isEmpty) {
      logger.error("Netlogo experiment is not specified")
      System.exit(1)
    }

    if (table.isEmpty) {
      logger.error("NetLogo table file is not given")
      System.exit(1)
    }

    val modelFile: File = new File(model.get)
    val tableFile: File = new File(table.get)

    val connection: NetworkConnection = new NetworkConnection(clientId, applicationId, uri, listen)

    val networkThread = new Thread(connection)
    networkThread.start()
    Thread.sleep(1000)

    if (workerRole) {
      val worker = new Worker(connection, modelFile, experiment.get, tableFile)
      worker.run
    } else if (controllerRole) {
      val controller = new Controller(connection, modelFile, experiment.get, tableFile, duration, scheduler, concurrent)
      controller.run
    } else {
      throw new RuntimeException("No role specified")
    }

    //exit properly
    sys.exit(0)
  }
}
