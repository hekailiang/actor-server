package errors

case class BadRequestException(msg: String) extends Exception
