package com.guizmaii.monix

import monix.eval.Task
import monix.execution.Scheduler
import monix.execution.schedulers.TestScheduler

import scala.collection.concurrent.TrieMap

object IoTaskSuite extends BaseTestSuite {

  test("true should be true") { _ =>
    assertEquals(true, true)
  }

  implicit val global: Scheduler = _root_.monix.execution.Scheduler.global
  val io                         = _root_.monix.execution.Scheduler.io()

  /**
    * Thanks to Piotr Gawryś (@Avasil) for this !
    */
  object IoTask {
    @inline final def apply[A](a: => A): Task[A] = Task.eval(a).executeOn(io).asyncBoundary
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
      Task
        .apply { body("a") }
        .flatMap(_ => IoTask { body("b") })
        .flatMap(_ => Task.eval { body("c") })
        .flatMap(_ => Task.apply { body("d") })

    assertEquals(concurrentMap.isEmpty, true)

    f.runAsync.map { _ =>
      assert(concurrentMap("a").contains("scala-execution-context-global"))
      assert(concurrentMap("b").contains("monix-io"))
      assert(concurrentMap("c").contains("scala-execution-context-global"))
      assert(concurrentMap("d").contains("scala-execution-context-global"))
    }
  }

}
