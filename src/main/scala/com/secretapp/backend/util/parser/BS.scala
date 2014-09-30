package com.secretapp.backend.util.parser

import akka.util.ByteString
import scodec.bits.BitVector
import scala.annotation.tailrec
import scalaz._
import Scalaz._

object BS {
  def isWhiteSpace(b: Byte) = ((1L << b) & ((b - 64) >> 31) & 0x100002600L) != 0L

  def isDigit(b: Byte) = b >= 48 && b <= 57

  @tailrec
  private def convertBS2Long(bs: ByteString, index: Int = 0, acc: Long = 0L): Long = {
    if (index >= bs.length) acc
    else {
      val n = (bs(index) - 48) * math.pow(10, bs.length - index - 1).toLong
      convertBS2Long(bs, index + 1, acc + n)
    }
  }

  case class ParserState(data: ByteString) {
    var cursor: Int = 0
    var rightCursor: Int = data.length - 1
  }

  def parseSigned = State[ParserState, Long] { p =>
    val sign = if (p.data(p.cursor) == '-') {
      p.cursor += 1
      -1
    } else 1
    val (_, buf) = parse(isDigit)(p)
    (p, convertBS2Long(buf) * sign)
  }

  def parse(f: (Byte) => Boolean) = State[ParserState, ByteString] { p =>
    val startCursor = p.cursor
    while (p.cursor <= p.rightCursor && f(p.data(p.cursor))) p.cursor += 1
    (p, p.data.slice(startCursor, p.cursor))
  }

  def skipTill(f: (Byte) => Boolean) = State[ParserState, Unit] { p =>
    while (p.cursor <= p.rightCursor && f(p.data(p.cursor))) p.cursor += 1
    (p, ())
  }

  def skipRightTill(f: (Byte) => Boolean) = State[ParserState, Unit] { p =>
    while (p.rightCursor > 0 && f(p.data(p.rightCursor))) p.rightCursor -= 1
    (p, ())
  }

  def slice = State[ParserState, ByteString] { p =>
    (p, p.data.slice(p.cursor, p.rightCursor + 1))
  }

  def runParser[A](parser: State[ParserState, A], bs: ByteString) = parser.eval(ParserState(bs))
}
