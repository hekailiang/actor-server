package com.secretapp.backend.proto

trait Reads[A, B] {

  def fromProto(b: B): A

}
