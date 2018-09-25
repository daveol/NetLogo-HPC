package nl.tudelft.tbm.netlogo_hpc.messages

import org.sim0mq.message.SimulationMessage

/** KeepAliveMessage
  *
  * @param simulationRunId The application id to verify the message
  * @param senderId The client id of the client that send it
  * @param receiverId The client id of the receiver
  * @param messageId The id for the message
  */
class KeepAliveMessage(
  override val simulationRunId: String,
  override val senderId: String,
  override val receiverId: String,
  override val messageId: Long
) extends Message {

  /** The message type for KeepAliveMessage
    */
  override val messageType: Message.Type.Value = Message.Type.KeepAliveMessage

  /** Encode the message
    *
    * @return An array of bytes
    */
  override def encode(): Array[Byte] = {
    SimulationMessage.encodeUTF8(
      this.simulationRunId,
      this.senderId,
      this.receiverId,
      this.messageType.id,
      this.messageId,
      this.messageStatus
    )
  }
}

object KeepAliveMessage {

  /** Decode to a KeepAliveMessage object
    *
    * @param data The array of objects to decode from
    * @return A KeepAliveMessage object
    */
  def decode(data: Array[Object]): KeepAliveMessage = {
    val messageType = Message.Type(data(Message.Part.messageTypeId).asInstanceOf[Int])

    // You shouldn't be using this directly anyway, use Message.decode which handles type handling for you
    if (messageType != Message.Type.KeepAliveMessage) throw new IllegalArgumentException(
      s"Incorrect message type: expected ${Message.Type.KeepAliveMessage.toString} but got ${messageType.toString}"
    )

    new KeepAliveMessage(
      data(Message.Part.simulationRunId).asInstanceOf[String],
      data(Message.Part.senderId).asInstanceOf[String],
      data(Message.Part.receiverId).asInstanceOf[String],
      data(Message.Part.messageId).asInstanceOf[Long]
    )
  }
}
