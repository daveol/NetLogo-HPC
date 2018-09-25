package nl.tudelft.tbm.netlogo_hpc.exception

/** Thrown if NetLogo fails to report properly
  *
  * @param message The message to report on exception
  */
class NetLogoReportException(
  message: String
) extends Exception(message) {}
