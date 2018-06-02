package com.guizmaii.cats_typeclasses

import com.guizmaii.BaseTestSuite
import monix.eval.Task
import monix.execution.schedulers.TestScheduler

import scala.collection.concurrent.TrieMap
import scala.concurrent.Future
import scala.concurrent.duration._

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
    def apply[F[_]](duration: FiniteDuration)(implicit F: Concurrent[F], timer: Timer[F]) =
      timer.sleep(duration) >> F.async[Unit] { cb =>
        cb(Right(concurrentMap.update(Thread.currentThread().getName, "")))
      }
  }

  override def tearDown(env: TestScheduler): Unit = {
    concurrentMap.clear()
    super.tearDown(env)
  }

  def launchTest[F[_]: Concurrent: Par: Timer](name: String)(runAsync: F[_] => Future[Unit]): Future[Unit] = {
    // The `.parTraverse` is crucial here to parallelize the calls.
    // If you replace it by its sequential equivalent, `.traverse`, the execution will take 30 x 1 second to finish.
    val f = (0 to 500).toList.parTraverse(_ => AIO.apply(1.second))

    assertEquals(concurrentMap.isEmpty, true)

    runAsync {
      f.map { _ =>
        concurrentMap.foreach(tp => println(s"---> $name $tp"))
        println("")

        // This test is here to verify that more than one thread has been used.
        //
        // After observation, it turns out that both Cats and Monix use n threads, n being the numbers of cores on your machine.
        // I think that it's because they both use the Scala default EC which is booted with n threads.
        assert(concurrentMap.size == Runtime.getRuntime.availableProcessors())
      }
    }
  }

  testAsync("With Monix Task") { _ =>
    import monix.execution.Scheduler.Implicits.global

    launchTest[Task]("Monix")(_.runAsync.asInstanceOf[Future[Unit]])
  }

  testAsync("With Cats IO") { _ =>
    import scala.concurrent.ExecutionContext.Implicits.global

    launchTest[IO]("Cats")(_.unsafeToFuture.asInstanceOf[Future[Unit]])
  }

}
