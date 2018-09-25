package nl.tudelft.tbm.netlogo_hpc.controller

import nl.tudelft.tbm.netlogo_hpc.messages.{Message, RunNumberReplyMessage}
import nl.tudelft.tbm.netlogo_hpc.{BehaviorSpaceUtil, NetworkConnection}
import org.apache.log4j.Logger
import org.nlogo.api.LabProtocol

import scala.collection.mutable

/** Distribution of runNumbers
  *
  * @param network The network thread to communicate through
  * @param protocol The experiment as a LabProtocol representation
  */
class BehaviorDistributor(
  network: NetworkConnection,
  protocol: LabProtocol
) extends Runnable {

  /** Logger instance
    */
  private val logger: Logger = Logger.getLogger(this.getClass)

  /** The queue with runNumbers
    */
  private val runNumberQueue: mutable.Queue[Int] = BehaviorSpaceUtil.getRunNumberQueue(protocol)

  /** A HashMap containing the running workers and the runNumber they are running
    */
  private val running: mutable.HashMap[String, Int] = mutable.HashMap.empty[String, Int]

  def run(): Unit = {
    while (!Thread.currentThread().isInterrupted && isNeeded) {
      // get the first RunNumberRequestMessage
      val message: Option[Message] = network.receive(Message.Type.RunNumberRequestMessage)

      // is there a message?
      if (message.isDefined) {
        val runNumber: Int = runNumberQueue.synchronized {
          if (runNumberQueue.isEmpty) 0 // no more numbers to distribute
          //else if() 0                 // check eligibility
          else runNumberQueue.dequeue() // get from queue
        }

        val reply: Message = new RunNumberReplyMessage(
          message.get.simulationRunId,
          message.get.receiverId,
          message.get.senderId,
          0, // not used yet
          runNumber
        )

        logger.info(s"allocating runNumber=$runNumber to client=${message.get.senderId}")

        running.synchronized {
          running += (message.get.senderId -> runNumber)
        }

        network.send(reply)
      }

      running.synchronized {
        // filter all terminated workers
        running.retain((id, number) => {
          number != 0
        })

        // don't check empty lists
        if (running.nonEmpty) {
          for ((id: String, number: Int) <- running) {
            // check if worker is alive
            if (!network.checkClientAlive(id)) {
              logger.info(s"assuming client=$id died, add runNumber=$number back")
              // worker died, remove it from the list
              running.remove(id)

              // requeue number
              runNumberQueue.enqueue(number)
            }
          }
        }
      }
    }
  }

  def isNeeded: Boolean = {
    // sync before check
    runNumberQueue.synchronized {
      running.synchronized {

        // only if there are no runNumbers or running processes we return false
        if(runNumberQueue.nonEmpty)
          true
        else if(running.nonEmpty)
          true
        else
          true
      }
    }
  }

  def runNumbersAvailable: Int = {
    runNumberQueue.synchronized {
      runNumberQueue.length
    }
  }

  def getRunning: Seq[(String,Int)] = {
    running.synchronized {
      running.toSeq
    }
  }
}
