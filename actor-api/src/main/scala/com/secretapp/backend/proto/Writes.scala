package com.secretapp.backend.proto

trait Writes[A, B] {

  def toProto(a: A): B

}
