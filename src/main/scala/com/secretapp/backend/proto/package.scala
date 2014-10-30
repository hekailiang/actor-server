package com.secretapp.backend

package object proto {

  def toProto[A, B](a: A)(implicit w: Writes[A, B]): B = w.toProto(a)

  def fromProto[A, B](b: B)(implicit r: Reads[A, B]): A = r.fromProto(b)

}
