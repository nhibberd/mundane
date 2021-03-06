package com.ambiata.mundane.data

import scalaz._, Scalaz._

object Maps {

  def outerJoin[K, V1, V2](m1: Map[K, V1], m2: Map[K, V2]): Vector[(K, \&/[V1, V2])] = {
    val b = Vector.newBuilder[(K, \&/[V1, V2])]
    m1.foreach {
      case (k, v) => b += k -> m2.get(k).cata(\&/.Both(v, _), \&/.This[V1](v))
    }
    m2.foreach {
      case (k, v) => if (!m1.contains(k)) b += k -> \&/.That[V2](v)
    }
    b.result()
  }

  def outerJoinMap[K, V1, V2](m1: Map[K, V1], m2: Map[K, V2]): Map[K, \&/[V1, V2]] =
    outerJoin(m1, m2).toMap
}
