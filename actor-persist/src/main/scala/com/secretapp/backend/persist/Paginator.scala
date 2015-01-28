package com.secretapp.backend.persist

import scalikejdbc._

trait Paginator[A] { this: SQLSyntaxSupport[A] =>
  val publicColumns: Set[String] = Set()
  val digitColumns: Set[String] = Set()
  val alias: QuerySQLSyntaxProvider[SQLSyntaxSupport[A], A]

  val MAX_LIMIT = 128

  def paginateWithTotal[T](sqlQ: SQLSyntax, req: Map[String, Seq[String]], defaultOrderBy: Option[String])
                          (f: WrappedResultSet => T)
                          (implicit session: DBSession): (Seq[T], Int) = {
    val filterMap = req.collect {
      case (key, value) if key.startsWith("filter[") =>
        val column = key.replaceAll("""\Afilter\[|\]\z""", "")
        if (publicColumns.contains(column)) {
          val query = if (digitColumns.contains(column)) value.mkString.toLong else value.mkString
          Some((column, query))
        } else None
    }.collect { case Some(v) => v }

    val orderBy = req.get("orderBy").flatMap { s =>
      val v = s.mkString.split("\\.").toList
      val column = v.head
      val asc = if (v.last.toLowerCase == "desc") "desc" else "asc"
      if (publicColumns.contains(column)) Some(column, asc)
      else None
    }

    val whereQ = filterMap match {
      case x :: xs =>
        xs.foldLeft(sqlQ.where.eq(alias.column(x._1), x._2)) {
          (acc, s) => acc.and.eq(alias.column(s._1), s._2)
        }
      case Nil => sqlQ
    }

    val orderQ = orderBy match {
      case Some((orderSql, asc)) =>
        val o = whereQ.orderBy(alias.column(orderSql))
        if (asc == "asc") o.asc
        else o.desc
      case None => whereQ.orderBy(alias.column(defaultOrderBy.getOrElse("")))
    }

    val offset = math.max(0, req.get("offset").map(_.mkString.toInt).getOrElse(0))
    val limit = math.min(MAX_LIMIT, req.get("limit").map(_.mkString.toInt).getOrElse(MAX_LIMIT))

    val q = orderQ.limit(limit).offset(offset)

    val entries = sql"$q".map(f).list().apply
    val totalCount = sql"select count(*) as _total_count from ($q) as _ct"
      .map(_.int("_total_count")).single().apply().head

    (entries, totalCount)
  }
}
