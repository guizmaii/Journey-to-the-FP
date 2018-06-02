package future

import java.util.concurrent.ConcurrentHashMap

import scala.collection.immutable.IndexedSeq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{Await, Future}

object CountThreadUsedInAsync extends App {

  val map: ConcurrentHashMap[String, Boolean] =
    new ConcurrentHashMap[String, Boolean]

  def xDurationOfWork(x: Duration)(i: Int): Future[Unit] =
    Future {
      Thread.sleep(x.toMillis)
      map.put(Thread.currentThread().getName, true)
      println(s"A$i: ${Thread.currentThread().getName}")
    }.map { _ =>
        map.put(Thread.currentThread().getName, true);
        println(s"B$i: ${Thread.currentThread().getName}")
      }
      .flatMap(
        _ =>
          Future {
            map.put(Thread.currentThread().getName, true)
            println(s"C$i: ${Thread.currentThread().getName}")
          }
      )
      .map { _ =>
        map.put(Thread.currentThread().getName, true);
        println(s"D$i: ${Thread.currentThread().getName}")
      }

  def tasks: Future[IndexedSeq[Unit]] =
    Future.sequence((0 to 100) map xDurationOfWork(1.seconds))

  Await.result(tasks, 10000.seconds)
  println(s"Number of different threads used: ${map.size()}")

}
