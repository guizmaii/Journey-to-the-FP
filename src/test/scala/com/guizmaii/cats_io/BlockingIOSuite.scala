package com.guizmaii.cats_io

import cats.effect.IO
import com.guizmaii.BaseTestSuite
import com.guizmaii.utils.ScalaUtils.globalExecutionThreadPoolName
import monix.execution.schedulers.TestScheduler

import scala.collection.concurrent.TrieMap

object BlockingIOSuite extends BaseTestSuite {

  import cats.syntax.apply._

  import scala.concurrent.ExecutionContext.Implicits.global

  test("true should be true") { _ =>
    assertEquals(true, true)
  }

  object BlockingIO {
    @inline final def apply[A](a: => A): IO[A] = IO.shift(_root_.monix.execution.Scheduler.io()) *> IO(a) <* IO.shift
  }

  final val concurrentMap: TrieMap[String, String] = TrieMap.empty[String, String]

  @inline final def body(name: String): Unit =
    concurrentMap.update(name, Thread.currentThread().getName)

  override def tearDown(env: TestScheduler): Unit = {
    concurrentMap.clear()
    super.tearDown(env)
  }

  testAsync("IoTask should execute its code on its Scheduler") { _ =>
    val f =
      IO.apply { body("a") }
        .flatMap(_ => BlockingIO { body("b") })
        .flatMap(_ => IO.pure { body("c") })
        .flatMap(_ => IO.apply { body("d") })

    assertEquals(concurrentMap.isEmpty, true)

    f.unsafeToFuture.map { _ =>
      assert(concurrentMap("a").contains(globalExecutionThreadPoolName))
      assert(concurrentMap("b").contains("monix-io"))
      assert(concurrentMap("c").contains(globalExecutionThreadPoolName))
      assert(concurrentMap("d").contains(globalExecutionThreadPoolName))
    }
  }

}
