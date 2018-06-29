package nl.tudelft.tbm.netlogo_hpc.messages

import org.sim0mq.message.SimulationMessage

class MetricReportMessage(
  override val simulationRunId: String,
  override val senderId: String,
  override val receiverId: String,
  override val messageId: Long,
  val runNumber: Int,
  val step: Int,
  val data: List[AnyRef]
) extends Message {

  override val messageType: Message.Type.Value = Message.Type.MetricReportMessage

  override def encode(): Array[Byte] = {

    val mdata: List[Any] = this.runNumber :: this.step :: this.data

    SimulationMessage.encodeUTF8(
      this.simulationRunId,
      this.senderId,
      this.receiverId,
      this.messageType.id,
      this.messageId,
      this.messageStatus,
      mdata.asInstanceOf[List[AnyRef]]: _* // submit list as arguments (flat)
    )
  }
}

object MetricReportMessage {
  def decode(data: Array[Object]): MetricReportMessage = {
    val messageType = Message.Type(data(Message.Part.messageTypeId).asInstanceOf[Int])

    // You shouldn't be using this directly anyway, use Message.decode which looks at the type for you
    if (messageType != Message.Type.MetricReportMessage) throw new IllegalArgumentException(
      s"Incorrect message type: expected ${Message.Type.MetricReportMessage.toString} but got ${messageType.toString}"
    )

    val content: List[Any] = List(data.drop(Message.Part.contentStart): _*)

    new MetricReportMessage(
      data(Message.Part.simulationRunId).asInstanceOf[String],
      data(Message.Part.senderId).asInstanceOf[String],
      data(Message.Part.receiverId).asInstanceOf[String],
      data(Message.Part.messageId).asInstanceOf[Long],
      content(0).asInstanceOf[Int],
      content(1).asInstanceOf[Int],
      content.asInstanceOf[List[AnyRef]].drop(2)
    )
  }
}
