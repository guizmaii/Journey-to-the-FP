package com.guizmaii.cats_typeclasses

import cats.Show
import cats.effect.internals.IOContextShift
import com.guizmaii.BaseTestSuite
import monix.eval.Task
import monix.execution.schedulers.TestScheduler

import scala.collection.concurrent.TrieMap
import scala.concurrent.{ExecutionContext, Future}

sealed trait EffectSystem extends Product with Serializable
case object Monix         extends EffectSystem
case object CatsEffect    extends EffectSystem

object EffectSystem {
  implicit final val show: Show[EffectSystem] = {
    case Monix      => "Monix"
    case CatsEffect => "Cats"
  }
}

/**
  * Everything is inspired by (actually, it's almost only copied from): https://gist.github.com/kevinmeredith/1812c2bc221a5adc8bae0f29104cc4e5
  *
  * which comes from a discussion of Twitter: https://twitter.com/Gentmen/status/1001135709972025345
  */
object AsyncSuite extends BaseTestSuite {

  test("true should be true") { _ =>
    assertEquals(true, true)
  }

  import cats.effect._
  import cats.implicits._
  import cats.temp.par._

  final val concurrentMap: TrieMap[String, String] = TrieMap.empty[String, String]

  object AIO {
    def apply[F[_]: Sync](): F[Unit] = Sync[F].delay { concurrentMap.update(Thread.currentThread().getName, "") }
  }

  override def tearDown(env: TestScheduler): Unit = {
    concurrentMap.clear()
    super.tearDown(env)
  }

  def launchTest[F[_]: Sync: Par: Timer](
      effectSystem: EffectSystem
  )(runAsync: F[_] => Future[Unit])(implicit show: Show[EffectSystem]): Future[Unit] = {

    val f = (0 to 500).toList.parTraverse(_ => AIO.apply())

    assertEquals(concurrentMap.isEmpty, true)

    runAsync {
      f.map { _ =>
        concurrentMap.foreach(tp => println(s"---> ${effectSystem.show} $tp"))
        println(s"number of thread used: ${concurrentMap.size}")
        println()

        // This test is here to verify that more than one thread has been used.
        // So, that the programe is parallelized.
        //
        // After observation, it turns out that:
        //
        // 1. With 500 `AIO`:
        //  - Monix and Cats use at most `n` threads. On my 8 cores machine, sometimes it uses 6, sometimes 7, sometimes 8 but rarely 8.
        //
        // 2. With 10.000 `AIO`:
        //  - Monix and Cats use `n` threads
        //
        // `n` being the numbers of cores on your machine.
        //
        // I think that the fact that `n` has a relation with number of cores of the machine
        // is because they both use the Scala default EC, which is booted with `n` threads.
        //
        assert(concurrentMap.size > 1 && concurrentMap.size <= Runtime.getRuntime.availableProcessors())
      }
    }
  }

  testAsync("With Monix Task") { _ =>
    import monix.execution.Scheduler.Implicits.global

    launchTest[Task](Monix)(_.runAsync.asInstanceOf[Future[Unit]])
  }

  testAsync("With Cats IO") { _ =>
    val globalEC: ExecutionContext = ExecutionContext.global

    implicit val ctx: ContextShift[IO] = IOContextShift.apply(globalEC)
    implicit val timer: Timer[IO]      = IO.timer(globalEC)

    launchTest[IO](CatsEffect)(_.unsafeToFuture.asInstanceOf[Future[Unit]])
  }

}
