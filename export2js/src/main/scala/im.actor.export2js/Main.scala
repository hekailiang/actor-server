package im.actor.export2js

import com.secretapp.backend.data.message._

import language.experimental.macros
import scala.reflect.macros.Context

case class JsonPackage(name: String, klasses: Seq[JsonClass])
case class JsonClass(name: String, header: Option[Int])

object SealedExample {
  def values[A]: Seq[Int] = macro values_impl[A]

  def values_impl[A: c.WeakTypeTag](c: Context) = {
    import c.universe._

    val symbol = weakTypeOf[A].typeSymbol
    val tm: String = symbol.fullName
    val protoMessageT = weakTypeOf[ProtoMessageWithHeader].typeSymbol.fullName

    def deepSearch(p: Type, space: String = ""): Set[Symbol] = {
      val declarations = p.declarations
      val child = declarations.filter(c => c.isClass && c.asClass.baseClasses.exists(_.fullName == tm) && c.asClass.isCaseClass && !c.asClass.isAbstractClass)
      val nested = declarations.filter(_.isPackage) flatMap { p =>
        println(s"$space${p.fullName}")
        deepSearch(p.typeSignature, space.concat(" "))
      }
      child.toSet ++ nested
    }

    val pkg = c.mirror.staticPackage("com.secretapp.backend.data")
    val child = deepSearch(pkg.typeSignature)
    println(s"child: $child")

    var s = Seq[Int]()

    for (klass <- child) {
      val vals = klass.typeSignature.declarations.collect {
        case m: MethodSymbol if m.isCaseAccessor => m
      } map { v =>
        val kind = klass.typeSignature.declaration(v.name).typeSignature.toString.replaceFirst("=> ", "").replaceAll(".*\\.", "")
        s"${v.name} -> $kind"
      }
      if (klass.asClass.baseClasses.exists(_.fullName == protoMessageT)) {
        val header = c.eval(c.Expr[Int](c.parse(s"${klass.fullName}.header")))
        println(s"header: $header")
        s ++= Seq(header)
      }
      println(s"$klass: $vals}")
    }

    val k = s.map { i => c.universe.reify { c.literal(i).splice }}
    c.Expr[Seq[Int]](Apply(Select(reify(Seq).tree, newTermName("apply")), k.map(_.tree).toList))
  }
}

object Main {
  def main(args: Array[String]) {
    println(s"hello!: ${args.toList}")
  }
}

//  /*
//SealedExample.values[com.secretapp.backend.data.message.TransportMessage]
//SealedExample.values[com.secretapp.backend.data.message.ProtobufMessage]
//SealedExample.values[com.secretapp.backend.data.message.rpc.RpcRequestMessage]
//SealedExample.values[com.secretapp.backend.data.message.rpc.RpcResponseMessage]
//SealedExample.values[com.secretapp.backend.data.message.update.UpdateMessage]
//SealedExample.values[com.secretapp.backend.data.message.update.SeqUpdateMessage]
//SealedExample.values[com.secretapp.backend.data.message.update.WeakUpdateMessage]
//  */
