package com.guizmaii.scala

import minitest.TestSuite
import monix.execution.Scheduler
import monix.execution.atomic.AtomicInt

object ImplicitFinalValSpec extends TestSuite[Unit] {

  final val counter: AtomicInt = AtomicInt(0)

  def io(): Scheduler = {
    counter.increment()

    Scheduler.io()
  }

  override def setup(): Unit             = counter.set(0)
  override def tearDown(env: Unit): Unit = ()

  def a()(implicit scheduler: Scheduler): Unit = scheduler.execute(() => ())
  def b()(implicit scheduler: Scheduler): Unit = scheduler.execute(() => ())
  def c()(implicit scheduler: Scheduler): Unit = scheduler.execute(() => ())

  test("count the number of FinalValIo instanciated when `implicit final val` is used") { _ =>
    object FinalVal {
      implicit final val v: Scheduler = io()
    }
    import FinalVal._

    a()
    b()
    c()

    assertEquals(counter.get, 1)
  }

  test("count the number of FinalValIo instanciated when `implicit final def` is used") { _ =>
    object FinalDef {
      implicit final def v: Scheduler = io()
    }
    import FinalDef._

    a()
    b()
    c()

    assertEquals(counter.get, 3)
  }

  test("count the number of ValIo instanciated when `implicit val` is used") { _ =>
    object Val {
      implicit val v: Scheduler = io()
    }
    import Val._

    a()
    b()
    c()

    assertEquals(counter.get, 1)
  }

  test("count the number of ValIo instanciated when `implicit def` is used") { _ =>
    object Def {
      implicit def v: Scheduler = io()
    }
    import Def._

    a()
    b()
    c()

    assertEquals(counter.get, 3)
  }

}
