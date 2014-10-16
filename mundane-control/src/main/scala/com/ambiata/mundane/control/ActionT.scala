package com.ambiata.mundane.control

import scala.util.control.NonFatal
import scalaz._, Scalaz._, \&/._
import scalaz.effect._

/**
 * A data type for holding computations that can fail with exceptions.
 * This is effectively a ReaderT > ErrorT > WriterT > F stack, with
 * a specialized error type. This particular specializtion handles
 * string/exception based failures and should be used to wrap up unsafe
 * apis (i.e. java code).
 *
 * This specialization exists for a number of reasons:
 *  - Basically because you can't use the stack directly via a type alias
 *    without incurring the wrath of scalac and the inference warlords.
 *  - The formulation lets us plug in a few things together to handle
 *    IO and other values of F, whilst keeping some level of sanity.
 *
 * NOTE: This is specifically formulated to not hit scalac bugzzzzz, change with caution.....
 */
case class ActionT[F[_], W, R, A](runT: R => ResultT[({ type l[a] = WriterT[F, W, a] })#l, A]) {
  def map[B](f: A => B)(implicit W: Monoid[W], F: Functor[F]): ActionT[F, W, R, B] =
    ActionT(r => runT(r).map(f))

  def contramap[B](f: B => R)(implicit W: Monoid[W], F: Functor[F]): ActionT[F, W, B, A] =
    ActionT(r => runT(f(r)))

  def flatMap[B](f: A => ActionT[F, W, R, B])(implicit W: Monoid[W], F: Monad[F]): ActionT[F, W, R, B] =
    ActionT(r => runT(r).flatMap(a => f(a).runT(r)))

  def flatMapError(f: These[String, Throwable] => ActionT[F, W, R, A])(implicit W: Monoid[W], F: Monad[F]): ActionT[F, W, R, A] =
    ActionT(r => runT(r).flatMapError(a => f(a).runT(r)))

  def onResult[B](f: Result[A] => Result[B])(implicit W: Monoid[W], F: Functor[F]): ActionT[F, W, R, B] =
    ActionT(r => runT(r).onResult(f))

  def mapError(f: These[String, Throwable] => These[String, Throwable])(implicit W: Monoid[W], F: Functor[F]): ActionT[F, W, R, A] =
    onResult(_.mapError(f))

  def run(r: R): F[(W, Result[A])] =
    runT(r).run.run

  def execute(r: R)(implicit F: Functor[F]): F[Result[A]] =
    run(r).map({ case (w, a) => a })

  def executeT(r: R)(implicit F: Functor[F]): ResultT[F, A] =
    ResultT(execute(r))

  def |||(otherwise: => ActionT[F, W, R, A])(implicit W: Monoid[W], F: Monad[F]): ActionT[F, W, R, A] =
    ActionT[F, W, R, A](r => runT(r) ||| otherwise.runT(r))

  def orElse(otherwise: => A)(implicit W: Monoid[W], F: Monad[F]): ActionT[F, W, R, A] =
    |||(ActionT.ok[F, W, R, A](otherwise))
}

object ActionT extends ActionTLowPriority {
  def ask[F[_]: Monad, W: Monoid, R]: ActionT[F, W, R, R] =
    reader(identity)

  def reader[F[_]: Monad, W: Monoid, R, A](f: R => A): ActionT[F, W, R, A] =
    ActionT(r => ResultT.safe[({ type l[a] = WriterT[F, W, a] })#l, A](f(r)))

  def result[F[_]: Monad, W: Monoid, R, A](f: R => Result[A]): ActionT[F, W, R, A] =
    ActionT(r => ResultT.result[({ type l[a] = WriterT[F, W, a] })#l, A](f(r)))

  def resultT[F[_]: Monad, W: Monoid, R, A](f: R => ResultT[F, A]): ActionT[F, W, R, A] =
    ActionT(r => ResultT[({ type l[a] = WriterT[F, W, a] })#l, A](WriterT[F, W, Result[A]](f(r).run.map(a => (Monoid[W].zero, a)))))

  def option[F[_]: Monad, W: Monoid, R, A](f: R => A): ActionT[F, W, R, Option[A]] =
    ActionT(r => ResultT.option[({ type l[a] = WriterT[F, W, a] })#l, A](f(r)))

  def safe[F[_]: Monad, W: Monoid, R, A](a: => A): ActionT[F, W, R, A] =
    reader[F, W, R, A](_ => a)

  def ok[F[_]: Monad, W: Monoid, R, A](a: => A): ActionT[F, W, R, A] =
    ActionT(_ => ResultT.ok[({ type l[a] = WriterT[F, W, a] })#l, A](a))

  def exception[F[_]: Monad, W: Monoid, R, A](t: Throwable): ActionT[F, W, R, A] =
    ActionT(_ => ResultT.exception[({ type l[a] = WriterT[F, W, a] })#l, A](t))

  def fail[F[_]: Monad, W: Monoid, R, A](message: String): ActionT[F, W, R, A] =
    ActionT(_ => ResultT.fail[({ type l[a] = WriterT[F, W, a] })#l, A](message))

  def error[F[_]: Monad, W: Monoid, R, A](message: String, t: Throwable): ActionT[F, W, R, A] =
    ActionT(_ => ResultT.error[({ type l[a] = WriterT[F, W, a] })#l, A](message, t))

  def these[F[_]: Monad, W: Monoid, R, A](both: These[String, Throwable]): ActionT[F, W, R, A] =
    ActionT(_ => ResultT.these[({ type l[a] = WriterT[F, W, a] })#l, A](both))

  def fromDisjunction[F[_]: Monad, W: Monoid, R, A](either: These[String, Throwable] \/ A): ActionT[F, W, R, A] =
    ActionT[F, W, R, A](_ => ResultT.fromDisjunction[({ type l[a] = WriterT[F, W, a] })#l, A](either))

  def fromDisjunctionString[F[_]: Monad, W: Monoid, R, A](either: String \/ A): ActionT[F, W, R, A] =
    fromDisjunction[F, W, R, A](either.leftMap(This.apply))

  def fromDisjunctionThrowable[F[_]: Monad, W: Monoid, R, A](either: Throwable \/ A): ActionT[F, W, R, A] =
    fromDisjunction[F, W, R, A](either.leftMap(That.apply))

  def fromDisjunctionF[F[_]: Monad, W: Monoid, R, A](either: F[These[String, Throwable] \/ A]): ActionT[F, W, R, A] =
    ActionT[F, W, R, A](_ => ResultT.fromDisjunctionF[({ type l[a] = WriterT[F, W, a] })#l, A](WriterT(either.map(a => (Monoid[W].zero, a)))))

  def fromIO[F[_]: MonadIO, W: Monoid, R, A](v: IO[A]): ActionT[F, W, R, A] =
    ActionT[F, W, R, A](_ => ResultT[({ type l[a] = WriterT[F, W, a] })#l, A](WriterT(v.map(a => (Monoid[W].zero, Result.ok(a))).liftIO[F])))

  def fromIOResult[F[_]: MonadIO, W: Monoid, R, A](v: IO[Result[A]]): ActionT[F, W, R, A] =
    fromIO[F, W, R, Result[A]](v).flatMap(r => result(_ => r))

  def fromResultT[F[_]: Monad, W: Monoid, R, A](v: ResultT[F, A]): ActionT[F, W, R, A] =
    resultT(_ => v)

  def using[A: Resource, B <: A, W: Monoid, R, C](a: ActionT[IO, W, R, B])(run: B => ActionT[IO, W, R, C]): ActionT[IO, W, R, C] =
    ActionT(res => {
      type WriterTIOW[X] = WriterT[IO, W, X]
      val R = implicitly[Resource[A]]
      ResultT[WriterTIOW, C](WriterT(for {
        wrb <- a.run(res)
        (w, rb) = wrb
        r <- rb match {
          case Ok(b) => for {
            z <- run(b).run(res) onException R.close(b)
            _ <- R.close(b)
          } yield z
          case Error(e) =>
            (w, Error[C](e)).pure[IO]
        }
        (ww, rc) = r
      } yield (w |+| ww, rc)))
    })

  implicit def ActionTMonad[F[_]: Monad, W: Monoid, R]: Monad[({ type l[a] = ActionT[F, W, R, a] })#l] =
    new Monad[({ type l[a] = ActionT[F, W, R, a] })#l] {
      def bind[A, B](a: ActionT[F, W, R, A])(f: A => ActionT[F, W, R, B]) = a.flatMap(f)
      def point[A](a: => A) = ok[F, W, R, A](a)
    }
}

trait ActionTLowPriority {
  implicit def ActionTMonadIOPlus[F[_]: MonadIO, W: Monoid, R]:
    MonadIO[({ type l[a] = ActionT[F, W, R, a] })#l] with MonadPlus[({ type l[a] = ActionT[F, W, R, a] })#l]=
    new MonadIO[({ type l[a] = ActionT[F, W, R, a] })#l] with MonadPlus[({ type l[a] = ActionT[F, W, R, a] })#l] {
      def bind[A, B](a: ActionT[F, W, R, A])(f: A => ActionT[F, W, R, B]) = a.flatMap(f)
      def point[A](a: => A) = ActionT.ok[F, W, R, A](a)
      def liftIO[A](a: IO[A]) = ActionT.fromIO[F, W, R, A](a)
      def empty[A] = ActionT.fail[F, W, R, A]("fail")
      def plus[A](fa: ActionT[F, W, R, A], fb: =>ActionT[F, W, R, A]): ActionT[F, W, R, A] = bind(fa)(_ => fb)
    }
}


trait ActionTSupport[F[_], W, R] {
  def ask(implicit M: Monad[F], W: Monoid[W]): ActionT[F, W, R, R] =
    ActionT.ask

  def reader[A](f: R => A)(implicit M: Monad[F], W: Monoid[W]): ActionT[F, W, R, A] =
    ActionT.reader(f)

  def result[A](f: R => Result[A])(implicit M: Monad[F], W: Monoid[W]): ActionT[F, W, R, A] =
    ActionT.result(f)

  def resultT[A](f: R => ResultT[F, A])(implicit M: Monad[F], W: Monoid[W]): ActionT[F, W, R, A] =
    ActionT.resultT(f)

  def option[A](f: R => A)(implicit M: Monad[F], W: Monoid[W]): ActionT[F, W, R, Option[A]] =
    ActionT.option(f)

  def safe[A](a: => A)(implicit M: Monad[F], W: Monoid[W]): ActionT[F, W, R, A] =
    ActionT.safe(a)

  def ok[A](a: => A)(implicit M: Monad[F], W: Monoid[W]): ActionT[F, W, R, A] =
    ActionT.ok(a)

  def fromIO[A](v: IO[A])(implicit M: MonadIO[F], W: Monoid[W]): ActionT[F, W, R, A] =
    ActionT.fromIO(v)

  def fromIOResult[A](v: IO[Result[A]])(implicit M: MonadIO[F], W: Monoid[W]): ActionT[F, W, R, A] =
    ActionT.fromIOResult(v)

  def fromResultT[A](v: ResultT[F, A])(implicit M: Monad[F], W: Monoid[W]): ActionT[F, W, R, A] =
    ActionT.fromResultT(v)

  def exception[A](t: Throwable)(implicit M: Monad[F], W: Monoid[W]): ActionT[F, W, R, A] =
    ActionT.exception(t)

  def fail[A](message: String)(implicit M: Monad[F], W: Monoid[W]): ActionT[F, W, R, A] =
    ActionT.fail(message)

  def error[A](message: String, t: Throwable)(implicit M: Monad[F], W: Monoid[W]): ActionT[F, W, R, A] =
    ActionT.error(message, t)

  def these[A](both: These[String, Throwable])(implicit M: Monad[F], W: Monoid[W]): ActionT[F, W, R, A] =
    ActionT.these(both)
}
