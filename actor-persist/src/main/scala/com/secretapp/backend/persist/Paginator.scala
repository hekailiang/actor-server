package com.secretapp.backend.persist

import scalikejdbc._

trait Paginator[A] { this: SQLSyntaxSupport[A] =>
  val publicColumns: Seq[String]
  val digitColumns: Set[String] = Set()
  val alias: QuerySQLSyntaxProvider[SQLSyntaxSupport[A], A]

  val MAX_LIMIT = 50

  def apply(a: SyntaxProvider[A])(rs: WrappedResultSet): A

  def paginate(req: Map[String, Seq[String]])(implicit session: DBSession): (Seq[A], Int) = {
    val selectQ = select.from(this as alias)

    val filterMap = req.collect {
      case (key, value) if key.startsWith("filter[") =>
        val column = key.replaceAll("""\Afilter\[|\]\z""", "")
        if (publicColumns.contains(column)) {
          val query = if (digitColumns.contains(column)) value.mkString.toLong else value.mkString
          Some((column, query))
        } else None
    }.collect { case Some(v) => v }

    val orderBy = req.get("order_by").flatMap { s =>
      val column = s.mkString
      if (publicColumns.contains(column)) Some(column)
      else None
    }

    val whereQ = filterMap match {
      case x :: xs =>
        xs.foldLeft(selectQ.where.eq(alias.column(x._1), x._2)) {
          (acc, s) => acc.and.eq(alias.column(s._1), s._2)
        }
      case Nil => selectQ.where(sqls"") // HACK
    }

    val orderQ = orderBy match {
      case Some(orderSql) => whereQ.orderBy(sqls"$orderSql")
      case None => whereQ.orderBy(sqls"") // HACK
    }

    val offset = math.max(0, req.get("offset").map(_.mkString.toInt).getOrElse(0))
    val limit = math.min(MAX_LIMIT, req.get("limit").map(_.mkString.toInt).getOrElse(MAX_LIMIT))

    val q = orderQ.limit(limit).offset(offset)

    val totalCount = sql"select count(*) as _total_count from (${q.toSQLSyntax}) as _ct"
      .map(_.int("_total_count")).single().apply().head

    (q.toSQL.map(this(alias)).list().apply, totalCount)
  }
}
