package com.guizmaii.gadts

object GADTs extends App {

  sealed trait Expr[A]
  final case class I(v: Int)                         extends Expr[Int]
  final case class B(v: Boolean)                     extends Expr[Boolean]
  final case class Add(e1: Expr[Int], e2: Expr[Int]) extends Expr[Int]
  final case class Mul(e1: Expr[Int], e2: Expr[Int]) extends Expr[Int]
  final case class Eq(e1: Expr[Int], e2: Expr[Int])  extends Expr[Boolean]

  object Expr {
    implicit final class ExprOps(private val e1: Expr[Int]) extends AnyVal {
      def +(e2: Expr[Int]): Expr[Int]       = Add(e1, e2)
      def *(e2: Expr[Int]): Expr[Int]       = Mul(e1, e2)
      def ===(e2: Expr[Int]): Expr[Boolean] = Eq(e1, e2)
    }
  }
  import Expr._

  final def eval[A](expr: Expr[A]): A =
    expr match {
      case I(v)        => v
      case B(v)        => v
      case Add(e1, e2) => eval(e1) + eval(e2)
      case Mul(e1, e2) => eval(e1) * eval(e2)
      case Eq(e1, e2)  => eval(e1) == eval(e2)
    }

  assert(eval(I(1)) == 1)
  assert(eval(I(1) === I(1)))
  assert(eval(I(3) + I(2) * I(2) === I(7)))

}
