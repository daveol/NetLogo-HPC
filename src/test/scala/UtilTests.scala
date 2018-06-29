import nl.tudelft.tbm.netlogo_hpc.Util
import nl.tudelft.tbm.netlogo_hpc.exception.MissingBinaryException
import org.scalatest.{FunSuite, Matchers}

/**
  * Test Utility class nl.tudelft.tbm.netlogo.hpc.Util
  */
class UtilTests extends FunSuite with Matchers {

  test("Util.which: test for java in path") {
    Util.which("java")
  }

  test("Util.which: test if which throws MissingBinaryException") {
    assertThrows[MissingBinaryException] {
      Util.which("this-should-be-invalid")
    }
  }
}
