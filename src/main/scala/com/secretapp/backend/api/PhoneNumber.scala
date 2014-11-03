package com.secretapp.backend.api

import scala.annotation.tailrec
import scala.util.{ Try, Success }
import com.google.i18n.phonenumbers.PhoneNumberUtil

object PhoneNumber {
  def normalize(number: Long): Option[Long] = {
    val phoneUtil = PhoneNumberUtil.getInstance()
    Try(phoneUtil.parse(s"+$number", "")) match {
      case Success(p) =>
        val phoneNumber = p.getCountryCode * Math.pow(10L, sizeOf(p.getNationalNumber) + 1).longValue + p.getNationalNumber
        Some(phoneNumber)
      case _ => None
    }
  }

  def normalizeWithCountry(number: Long): Option[(Long, String)] = {
    val phoneUtil = PhoneNumberUtil.getInstance()
    Try(phoneUtil.parse(s"+$number", "")) match {
      case Success(p) =>
        val phoneNumber = p.getCountryCode * Math.pow(10L, sizeOf(p.getNationalNumber) + 1).longValue + p.getNationalNumber
        Some(phoneNumber, phoneUtil.getRegionCodeForCountryCode(p.getCountryCode))
      case _ => None
    }
  }

  private def sizeOf(number: Long): Long = {
    @tailrec
    def f(n: Long, res: Long): Long = {
      if (n >= 10) f(n / 10, res + 1)
      else res
    }
    f(number, 0)
  }
}
