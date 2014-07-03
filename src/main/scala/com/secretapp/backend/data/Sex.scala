package com.secretapp.backend.data

sealed trait Sex
case object NoSex extends Sex
case object Man extends Sex
case object Woman extends Sex
