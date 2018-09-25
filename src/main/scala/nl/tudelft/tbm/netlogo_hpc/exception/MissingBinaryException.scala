package nl.tudelft.tbm.netlogo_hpc.exception

/** Thrown if a binary is missing
  *
  * @param message The message to report on exception
  */
class MissingBinaryException(
  message: String
) extends Exception(message) {}
