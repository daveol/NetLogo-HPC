package nl.tudelft.tbm.netlogo_hpc.exception

/** Thrown if a Timeout is reached
  *
  * @param message The message to report on exception
  */
class TimeoutException(
  message: String
) extends Exception(message) {}