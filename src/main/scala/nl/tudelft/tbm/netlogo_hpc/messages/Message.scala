package nl.tudelft.tbm.netlogo_hpc.messages

import nl.tudelft.tbm.netlogo_hpc.exception.DecodingException
import org.sim0mq.message.MessageStatus

trait Message {
  val simulationRunId: String
  val senderId: String
  val receiverId: String
  val messageId: Long
  val messageType: Message.Type.Value
  val messageStatus: MessageStatus = MessageStatus.NEW

  def encode(): Array[Byte]
}

object Message {
  def decode(data: Array[Object]): Message = {
    Message.Type(data(Message.Part.messageTypeId).asInstanceOf[Int]) match {
      case Message.Type.RunNumberRequestMessage => return RunNumberRequestMessage.decode(data)
      case Message.Type.RunNumberReplyMessage => return RunNumberReplyMessage.decode(data)
      case Message.Type.MetricReportMessage => return MetricReportMessage.decode(data)
      case Message.Type.KeepAliveMessage => return KeepAliveMessage.decode(data)
    }
    throw new DecodingException("Message.Type is not valid")
  }

  object Type extends Enumeration {
    val KeepAliveMessage: Type.Value = Value
    val RunNumberRequestMessage: Type.Value = Value
    val RunNumberReplyMessage: Type.Value = Value
    val MetricReportMessage: Type.Value = Value
    val StatusReportMessage: Type.Value = Value
  }

  /**
    * Constants for TypedMessage parts
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
