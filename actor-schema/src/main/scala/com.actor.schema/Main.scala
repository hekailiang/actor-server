package com.actor.schema

import AST._
import backend._
import scala.annotation.tailrec
import scala.collection.immutable
import scala.io.Source
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import java.io.FileOutputStream

object Main extends App {
  def genSchema(body: String, packageName: String, backend: Backend) = {
    val json = parse(body)
    val items = (json \ "sections")
      .children
      .map { i => (i \ "items").values}
      .foldLeft(List[Map[String, Any]]()) { (acc, maps) => acc ++ maps.asInstanceOf[List[Map[String, Any]]]}

    val aliases = (json \ "aliases").children.values.map { a =>
      val m = a.asInstanceOf[Map[String, String]]
      (m("alias"), m("type"))
    }.toMap

    def mapKlasses(klasses: Seq[Map[String, Any]], parentNameOpt: Option[String] = None) = klasses map { klass =>
      val content = klass("content").asInstanceOf[Map[String, Any]]
      val parentName =
        if (parentNameOpt.nonEmpty) parentNameOpt
        else content.get("trait").map(_.asInstanceOf[Map[String, Any]]("name").asInstanceOf[String])
      val klassNamePrefix = klass("type") match {
        case "rpc" => "Request"
        case "response" => "Response"
        case "update" => "Update"
        case _ => ""
      }
      val fields = content("attributes").asInstanceOf[Seq[Map[String, Any]]].map { attribute =>
        val name = attribute("name").asInstanceOf[String]
        val id = attribute("id").asInstanceOf[BigInt].toInt

        @tailrec
        def getNameAndKind(m: Any, kindOpt: Option[Rule.Rule] = None, isTrait: Boolean = false): (String, Rule.Rule, Boolean) = m match {
          case n: String => (n, kindOpt.getOrElse(Rule.Required), isTrait)
          case m: Map[String, Any] =>
            val kind: Rule.Rule = kindOpt.getOrElse {
              m("type").asInstanceOf[String] match {
                case "opt" => Rule.Optional
                case "list" => Rule.Repeated
                case _ => Rule.Required
              }
            }
            val t = m("type").asInstanceOf[String]
            m.get("childType") match {
              case None => (t, kind, false)
              case Some(map) =>
                val isTrait = map match {
                  case _: String => t == "trait"
                  case _ => false
                }
                getNameAndKind(map, Some(kind), isTrait)
            }
        }

        val (kind, rule, _isTrait) = getNameAndKind(attribute("type"))
        // TODO: dirty hack
        val isTrait = _isTrait || (name == "update" && kind == "bytes")

        Field(name, id, aliases.getOrElse(kind, kind), rule, isTrait)
      }
      val header = content.get("header").map(_.asInstanceOf[BigInt].toInt)
      val name = s"$klassNamePrefix${content("name").asInstanceOf[String]}"
      Klass(name, parentName, fields, header)
    }

    val rpcRequestItems = items.filter(_("type") == "rpc")
    val rpcRequests = mapKlasses(rpcRequestItems, Some("RpcRequestMessage"))

    val anonymousResponses = rpcRequestItems.filter {
      _("content").asInstanceOf[Map[String, Any]].get("response").map {
        _.asInstanceOf[Map[String, Any]]("type").asInstanceOf[String]
      } match {
        case Some("anonymous") => true
        case _ => false
      }
    }.map { r =>
      val content = r("content").asInstanceOf[Map[String, Any]]
      val response = content("response").asInstanceOf[Map[String, Any]]
      Map[String, Any](
        "type" -> "response",
        "content" -> Map[String, Any](
          "name" -> content("name").asInstanceOf[String],
          "header" -> response("header"),
          "attributes" -> response("attributes")
        )
      )
    }
    val rpcResponseItems = items.filter(_("type") == "response") ++ anonymousResponses
    val rpcResponses = mapKlasses(rpcResponseItems, Some("RpcResponseMessage"))

    val updateBoxItems = mapKlasses(items.filter(_("type") == "update_box"), Some("UpdateBoxMessage"))
    val updates = mapKlasses(items.filter(_("type") == "update"), Some("UpdateMessage"))
    val structs = mapKlasses(items.filter(_("type") == "struct"))
    val klasses = rpcRequests ++ rpcResponses ++ updateBoxItems ++ updates ++ structs

    val abstractKlasses = klasses.filter(_.parentKlass.isDefined).groupBy(_.parentKlass.get).map {
      case (name, child) =>
        val headerSize = name match {
          case "UpdateBoxMessage" | "RpcResponseMessage" | "RpcRequestMessage" => HeaderSize.Int
          case _ => HeaderSize.Byte
        }
        AbstractKlass(name, child, headerSize)
    }.toSeq

    val enums = items.filter(_("type") == "enum").map { e =>
      val content = e("content").asInstanceOf[Map[String, Any]]
      val values = content("values").asInstanceOf[List[Map[String, Any]]].map { m =>
        (m("name").asInstanceOf[String], m("id").asInstanceOf[BigInt].toInt)
      }.toMap
      Enum(content("name").asInstanceOf[String], values)
    }

    val sameKlassNames = klasses.map(_.name).foldLeft(immutable.Map[String, Int]()) {
      case (map, name) => map.get(name) match {
        case None => map + Tuple2(name, 1)
        case Some(n) => map + Tuple2(name, n + 1)
      }
    }.filter(_._2 > 1).keys
    assert(sameKlassNames.size == 0, s"Same names: ${sameKlassNames.mkString(", ")}")

    backend.gen(packageName, abstractKlasses, klasses, enums)
  }

  val outputDir = "/Users/timothyklim/Projects/secretapp/actor-js/lib/schema"
  val backend = CoffeeScript
  Seq(
    ("schema/actor.json", "ActorMessages"),
    ("schema/actor_encrypted.json", "ActorEncryptedMessages")
  ) foreach { case (fileName, packageName) =>
    val body = Source.fromFile(fileName).mkString
    val res = genSchema(body, packageName, backend)
    val file = new FileOutputStream(s"$outputDir/$packageName.${backend.ext}", false)
    file.write(res.mkString.getBytes)
    file.close()
  }
}
