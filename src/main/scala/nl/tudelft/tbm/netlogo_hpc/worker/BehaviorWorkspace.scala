package nl.tudelft.tbm.netlogo_hpc.worker

import java.io.File

import nl.tudelft.tbm.netlogo_hpc.exception.NetLogoReportException
import nl.tudelft.tbm.netlogo_hpc.messages.MetricReportMessage
import nl.tudelft.tbm.netlogo_hpc.{BehaviorSpaceUtil, NetworkConnection}
import org.apache.log4j.Logger
import org.nlogo.api.{Dump, LabProtocol, SimpleJobOwner}
import org.nlogo.core.WorldDimensions
import org.nlogo.headless.HeadlessWorkspace
import org.nlogo.nvm.LabInterface.ProgressListener
import org.nlogo.nvm.Procedure

import scala.collection.mutable.MutableList

/**
  * A workspace to run part of a NetLogo BehaviorSpace experiment
  *
  * @param model     the model file, usually ends in .nlogo
  * @param protocol  the BehaviorSpace experiment (LabProtocol) to run from
  * @param runNumber the experiment's run number to run
  */
class BehaviorWorkspace(
  private val model: File,
  private val protocol: LabProtocol,
  private val runNumber: Int,
  private val connection: NetworkConnection
) {

  /** Logging instance
    */
  private val logger: Logger = Logger.getLogger(this.getClass)

  /** The actual NetLogo Workspace
    *
    * This workspace is for handling most of the NetLogo model
    */
  private val workspace: HeadlessWorkspace = HeadlessWorkspace.newInstance
  workspace.open(model.getPath)

  /** The owner to use for runCompiledReporter and runCompiledCommand in the workspace
    */
  private val owner: SimpleJobOwner = new SimpleJobOwner("BehaviorWorkSpace", workspace.world.mainRNG)

  /** The settings to get, from the run number
    */
  private val settings: List[(String, Object)] = BehaviorSpaceUtil.getSettingsFromExperiment(protocol, runNumber)

  /** The listeners that we need to report to
    */
  private val listeners: MutableList[ProgressListener] = new MutableList[ProgressListener]

  /** The compiled 'setup' procedure
    *
    * Compiled from the setupCommands variable defined in the BehaviorSpace experiment (LabProtocol)
    */
  private val setupProcedure: Procedure = workspace.compileCommands(protocol.setupCommands)

  /** The compiled 'go' procedure
    *
    * Compiled from the goCommands variable defined in the BehaviorSpace experiment (LabProtocol)
    */
  private val goProcedure: Procedure = workspace.compileCommands(protocol.goCommands)

  /** The compiled 'final' procedure
    *
    * Compiled from the finalCommands variable defined in the BehaviorSpace experiment (LabProtocol)
    */
  private val finalProcedure: Procedure = workspace.compileCommands(protocol.finalCommands)

  /** A List of compiled procedures to get metrics
    *
    * Compiled from the list of metrics defined in the BehaviorSpace experiment (LabProtocol)
    */
  private val metricProcedures: List[Procedure] = protocol.metrics.map(workspace.compileReporter)

  /** The compiled procedure for checking the exit condition
    */
  private val exitConditionProcedure: Option[Procedure] = BehaviorSpaceUtil.getCompiledExitCondition(workspace, protocol)


  // Adjust Workspace BehaviorSpace settings
  workspace.behaviorSpaceRunNumber(runNumber)
  workspace.behaviorSpaceExperimentName(protocol.name)

  // Now setup the world variables
  for ((name, value) <- settings) {
    if (workspace.world.isDimensionVariable(name)) {
      // Dimension variables are always integers
      val _value = value.asInstanceOf[java.lang.Double].intValue()
      val _dimension = workspace.world.setDimensionVariable(name, _value, workspace.world.getDimensions)
      // we'll only set the dimention if it is different
      if (!workspace.world.equalDimensions(_dimension)) workspace.setDimensions(_dimension)

    } else if (name.equalsIgnoreCase("RANDOM-SEED")) {
      // The random seed is interpreted as a long
      val _value = value.asInstanceOf[java.lang.Double].longValue()
      workspace.world.mainRNG.setSeed(_value)

    } else {
      // we set the global variable
      workspace.world.setObserverVariableByName(name, value)
    }
  }

  def run(): Unit = {
    // call ProgressListener.runStarted
    listeners.synchronized {
      listeners.foreach(_.runStarted(workspace, runNumber, settings))
    }

    // run setup procedure
    workspace.runCompiledCommands(owner, setupProcedure)

    var steps = 0
    if (protocol.runMetricsEveryStep) reportMeasurements(steps)

    while (
      !timeLimitReached(steps) &&
        !exitConditionReached &&
        !workspace.runCompiledCommands(owner, goProcedure)
    ) {
      steps += 1

      listeners.synchronized {
        listeners.foreach(_.stepCompleted(workspace, steps))
      }

      if (protocol.runMetricsEveryStep) reportMeasurements(steps)

      workspace.updateDisplay(false)
    }

    logger.debug(s"Conditions: runNumber=${runNumber} timeLimitReached=${!timeLimitReached(steps)} exitConditionReached=${!exitConditionReached}")

    if (workspace.lastLogoException != null) {
      val exception = workspace.lastLogoException
      logger.warn(s"Logo exception found: ${exception.getMessage}")
    }

    if (!protocol.runMetricsEveryStep) reportMeasurements(steps)

    workspace.runCompiledCommands(owner, finalProcedure)
    listeners.synchronized {
      listeners.foreach(_.runCompleted(workspace, runNumber, steps))
    }
  }

  /** Add a ProgressListener for the workspace from monitoring metrics & more
    *
    * @param listener the progresslistener to add
    */
  def addListener(listener: ProgressListener): Unit = {
    listener.synchronized {
      listeners += listener
    }
  }

  /** Report the measurements to the progressListeners and send the metrics
    *
    * @param step The step to report the measurements for
    */
  private def reportMeasurements(step: Int): Unit = {
    val measurements: List[AnyRef] = metricProcedures.map(workspace.runCompiledReporter(owner, _))
    listeners.synchronized {
      listeners.foreach(_.measurementsTaken(workspace, runNumber, step, measurements))
    }

    //logger.debug(s"Sending progress: runNumber=${runNumber}, step=${step}, data.length=${measurements.length}")
    connection.send(new MetricReportMessage(
      connection.applicationId,
      connection.clientId,
      "controller",
      0,
      runNumber,
      step,
      measurements
    ))
  }

  /** Check if the time limit is reached
    *
    * @param step The step to check the limit on
    * @return True if the condition is reached, False if not
    */
  private def timeLimitReached(step: Int): Boolean = {
    !(protocol.timeLimit == 0 || step < protocol.timeLimit)
  }

  /** Check if the exit condition is met in the simulation
    *
    * @return True if the condition is reached, False if not
    */
  private def exitConditionReached: Boolean = {
    // Don't bother checking if the exitCondition is empty
    if (exitConditionProcedure.isEmpty) return false

    // match the result to something useful or throw
    workspace.runCompiledReporter(owner, exitConditionProcedure.get) match {
      case b: java.lang.Boolean => b.booleanValue()
      case null => throw new NetLogoReportException(
        s"Stopping condition did not return a value"
      )
      case result: AnyRef => throw new NetLogoReportException(
        s"Stopping condition did not report a boolean, but reported ${Dump.typeName(result)} ${Dump.logoObject(result)}"
      )
    }
  }

  def dispose(): Unit = {
    workspace.dispose()
  }

}
