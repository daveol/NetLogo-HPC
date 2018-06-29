package nl.tudelft.tbm.netlogo_hpc.messages

import org.sim0mq.message.SimulationMessage

class KeepAliveMessage(
  override val simulationRunId: String,
  override val senderId: String,
  override val receiverId: String,
  override val messageId: Long
) extends Message {
  override val messageType: Message.Type.Value = Message.Type.KeepAliveMessage

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
