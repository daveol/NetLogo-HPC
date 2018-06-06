package nl.tudelft.tbm.experiment

import org.nlogo.headless.HeadlessWorkspace
import org.nlogo.headless.BehaviorSpaceCoordinator
import org.nlogo.nvm.LabInterface.Settings

object  BehaviorDump {
  def main(args: Array[String]) {
    var model: Option[String] = None //Some(args(0))
    var experiment: Option[String] = None //Some(args(1))

    if(args.length >= 1) model = Some(args(0))
    if(args.length >= 2) experiment = Some(args(1))

    val settings = new Settings(
      model.get,       //modelPath
      experiment,  //protocolName
      None,          //externalXMLFile
      None,          //tableWriter
      None,          //spreadsheetWriter
      None,          //dims
      0,             //threads
      false          //suppressErrors
    )

    val workspace = HeadlessWorkspace.newInstance
    workspace.open(settings.modelPath)

    val proto = try {
      BehaviorSpaceCoordinator.selectProtocol(settings, workspace)
    } finally {
      workspace.dispose()
    }

    proto match{
      case Some(protocol) =>
        println(s"LabProtocol.countRuns:\t\t\t${protocol.countRuns}")
        println(s"LabProtocol.exitCondition:\t\t${protocol.exitCondition}")
        println(s"LabProtocol.finalCommands:\t\t${protocol.finalCommands}")
        println(s"LabProtocol.goCommands:\t\t\t${protocol.goCommands}")
        println(s"LabProtocol.metrics:")
        protocol.metrics.foreach(metric =>
          println(s"\t${metric}")
        )
        println(s"LabProtocol.name:\t\t\t${protocol.name}")
        println(s"LabProtocol.refElements:")
        for((settings, runNumber) <- protocol.refElements zip Stream.from(1).iterator){
          println(s"\t#$runNumber")
          for((name, value) <- settings){
            println(s"\t\t${name}=${value}")
          }
        }
        println(s"LabProtocol.repetitions:\t\t${protocol.repetitions}")
        println(s"LabProtocol.runMetricsEveryStep:\t${protocol.runMetricsEveryStep}")
        println(s"LabProtocol.sequentialRunOrder:\t\t${protocol.sequentialRunOrder}")
        println(s"LabProtocol.setupCommands:\t\t${protocol.setupCommands}")
        println(s"LabProtocol.timeLimit:\t\t\t${protocol.timeLimit}")
        println(s"LabProtocol.valueSets:")
        protocol.valueSets.foreach(valueSet =>
            println(s"\t${valueSet.toString()}")
        )
      case None =>
        throw new IllegalArgumentException("Invalid run, specify experiment name or setup file")
    }

    workspace.dispose()
  }
}

