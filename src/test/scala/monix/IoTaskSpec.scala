package monix

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}

class IoTaskSpec extends WordSpec with Matchers with ScalaFutures with BeforeAndAfterEach {

  "true" should {
    "be true" in {
      true shouldBe true
    }
  }

}
