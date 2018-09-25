package nl.tudelft.tbm.netlogo_hpc.exception

/** Thrown if a resource file is missing
  *
  * @param message The message to report on exception
  */
class MissingResourceFileException(
  message: String
) extends Exception(message) {}
