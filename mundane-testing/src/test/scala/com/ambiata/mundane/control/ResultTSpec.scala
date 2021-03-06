package com.ambiata.mundane
package control

import testing.Arbitraries._
import testing.Laws._
import org.specs2._
import scalaz._, Scalaz._, \&/._

class ResultTSpec extends Specification with ScalaCheck { def is = s2"""

 ResultT Laws
 ============

   equals laws                    ${equal.laws[ResultT[Option, Int]]}
   monad laws                     ${monad.laws[({ type l[a] = ResultT[Option, a] })#l]}


 ResultT Combinators
 ===================

   ||| ok case                       $okOr
   ||| error case                    $errorOr
   getOrElse ok case                 $okGetOrElse
   getOrElse error case              $errorGetOrElse
   disjunction conversions           $disjunction
   disjunction string conversions    $disjunctionString
   disjunction throwable conversions $disjunctionThrowable
   disjunctionF conversions          $disjunctionF
   fromOption                        $fromOption
   fromOption error case             $fromOptionF
   when                              $when
   unless                            $unless

 ResultT Construction
 ====================

   exception safety (ok)          $safe
   exception safety (exception)   $exception
   option safety (none)           $nullage
   option safety (some)           $some

"""
  type Fail = String \&/ Throwable

  def monads =
    monad.laws[({ type l[a] = ResultT[Option, a] })#l]

  def okOr = prop((a: Int, b: ResultT[Option, Int]) =>
    (ResultT.ok[Option, Int](a) ||| b) == ResultT.ok[Option, Int](a))

  def errorOr = prop((a: Fail, b: ResultT[Option, Int]) =>
    (ResultT.these[Option, Int](a) ||| b) == b)

  def okGetOrElse = prop((a: Int, b: Int) =>
    ResultT.ok[Option, Int](a).getOrElse(b) == Some(a))

  def errorGetOrElse = prop((a: Fail, b: Int) =>
    ResultT.these[Option, Int](a).getOrElse(b) == Some(b))

  def disjunctionF = prop((a: Option[Fail \/ Int]) =>
    ResultT.fromDisjunctionF[Option, Int](a).toDisjunction == a)

  def disjunction = prop((a: Fail \/ Int) =>
    ResultT.fromDisjunction[Option, Int](a).toDisjunction == a.pure[Option])

  def disjunctionString = prop((a: String \/ Int) =>
    ResultT.fromDisjunctionString[Id, Int](a).toDisjunction ==== a.leftMap(This.apply))

  def disjunctionThrowable = prop((a: Throwable \/ Int) =>
    ResultT.fromDisjunctionThrowable[Id, Int](a).toDisjunction ==== a.leftMap(That.apply))

  def fromOption =
    ResultT.fromOption[Id, Int](Some(1), "foo") ==== ResultT.ok(1)

  def fromOptionF =
    ResultT.fromOption[Id, Int](None, "foo") ==== ResultT.fail("foo")

  def when =
    (ResultT.when[Id](true, ResultT.fail("foo")), ResultT.when[Id](false, ResultT.fail("foo"))) ====
      ((ResultT.fail("foo"), ResultT.unit))

  def unless =
    (ResultT.unless[Id](true, ResultT.fail("foo")), ResultT.unless[Id](false, ResultT.fail("foo"))) ====
      ((ResultT.unit, ResultT.fail("foo")))

  def safe = prop((a: Int) =>
    ResultT.safe[Option, Int](a) == ResultT.ok[Option, Int](a))

  def exception = prop((t: Throwable) =>
    ResultT.safe[Option, Int](throw t) == ResultT.exception[Option, Int](t))

  def some = prop((a: Int) =>
    ResultT.option[Id, Int](a) == ResultT.ok[Id, Option[Int]](Some(a)))

  def nullage = prop((_: Unit) =>
    ResultT.option[Id, String](bad) == ResultT.ok[Id, Option[String]](None))

  def bad: String = null
}
