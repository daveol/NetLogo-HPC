package nl.tudelft.tbm.netlogo_hpc.messages

import nl.tudelft.tbm.netlogo_hpc.exception.DecodingException
import org.sim0mq.message.MessageStatus

/** Represent a message as a class
  */
trait Message {

  /** The ApplicationId of the message
    */
  val simulationRunId: String

  /** The client id of the sender
    */
  val senderId: String

  /** The client id of the receiver
    */
  val receiverId: String

  /** The id of the message
    */
  val messageId: Long

  /** The type of the Message
    */
  val messageType: Message.Type.Value

  /** The status of the message
    */
  val messageStatus: MessageStatus = MessageStatus.NEW

  /** Encode the message to bytes
    *
    * @return An array of bytes
    */
  def encode(): Array[Byte]
}

object Message {

  /** Decodes messages from objects
    *
    * @param data A array of objects
    * @return A message object
    */
  def decode(data: Array[Object]): Message = {
    Message.Type(data(Message.Part.messageTypeId).asInstanceOf[Int]) match {
      case Message.Type.RunNumberRequestMessage => return RunNumberRequestMessage.decode(data)
      case Message.Type.RunNumberReplyMessage => return RunNumberReplyMessage.decode(data)
      case Message.Type.MetricReportMessage => return MetricReportMessage.decode(data)
      case Message.Type.KeepAliveMessage => return KeepAliveMessage.decode(data)
    }
    throw new DecodingException("Message.Type is not valid")
  }

  /** Enumeration of message types
    */
  object Type extends Enumeration {
    /** The type for KeepAliveMessage
      */
    val KeepAliveMessage: Type.Value = Value

    /** The type for RunNumberRequestMessage
      */
    val RunNumberRequestMessage: Type.Value = Value

    /** The type for RunNumberReplyMessage
      */
    val RunNumberReplyMessage: Type.Value = Value

    /** The type for MetricReportMessage
      */
    val MetricReportMessage: Type.Value = Value

    /** The type for StatusReportMessage
      */
    val StatusReportMessage: Type.Value = Value
  }

  /** Constants for TypedMessage parts
    */
  object Part {
    /**
      * TypedMessageVersion field
      */
    val typedMessageVersion: Int = 0

    /**
      * simulationRunId field
      */
    val simulationRunId: Int = 1

    /**
      * senderId field
      */
    val senderId: Int = 2

    /**
      * receiverId field
      */
    val receiverId: Int = 3

    /**
      * messageTypeId field
      */
    val messageTypeId: Int = 4

    /**
      * messageId field
      */
    val messageId: Int = 5

    /**
      * messageStatus field
      */
    val messageStatus: Int = 6

    /**
      * contentLength field
      */
    val contentLength: Int = 7

    /**
      * contentStart the start of content fields
      */
    val contentStart: Int = 8
  }

}
