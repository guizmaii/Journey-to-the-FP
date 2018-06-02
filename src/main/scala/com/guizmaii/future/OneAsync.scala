package com.guizmaii.future

import java.util.concurrent.Executor

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

object OneAsync extends App {

  implicit val synchronousExecutionContext: ExecutionContext =
    ExecutionContext.fromExecutor(new Executor {
      def execute(task: Runnable): Unit = task.run()
    })

  def xSecondsOfWorks(x: Int)(i: Int): Future[Unit] = {
    println(s"Before: ${Thread.currentThread().getName} - $i")
    val before = System.currentTimeMillis()
    Future {
      Thread.sleep(x.seconds.toMillis)
    }.map { _ =>
      val after = System.currentTimeMillis()
      val millisPassed: Duration =
        (after.milliseconds - before.milliseconds).toSeconds.seconds
      println(
        s"After : ${Thread.currentThread().getName} - $i - Time passed: $millisPassed"
      )
    }
  }

  println(s"BEFORE ALL: ${Thread.currentThread().getName}")
  val before                         = System.currentTimeMillis()
  val work: () => Future[List[Unit]] = () => Future.sequence((0 to 9).toList map xSecondsOfWorks(1))
  val done = work().map(_ => {
    val after = System.currentTimeMillis()
    val millisPassed: Duration =
      (after.milliseconds - before.milliseconds).toSeconds.seconds
    println(
      s"AFTER ALL : ${Thread.currentThread().getName} - Total time passed: $millisPassed"
    )
  })

  Await.result(done, 1000.seconds)

}
