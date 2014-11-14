package utils

object Pagination {

  def fixCount(c: Int, min: Int = 1, max: Int = 100): Int =
    Math.min(max, Math.max(min, c))

}
