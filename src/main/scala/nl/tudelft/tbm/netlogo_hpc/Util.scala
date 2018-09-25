package nl.tudelft.tbm.netlogo_hpc

import java.io.File

import nl.tudelft.tbm.netlogo_hpc.exception.{MissingBinaryException, TimeoutException}


/** Utility class for common utility functions across the code base
  */
object Util {

  /** Implementation of the which(1) binary in scala
    *
    * @param bin which binary are we looking for
    * @return the path of the library we are looking for
    */
  def which(bin: String): String = {
    val paths: Array[String] = sys.env("PATH").split(':')
    for (dir <- paths) {
      val file: File = new File(dir, bin)
      if (file.isFile) return file.getPath
    }
    throw new MissingBinaryException(s"which: binary $bin does not exist in path")
  }

  /** Blocking until check returns false or timeout is reached
    *
    * @param check   a function that returns a boolean: when it returns false it will stop
    * @param timeout timeout in seconds
    */
  def checkWithTimeout(check: () => Boolean, timeout: Int): Unit = {
    /* the times */
    val startTime = unixEpoch()
    var checkTime = unixEpoch()

    while (((startTime + timeout) > checkTime) && check()) {
      checkTime = unixEpoch()
      // wait, delay?
    }

    if (!((startTime + timeout) > checkTime)) throw new TimeoutException("Timeout reached")
  }

  /** Easy shorthand for unix epoch time
    *
    * @return seconds since the 1st of january 1970 at 00:00 UTC
    */
  def unixEpoch(): Long = {
    System.currentTimeMillis() / 1000
  }

  /** Get the next item in the iterator or exit the process
    *
    * @param iterator iterator to check hasNext on
    * @param parameter The parameter for next
    * @param exitCode The exitcode to use, defaults to 1
    * @return The string from the iterator
    */
  def nextOrExit[A](iterator: Iterator[A], parameter: A, exitCode: Int = 1): A = {
    if (!iterator.hasNext) {
      println(s"error: '$parameter' requires a value")
      System.exit(exitCode)
    }
    iterator.next()
  }

  /** Get the path to the NetLogo-HPC script
    *
    * @return An absolute path for NetLogo-HPC
    */
  def getStartupScript: String = {
    val progBin = new File(sys.props("prog.home"), "bin")
    val prog = new File(progBin, "netlogo-hpc")

    prog.getAbsolutePath
  }
}

