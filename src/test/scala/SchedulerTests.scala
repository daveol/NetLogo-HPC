import nl.tudelft.tbm.netlogo_hpc.exception.DiscoveryException
import nl.tudelft.tbm.netlogo_hpc.scheduler.Scheduler
import org.scalatest.{FunSuite, Matchers}

class SchedulerTests extends FunSuite with Matchers {
  test("Scheduler.getScheduler: test exception") {
    assertThrows[DiscoveryException] {
      Scheduler.getScheduler
    }
  }
}
