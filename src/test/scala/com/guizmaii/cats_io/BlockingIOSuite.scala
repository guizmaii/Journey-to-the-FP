package com.guizmaii.cats_io

import cats.effect.{Async, IO, Timer}
import com.guizmaii.BaseTestSuite
import com.guizmaii.utils.ScalaUtils.globalExecutionThreadPoolName
import monix.execution.Scheduler
import monix.execution.schedulers.TestScheduler

import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext

object BlockingIOSuite extends BaseTestSuite {

  test("true should be true") { _ =>
    assertEquals(true, true)
  }

  import scala.concurrent.ExecutionContext.Implicits.global

  val ioEC: Scheduler = _root_.monix.execution.Scheduler.io()

  object ShiftedIO {

    /**
      * More info, see thread: https://twitter.com/guizmaii/status/1022819979606061058
      */
    @inline final def apply[F[_], A](ec: ExecutionContext)(a: => A)(implicit F: Async[F], timer: Timer[F]): F[A] =
      F.bracket(Async.shift(ec))(_ => F.delay(a))(_ => timer.shift)
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
        .flatMap(_ => ShiftedIO[IO, Unit](ioEC) { body("b") })
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
