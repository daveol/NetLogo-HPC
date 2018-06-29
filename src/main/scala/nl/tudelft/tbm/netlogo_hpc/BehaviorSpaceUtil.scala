package nl.tudelft.tbm.netlogo_hpc

import java.io.File

import nl.tudelft.tbm.netlogo_hpc.exception.MissingExperimentException
import org.nlogo.api.LabProtocol
import org.nlogo.core.WorldDimensions
import org.nlogo.headless.{BehaviorSpaceCoordinator, HeadlessWorkspace}
import org.nlogo.nvm.Procedure

import scala.collection.mutable.{HashMap, Queue}

/**
  * A collection of methods for working with BehaviorSpace
  */
object BehaviorSpaceUtil {
  def getDimensions(model: File, protocol: LabProtocol): WorldDimensions = {
    val workspace = HeadlessWorkspace.newInstance
    workspace.open(model.getAbsolutePath)

    for ((name, value) <- getSettingsFromExperiment(protocol, 1)) {
      if (workspace.world.isDimensionVariable(name)) {
        // Dimension variables are always integers
        val _value = value.asInstanceOf[java.lang.Double].intValue()
        val _dimension = workspace.world.setDimensionVariable(name, _value, workspace.world.getDimensions)
        // we'll only set the dimention if it is different
        if (!workspace.world.equalDimensions(_dimension)) workspace.setDimensions(_dimension)

      }
    }

    val dims = workspace.world.getDimensions
    workspace.dispose()

    dims
  }


  /** Get a HashMap for the LabProtocol settings
    *
    * @param protocol The protocol/experiment
    * @return A HashMap with runNumber as keys, and the settings list as the values
    */
  def getAllSettings(protocol: LabProtocol): HashMap[Int, List[(String, Any)]] = {
    val runList = new HashMap[Int, List[(String, Any)]]

    for ((settings, runNumber) <- protocol.refElements zip Stream.from(1).iterator) {
      runList.put(runNumber, settings)
    }

    runList
  }


  /** Get a queue of run numbers
    *
    * @param protocol The protocol/experiment to make the queue for
    * @return A queue of run numbers
    */
  def getRunNumberQueue(protocol: LabProtocol): Queue[Int] = {
    Queue.range(1, protocol.countRuns)
  }


  /** Get a compiled exit condition test or None
    *
    * @param workspace The workspace
    * @param protocol  The BehaviorSpace experiment (LabProtocol)
    * @return A compiled exit condition if there is one, otherwise return none
    */
  def getCompiledExitCondition(workspace: HeadlessWorkspace, protocol: LabProtocol): Option[Procedure] = {
    if (protocol.exitCondition.trim.isEmpty)
      None
    else
      Some(workspace.compileReporter(protocol.exitCondition))
  }


  /** Get settings for a run from an experiment
    *
    * @param experiment The BehaviorSpace experiment represented as a LabProtocol
    * @param runNumber  The run number to get the settings for
    * @return a list of settings for the model
    */
  def getSettingsFromExperiment(experiment: LabProtocol, runNumber: Int): List[(String, Object)] = {
    experiment.refElements.toSeq(runNumber - 1)
  }

  /** Get the BehaviorSpace experiment represented as a LabProtocol
    *
    * @param model          The NetLogo model file
    * @param experimentName the name of the experiment we want
    * @return a LabProtocol that represents the experiment
    */
  def getExperimentFromModel(model: File, experimentName: String): LabProtocol = {
    val protocols = BehaviorSpaceCoordinator.protocolsFromModel(
      model.getAbsolutePath,
      HeadlessWorkspace.newInstance
    )

    val protocol = protocols.find(_.name == experimentName)

    if (protocol.isEmpty) throw new MissingExperimentException(s"Missing experiment: ${experimentName}")

    protocol.get
  }
}
