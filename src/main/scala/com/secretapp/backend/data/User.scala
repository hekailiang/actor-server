package com.secretapp.backend.data

import scodec.bits.BitVector

case class User(firstName: String, lastName: String, sex: Sex, photo: BitVector)
