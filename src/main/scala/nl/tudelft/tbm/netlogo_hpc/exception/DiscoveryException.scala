package nl.tudelft.tbm.netlogo_hpc.exception

/** Thrown if discovery can not find a suitable entry
  *
  * @param message The message to report on exception
  */
class DiscoveryException(
  message: String
) extends Exception(message) {}
