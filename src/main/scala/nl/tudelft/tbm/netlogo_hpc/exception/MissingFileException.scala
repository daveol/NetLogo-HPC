package nl.tudelft.tbm.netlogo_hpc.exception

/** Thrown if a file is missing
  *
  * @param message The message to report on exception
  */
class MissingFileException(
  message: String
) extends Exception(message) {} 
