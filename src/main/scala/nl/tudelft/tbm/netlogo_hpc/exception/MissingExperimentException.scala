package nl.tudelft.tbm.netlogo_hpc.exception

/** Thrown if the specified experiment is missing
  *
  * @param message The message to report on exception
  */
class MissingExperimentException(
  message: String
) extends Exception(message) {}
