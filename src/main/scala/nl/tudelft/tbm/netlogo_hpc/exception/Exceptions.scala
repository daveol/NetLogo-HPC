package nl.tudelft.tbm.netlogo_hpc.exception

class MissingBinaryException(message: String) extends Exception(message) {}

class MissingFileException(message: String) extends Exception(message) {}

class MissingResourceFileException(message: String) extends Exception(message) {}

class DiscoveryException(message: String) extends Exception(message) {}

class NetLogoReportException(message: String) extends Exception(message) {}

class TimeoutException(message: String) extends Exception(message) {}

class DecodingException(message: String) extends Exception(message) {}