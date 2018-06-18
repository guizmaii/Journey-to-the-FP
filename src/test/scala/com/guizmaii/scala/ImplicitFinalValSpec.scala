package com.guizmaii.scala

import com.guizmaii.BaseTestSuite
import monix.execution.Scheduler

import scala.collection.concurrent.TrieMap

final case class FinalValIo(scheduler: Scheduler) extends AnyVal
final case class ValIo(scheduler: Scheduler)      extends AnyVal

object ImplicitFinalValSpec extends BaseTestSuite {

  def doNothing(): Runnable = () => ()

  final val countFinalVals: TrieMap[String, Int] = TrieMap.empty[String, Int]
  final val countVals: TrieMap[String, Int]      = TrieMap.empty[String, Int]
  final val key                                  = "key"

  def finalValIo(): FinalValIo = {
    countFinalVals.put(key, countFinalVals.get(key).fold(1)(_ + 1))

    FinalValIo(Scheduler.io())
  }

  def valIo(): ValIo = {
    countVals.put(key, countVals.get(key).fold(1)(_ + 1))

    ValIo(Scheduler.io())
  }

  object FinalVal {
    implicit final val v: FinalValIo = finalValIo()
  }

  object Val {
    implicit val v: ValIo = valIo()
  }

  def fv_A()(implicit fv: FinalValIo) = fv.scheduler.execute(doNothing())
  def fv_B()(implicit fv: FinalValIo) = fv.scheduler.execute(doNothing())
  def fv_C()(implicit fv: FinalValIo) = fv.scheduler.execute(doNothing())

  def v_A()(implicit v: ValIo) = v.scheduler.execute(doNothing())
  def v_B()(implicit v: ValIo) = v.scheduler.execute(doNothing())
  def v_C()(implicit v: ValIo) = v.scheduler.execute(doNothing())

  test("count the number of FinalValIo instanciated") { _ =>
    import FinalVal._

    fv_A()
    fv_B()
    fv_C()

    assertEquals(countFinalVals(key), 1)
  }
  test("count the number of ValIo instanciated") { _ =>
    import Val._

    v_A()
    v_B()
    v_C()

    assertEquals(countVals(key), 1)
  }

}
