package monix

import monix.eval.Task
import monix.execution.Scheduler

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.{implicitConversions, postfixOps}

object IoTask extends App {

  import Scheduler.Implicits.global

  final val io = Scheduler.io()

  /**
    * Thanks to Piotr Gawryś (@Avasil) for this !
    */
  object IoTask {
    def apply[A](a: => A): Task[A] = Task.eval(a).executeOn(io).asyncBoundary
  }

  final case class User(name: String) extends AnyVal

  def getUserName(id: String): Task[String] = Task {
    println(s"getUserName: ${Thread.currentThread()}")
    "username"
  }

  def blockingGetUser(name: String): Task[User] = IoTask {
    println(s"blockingGetUser: ${Thread.currentThread()}")
    User(name)
  }

  def getName(user: User): Task[String] = Task.eval {
    println(s"getName: ${Thread.currentThread()}")
    user.name
  }

  def getUser(id: String) =
    getUserName(id)
      .flatMap(blockingGetUser)
      .flatMap(getName)
      .flatMap(getUserName)
      .runAsync

  Await.result(getUser("abc"), 10 seconds)

}
