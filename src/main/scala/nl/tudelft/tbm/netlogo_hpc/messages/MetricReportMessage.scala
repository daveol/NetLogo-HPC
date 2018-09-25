package nl.tudelft.tbm.netlogo_hpc.messages

import org.sim0mq.message.SimulationMessage

/** MetricReportMessage
  *
  * @param simulationRunId The application id to verify the message
  * @param senderId The client id of the client that send it
  * @param receiverId The client id of the receiver
  * @param messageId The id for the message
  * @param runNumber The run number to report the metrics for
  * @param step The step to report the metrics for
  * @param data The metrics to report
  */
class MetricReportMessage(
  override val simulationRunId: String,
  override val senderId: String,
  override val receiverId: String,
  override val messageId: Long,
  val runNumber: Int,
  val step: Int,
  val data: List[AnyRef]
) extends Message {

  /** The message type for MetricReportMessage
    */
  override val messageType: Message.Type.Value = Message.Type.MetricReportMessage

  /** Encode the MetricReportMessage
    *
    * @return An array of bytes
    */
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

//noinspection ZeroIndexToHead
object MetricReportMessage {
  /** Decode the MetricReportMessage
    *
    * @param data The data to decode
    * @return A object of MetricReportMessage
    */
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
