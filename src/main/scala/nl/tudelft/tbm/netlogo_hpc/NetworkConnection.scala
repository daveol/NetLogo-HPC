package nl.tudelft.tbm.netlogo_hpc

import nl.tudelft.tbm.netlogo_hpc.exception.DecodingException
import nl.tudelft.tbm.netlogo_hpc.messages.{KeepAliveMessage, Message}
import org.apache.log4j.Logger
import org.sim0mq.Sim0MQException
import org.sim0mq.message.SimulationMessage
import org.zeromq.ZMQ
import zmq.ZMQ.ZMQ_LAST_ENDPOINT

import scala.collection.mutable
import scala.sys.process.Process


/** To send and receive Messages
  *
  * @param clientId      The unique client id to use for identification
  * @param applicationId The id unique to the application
  * @param uri           The uri to listen/connect on in the form of: [protocol]://[address]:[port]
  * @param listen        Whether to listen (true) or to connect (false)
  */
class NetworkConnection(
  val clientId: String,
  val applicationId: String,
  uri: String,
  listen: Boolean = false
) extends Runnable {

  /** Logger for the network class
    */
  private val logger: Logger = Logger.getLogger(this.getClass)

  /** ZMQ context to use for socket and poller
    */
  private val context: ZMQ.Context = ZMQ.context(1)

  /** ZMQ Poller to poll the socket for new messages
    */
  private val poller: ZMQ.Poller = context.poller(1)

  /** ZMQ Socket to use for receiving and sending
    */
  private val socket: ZMQ.Socket = context.socket(if (listen) ZMQ.REP else ZMQ.REQ)

  /** The Message queue for received messages
    */
  private val messageReceiverQueue: mutable.Queue[Message] = mutable.Queue.empty[Message]

  /** The Message queue for sending messages
    */
  private val messageSenderQueue: mutable.Queue[Message] = mutable.Queue.empty[Message]

  /** A list of clients with last time a message was received
    */
  private val lastMessageReceived: mutable.HashMap[String, Long] = mutable.HashMap.empty[String, Long]


  /** Main loop to receive and send messages
    */
  def run(): Unit = {
    //socket.setIdentity(clientId.getBytes(ZMQ.CHARSET))

    if (listen) {
      /* open socket for listening */
      socket.bind(uri)
    } else {
      /* open socket for connecting */
      logger.debug(s"Connecting to $uri")
      socket.connect(uri)
    }

    /* add to poller */
    poller.register(socket)

    if (listen) logger.info(s"Listening on: $URI")
    logger.debug(s"variables: applicationId=$applicationId clientId=$clientId")

    var remoteId: String = "controller"
    var received: Boolean = false

    while (!Thread.currentThread().isInterrupted) {

      /* receive */
      poller.poll(100)
      if (poller.pollin(0)) {
        val rawReceived = socket.recv(0)

        try {
          val messageObjects: Array[Object] = SimulationMessage.decode(rawReceived)
          val message: Message = Message.decode(messageObjects)

          remoteId = message.senderId

          if (clientId.equals(message.receiverId) && applicationId.equals(message.simulationRunId)) {
            // set last receive time
            lastMessageReceived.synchronized {
              lastMessageReceived(remoteId) = Util.unixEpoch()
            }

            if (!message.messageType.equals(Message.Type.KeepAliveMessage)) {
              // we received a 'real' message
              received = true

              messageReceiverQueue.synchronized { messageReceiverQueue.enqueue(message) }
            }
          } else logger.debug(
            s"Foreign Message Received: receiver=${message.receiverId} type=${message.messageType.toString}"
          )
        } catch {
          case e: DecodingException => logger.info(s"Invalid message received: ${e.getMessage}")
          case e: Sim0MQException => logger.info(s"Malformed message received: ${e.getMessage}")
          case e: InterruptedException => logger.info(s"Network thread interrupted: ${e.getMessage}")
        }
      }

      /* sender */
      if (poller.pollout(0)) {
        messageSenderQueue.synchronized {
          val message: Option[Message] = if (listen) {
            messageSenderQueue.dequeueFirst(_.receiverId == remoteId)
          } else if (messageSenderQueue.nonEmpty) {
            Some(messageSenderQueue.dequeue())
          } else {
            None
          }


          if (message.nonEmpty) {
            socket.send(message.get.encode())
          } else {
            val m = new KeepAliveMessage(applicationId, clientId, remoteId, 0)
            socket.send(m.encode())

            // only wait if we are just exchanging keepalives
            if(!received) Thread.sleep(100)
          }
        }
      }
    }

    /* last ditch, send everything */
    messageSenderQueue.synchronized {
      while (messageSenderQueue.nonEmpty) {
        val message = messageSenderQueue.dequeue()
        socket.send(message.encode())
      }
    }

    /* we broke out of the loop */
    socket.close()
  }

  /** Get the last known endpoint from sockopts
    *
    * @return the last known endpoint
    */
  def URI: String = {
    socket.base.getSocketOptx(ZMQ_LAST_ENDPOINT).asInstanceOf[String]
  }

  /** Get the first message matching message type or none from the receiver Queue in a threadsafe manner
    *
    * @param filterType the MessageType to filter on
    * @return the (optional) Message
    */
  def receive(filterType: Message.Type.Value): Option[Message] = {
    messageReceiverQueue.synchronized {
      messageReceiverQueue.dequeueFirst(_.messageType == filterType)
    }
  }

  /** Put a message in the sender Queue in a threadsafe manner
    *
    * @param message The Message to put in the Queue
    */
  def send(message: Message): Unit = {
    messageSenderQueue.synchronized {
      messageSenderQueue.enqueue(message)
    }
  }

  /** Checks if a client is alive by comparing the last time a message was received and now
    *
    * @param id The clientId to check
    * @param timeout The time difference in seconds deciding if the client is dead
    * @return True if alive and False if not present in the list or
    */
  def checkClientAlive(id: String, timeout: Int = 10): Boolean = {
    val lastTime: Long = lastMessageReceived.synchronized {
      lastMessageReceived.getOrElse(id, 0)
    }

    // no message for 10 seconds? not alive anymore
    (lastTime + timeout) > Util.unixEpoch()
  }
}
