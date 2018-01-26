package future

import scala.concurrent.duration._

object Sync extends App {

  def bench[T](beforePrint: String)(afterPrint: Duration => String)(f: => T): Unit = {
    println(beforePrint)
    val before = System.currentTimeMillis()
    f
    val after = System.currentTimeMillis()
    val millisPassed: Duration = (after.milliseconds - before.milliseconds).toSeconds.seconds
    println(afterPrint(millisPassed))
  }

  def xSecondsOfWorks(x: Int)(i: Int): Unit = {
    val threadName = Thread.currentThread().getName
    bench(s"Before: $threadName - $i")(millisPassed => s"After : $threadName - $i. Time passed: $millisPassed") {
      Thread.sleep(x.seconds.toMillis)
    }
  }

  bench("BEFORE ALL")(millisPassed => s"AFTER ALL $millisPassed") {
    0 to 9 foreach xSecondsOfWorks(1)
  }
}
