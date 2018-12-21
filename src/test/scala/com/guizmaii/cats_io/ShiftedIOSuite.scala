package com.guizmaii.cats_io

import com.guizmaii.BaseTestSuite
import com.guizmaii.utils.ScalaUtils.globalExecutionThreadPoolName
import monix.execution.Scheduler
import monix.execution.schedulers.TestScheduler

import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext

object ShiftedIOSuite extends BaseTestSuite {

  test("true should be true") { _ =>
    assertEquals(true, true)
  }

  import cats.effect._

  val globalEC: ExecutionContext = ExecutionContext.global
  val ioEC: Scheduler            = _root_.monix.execution.Scheduler.io()

  implicit val ctx: ContextShift[IO] = IO.contextShift(globalEC)

  object ShiftedIO {
    @inline final def apply[A](ec: ExecutionContext)(a: => A)(implicit cs: ContextShift[IO]): IO[A] = cs.evalOn(ec)(IO(a))
  }

  final val concurrentMap: TrieMap[String, String] = TrieMap.empty[String, String]

  @inline final def body(name: String): Unit =
    concurrentMap.update(name, Thread.currentThread().getName)

  override def tearDown(env: TestScheduler): Unit = {
    concurrentMap.clear()
    super.tearDown(env)
  }

  testAsync("BlockingIO should execute its code on its Scheduler") { _ =>
    val f =
      IO.apply { body("a") }
        .flatMap(_ => ShiftedIO(ioEC) { body("b") })
        .flatMap(_ => IO.pure { body("c") })
        .flatMap(_ => IO.apply { body("d") })

    assertEquals(concurrentMap.isEmpty, true)

    f.unsafeToFuture.map { _ =>
      assert(concurrentMap("a").contains(globalExecutionThreadPoolName))
      assert(concurrentMap("b").contains("monix-io"))
      assert(concurrentMap("c").contains(globalExecutionThreadPoolName))
      assert(concurrentMap("d").contains(globalExecutionThreadPoolName))
    }(globalEC)
  }

}
