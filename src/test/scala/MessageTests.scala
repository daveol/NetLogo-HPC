
import nl.tudelft.tbm.netlogo_hpc.messages._
import org.scalatest.{FunSuite, Matchers}
import org.sim0mq.message.SimulationMessage

import scala.util.Random

class MessageTests extends FunSuite with Matchers {

  val simulationRunId = "testSimRunId"
  val senderId = "testSendId"
  val receiverId = "testReceiveId"

  test("MetricReportMessage.encode: Testing encoding a MetricReportMessage") {
    //noinspection ScalaRedundantCast
    val list: List[Any] = List(
      "test",
      't',
      1.asInstanceOf[Int],
      1.asInstanceOf[Float],
      1.asInstanceOf[Double],
      1.asInstanceOf[Long],
      true
    )

    val message = new MetricReportMessage(
      simulationRunId,
      senderId,
      receiverId,
      0,
      0,
      0,
      list.asInstanceOf[List[AnyRef]]
    )

    message.encode()
  }

  test("RunNumberRequestMessage.encode: Testing encoding a RunNumberRequest") {
    val message = new RunNumberRequestMessage(
      simulationRunId,
      senderId,
      receiverId,
      1
    )

    message.encode()
  }

  test("RunNumberReplyMessage.encode: Testing encoding a RunNumberRequest") {
    val message = new RunNumberReplyMessage(
      simulationRunId,
      senderId,
      receiverId,
      2,
      Random.nextInt()
    )

    message.encode()
  }

  test("Message.decode: Test decoding a RunNumberRequest") {
    val data_message: Array[Byte] = new RunNumberRequestMessage(
      simulationRunId,
      senderId,
      receiverId,
      3
    ).encode()

    val message: Message = Message.decode(SimulationMessage.decode(data_message))

    // Tests
    assert(message.receiverId == receiverId)
    assert(message.senderId == senderId)
    assert(message.messageType == Message.Type.RunNumberRequestMessage)
  }

  test("Message.decode: Test decoding a RunNumberReply") {
    val runNumber = 1
    val data_message: Array[Byte] = new RunNumberReplyMessage(
      simulationRunId,
      senderId,
      receiverId,
      3,
      runNumber
    ).encode()

    val message: Message = Message.decode(SimulationMessage.decode(data_message))

    // Test Message
    assert(message.receiverId == receiverId)
    assert(message.senderId == senderId)
    assert(message.messageType == Message.Type.RunNumberReplyMessage)

    // Test runNumber
    assert(message.asInstanceOf[RunNumberReplyMessage].runNumber == runNumber)
  }

  test("Message.decode: Test decoding a MetricReportMessage") {
    val data: List[Any] = List(
      "test",
      't',
      1.asInstanceOf[Int],
      1.asInstanceOf[Float],
      1.asInstanceOf[Double],
      1.asInstanceOf[Long],
      true
    )

    val data_message: Array[Byte] = new MetricReportMessage(
      simulationRunId,
      senderId,
      receiverId,
      4,
      0,
      0,
      data.asInstanceOf[List[AnyRef]]
    ).encode()

    val message: Message = Message.decode(SimulationMessage.decode(data_message))

    // Test Message
    assert(message.receiverId == receiverId)
    assert(message.senderId == senderId)
    assert(message.messageType == Message.Type.MetricReportMessage)

    // Test data
    val metric = message.asInstanceOf[MetricReportMessage]
    assert(metric.runNumber == 0)
    assert(metric.step == 0)
    assert(metric.data == data)
  }
}
