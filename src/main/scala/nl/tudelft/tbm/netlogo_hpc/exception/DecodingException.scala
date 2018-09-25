package nl.tudelft.tbm.netlogo_hpc.exception


/** Thrown if decoding a message is not possible
  *
  * @param message The message to report on exception
  */
class DecodingException(
  message: String
) extends Exception(message) {}
