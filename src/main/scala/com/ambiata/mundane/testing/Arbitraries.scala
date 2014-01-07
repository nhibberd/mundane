package com.ambiata.mundane.testing

import org.scalacheck._
import Arbitrary._
import scalaz._, Scalaz._
import com.ambiata.mundane.control._

object Arbitraries {
  implicit def AttemptArbitrary[A: Arbitrary]: Arbitrary[Attempt[A]] =
    Arbitrary(arbitrary[(String \&/ Throwable) \/ A].map(Attempt.apply))

  implicit def ResultArbitrary[A: Arbitrary]: Arbitrary[Result[A]] =
    Arbitrary(arbitrary[(String \&/ Throwable) \/ A].map(Result.fromDisjunction))

  implicit def ResultTArbitrary[F[+_], A](implicit F: Functor[F], A: Arbitrary[F[(String \&/ Throwable) \/ A]]): Arbitrary[ResultT[F, A]] = {
    Functor[F]
    Arbitrary(arbitrary[F[(String \&/ Throwable) \/ A]].map(ResultT.fromDisjunction[F, A]))
  }

  /** WARNING: can't use scalaz-scalacheck-binding because of specs/scalacheck/scalaz compatibility at the moment */
  implicit def TheseArbitrary[A: Arbitrary, B: Arbitrary]: Arbitrary[A \&/ B] =
    Arbitrary(Gen.oneOf(
      arbitrary[(A, B)].map({ case (a, b) => \&/.Both(a, b) }),
      arbitrary[A].map(\&/.This(_): A \&/ B),
      arbitrary[B].map(\&/.That(_): A \&/ B)
    ))

  implicit def DisjunctionArbitrary[A: Arbitrary, B: Arbitrary]: Arbitrary[A \/ B] =
    Arbitrary(Gen.oneOf(
      arbitrary[A].map(-\/(_)),
      arbitrary[B].map(\/-(_))
    ))
}