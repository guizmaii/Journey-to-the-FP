package com.guizmaii.optimizing.finalz.tagless
import monix.eval.Task

/**
  * Study of https://typelevel.org/blog/2017/12/27/optimizing-final-tagless.html
  */
object Part1 extends App {

  import cats._
  import cats.data._
  import cats.implicits._

  trait KVStore[F[_]] {
    def get(key: String): F[Option[String]]
    def put(key: String, a: String): F[Unit]
  }

  def program_0[M[_]: FlatMap, F[_]](a: String)(K: KVStore[M])(implicit P: Parallel[M, F]): M[Option[String]] =
    for {
      _ <- K.put("A", a)
      x <- (K.get("B"), K.get("C")).parMapN(_ |+| _)
      _ <- K.put("X", x.getOrElse("-"))
    } yield x

  def program_1[F[_]: Apply](F: KVStore[F]): F[List[String]] =
    (F.get("Cats"), F.get("Dogs"), F.put("Mice", "42"), F.get("Cats"))
      .mapN((f, s, _, t) => List(f, s, t).flatten)

  val analysisInterpreter: KVStore[Const[(Set[String], Map[String, String]), ?]] =
    new KVStore[Const[(Set[String], Map[String, String]), ?]] {
      def get(key: String)            = Const((Set(key), Map.empty))
      def put(key: String, a: String) = Const((Set.empty, Map(key -> a)))
    }

  val optimized: Const[(Set[String], Map[String, String]), List[String]] = program_1(analysisInterpreter)

  def optimizedProgram_0[F[_]: Applicative](F: KVStore[F]): F[List[String]] = {
    val (gets, puts) = optimized.getConst

    puts.toList.traverse { case (k, v) => F.put(k, v) } *> gets.toList.traverse(F.get).map(_.flatten)
  }

  def program_2[F[_]: Apply](mouse: String)(F: KVStore[F]): F[List[String]] =
    (F.get("Cats"), F.get("Dogs"), F.put("Mice", mouse), F.get("Cats"))
      .mapN((f, s, _, t) => List(f, s, t).flatten)

  def optimizedProgram_1[F[_]: Applicative](mouse: String)(F: KVStore[F]): F[List[String]] = {
    val (gets, puts) = program_2(mouse)(analysisInterpreter).getConst

    puts.toList.traverse { case (k, v) => F.put(k, v) } *> gets.toList.traverse(F.get).map(_.flatten)
  }

  def monadicProgram[F[_]: Monad](F: KVStore[F]): F[Unit] =
    for {
      mouse <- F.get("Mice")
      list  <- optimizedProgram_1(mouse.getOrElse("64"))(F)
      _     <- F.put("Birds", list.headOption.getOrElse("128"))
    } yield ()

  /*
  type Program_0[Algebra[_[_]], F[_], A] = Algebra[F] => F[A]

  def optimize[Algebra[_[_]], F[_]: Applicative, A, M: Monoid](
      program: Algebra[F] => F[A]
  )(extract: Algebra[Const[M, ?]])(restructure: M => F[A]): Algebra[F] => F[A] = { interp =>
    val m = program(extract).getConst // error: type mismatch;
    // found   : extract.type (with underlying type Algebra[[β$0$]cats.data.Const[M,β$0$]])
    // required: Algebra[F]

    restructure(m)
  }
   */

  trait Program_1[Alg[_[_]], A] {
    def apply[F[_]: Applicative](interpreter: Alg[F]): F[A]
  }

  def optimize[Alg[_[_]], F[_]: Applicative, A, M: Monoid](
      program: Program_1[Alg, A]
  )(extract: Alg[Const[M, ?]])(restructure: M => F[A]): Alg[F] => F[A] = { interp =>
    val m = program(extract).getConst

    restructure(m)
  }

  def wrappedProgram(mouse: String): Program_1[KVStore, List[String]] =
    new Program_1[KVStore, List[String]] {
      def apply[F[_]: Applicative](alg: KVStore[F]): F[List[String]] = program_2(mouse)(alg)
    }

  def optimizedProgram[F[_]: Applicative](mouse: String)(F: KVStore[F]): KVStore[F] => F[List[String]] =
    optimize(wrappedProgram(mouse))(analysisInterpreter) {
      case (gets, puts) =>
        puts.toList.traverse { case (k, v) => F.put(k, v) } *> gets.toList.traverse(F.get).map(_.flatten)
    }

  trait Optimizer[Algebra[_[_]], F[_]] {
    type M

    def monoidM: Monoid[M]
    def monadF: Monad[F]

    def extract: Algebra[Const[M, ?]]
    def rebuild(m: M, interpreter: Algebra[F]): F[Algebra[F]]

    def optimize[A](p: Program_1[Algebra, A]): Algebra[F] => F[A] = { interpreter =>
      implicit val M: Monoid[M] = monoidM
      implicit val F: Monad[F]  = monadF

      val m: M = p(extract).getConst

      rebuild(m, interpreter).flatMap(p(_))
    }
  }

  implicit final class OptimizerOps[Alg[_[_]], A](val value: Program_1[Alg, A]) extends AnyVal {
    def optimize[F[_]: Monad](interp: Alg[F])(implicit O: Optimizer[Alg, F]): F[A] =
      O.optimize(value)(interp)
  }

  def monadicProgram_2[F[_]: Monad](F: KVStore[F])(implicit O: Optimizer[KVStore, F]): F[Unit] =
    for {
      mouse <- F.get("Mice")
      list  <- wrappedProgram(mouse.getOrElse("64")).optimize(F)
      _     <- F.put("Birds", list.headOption.getOrElse("128"))
    } yield ()

  implicit val kvStoreTaskOptimizer: Optimizer[KVStore, Task] =
    new Optimizer[KVStore, Task] {
      type M = Set[String]

      def monoidM = implicitly

      def monadF = implicitly

      def extract =
        new KVStore[Const[Set[String], ?]] {
          def get(key: String)                                      = Const(Set(key))
          def put(key: String, a: String): Const[Set[String], Unit] = Const(Set.empty)
        }

      def rebuild(gs: Set[String], interp: KVStore[Task]): Task[KVStore[Task]] =
        gs.toList
          .parTraverse(key => OptionT(interp.get(key)).map(s => (key, s)).value)
          .map(_.flattenOption.toMap)
          .map { map =>
            new KVStore[Task] {
              override def get(key: String) =
                map.get(key) match {
                  case v @ Some(_) => v.pure[Task]
                  case None        => interp.get(key)
                }

              def put(key: String, a: String): Task[Unit] = interp.put(key, a)
            }
          }
    }

}
