package com.secretapp.backend.proto

trait Formats[A, B] extends Writes[A, B] with Reads[A, B]
