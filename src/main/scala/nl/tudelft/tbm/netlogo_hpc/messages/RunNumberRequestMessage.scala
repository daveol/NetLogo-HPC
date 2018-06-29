package nl.tudelft.tbm.netlogo_hpc.messages

import org.sim0mq.message.SimulationMessage

/**
  *
  * @param simulationRunId
  * @param senderId
  * @param receiverId
  * @param messageId
  */
class RunNumberRequestMessage(
  override val simulationRunId: String,
  override val senderId: String,
  override val receiverId: String,
  override val messageId: Long,
) extends Message {

  /**
    *
    */
  override val messageType: Message.Type.Value = Message.Type.RunNumberRequestMessage

  /**
    *
    * @return
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

object RunNumberRequestMessage {
  def decode(data: Array[Object]): RunNumberRequestMessage = {
    val messageType = Message.Type(data(Message.Part.messageTypeId).asInstanceOf[Int])

    if (messageType != Message.Type.RunNumberRequestMessage) throw new IllegalArgumentException(
      s"Incorrect message type: expected ${Message.Type.RunNumberRequestMessage.toString} but got ${messageType.toString}"
    ) // You shouldn't be using this directly anyway, use Message.decode which handles type handling for you

    new RunNumberRequestMessage(
      data(Message.Part.simulationRunId).asInstanceOf[String],
      data(Message.Part.senderId).asInstanceOf[String],
      data(Message.Part.receiverId).asInstanceOf[String],
      data(Message.Part.messageId).asInstanceOf[Long]
    )
  }
}


