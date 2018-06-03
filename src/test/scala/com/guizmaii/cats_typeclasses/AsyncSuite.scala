package com.guizmaii.cats_typeclasses

import cats.Show
import com.guizmaii.BaseTestSuite
import monix.eval.Task
import monix.execution.schedulers.TestScheduler

import scala.collection.concurrent.TrieMap
import scala.concurrent.Future

sealed trait EffectSystem extends Product with Serializable
case object Monix         extends EffectSystem
case object CatsEffect    extends EffectSystem

object EffectSystem {
  implicit final val show: Show[EffectSystem] = new Show[EffectSystem] {
    override def show(e: EffectSystem): String = e match {
      case Monix      => "Monix"
      case CatsEffect => "Cats"
    }
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
    // TODO Jules: Can we simplify this part ?
    def apply[F[_]]()(implicit F: Async[F], timer: Timer[F]): F[Unit] =
      timer.shift >> F.async[Unit] { cb =>
        cb(Right(concurrentMap.update(Thread.currentThread().getName, "")))
      }
  }

  override def tearDown(env: TestScheduler): Unit = {
    concurrentMap.clear()
    super.tearDown(env)
  }

  def launchTest[F[_]: Async: Par: Timer](
      effectSystem: EffectSystem
  )(runAsync: F[_] => Future[Unit])(implicit show: Show[EffectSystem]): Future[Unit] = {

    // The `.parTraverse` is important here to parallelize the calls.
    //
    // But, if you replace it by its sequential equivalent, `.traverse`, the execution will also use more than 1 thread.
    // The difference seems to be that with `.parTraverse` more threads will be used.
    //
    // TODO: Does the execution parralelized even whitout the `.parTraverse` ? To Check !
    //
    val f = (0 to 500).toList.parTraverse(_ => AIO.apply())

    assertEquals(concurrentMap.isEmpty, true)

    runAsync {
      f.map { _ =>
        concurrentMap.foreach(tp => println(s"---> ${effectSystem.show} $tp"))
        println("")

        // This test is here to verify that more than one thread has been used.
        // So, that the programe is parallelized.
        //
        // After observation, it turns out that:
        //
        // 1. With 500 `AIO`:
        //  - Monix uses `n` threads. Always (or it looks like always).
        //  - Cats uses at most `n` threads. On my 8 cores machine, sometimes it uses 6, sometimes 7, sometimes 8 but rarely 8.
        //
        // 2. With 10.000 `AIO`:
        //  - Monix and Cats uses `n` threads
        //
        // `n` being the numbers of cores on your machine.
        //
        // I think that the fact that `n` has a relation with number of cores of the machine
        // is because they both use the Scala default EC, which is booted with `n` threads.
        //
        // TODO: For now, I don't know why Cats seems to use less thread than Cats in some cases. To Check !
        //
        effectSystem match {
          case Monix =>
            assert(concurrentMap.size == Runtime.getRuntime.availableProcessors())
          case CatsEffect =>
            assert(concurrentMap.size > 1 && concurrentMap.size <= Runtime.getRuntime.availableProcessors())
        }
      }
    }
  }

  testAsync("With Monix Task") { _ =>
    import monix.execution.Scheduler.Implicits.global

    launchTest[Task](Monix)(_.runAsync.asInstanceOf[Future[Unit]])
  }

  testAsync("With Cats IO") { _ =>
    import scala.concurrent.ExecutionContext.Implicits.global

    launchTest[IO](CatsEffect)(_.unsafeToFuture.asInstanceOf[Future[Unit]])
  }

}
