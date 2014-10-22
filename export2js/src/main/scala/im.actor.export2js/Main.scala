import com.secretapp.backend.data.message._

import language.experimental.macros
import scala.reflect.macros.Context

object SealedExample {
  def values[A]: Set[A] = macro values_impl[A]

  def values_impl[A: c.WeakTypeTag](c: Context) = {
    import c.universe._

    val symbol = weakTypeOf[A].typeSymbol

//    println(s"declarations: ${weakTypeOf[A].declarations}")
//
//    val internal = symbol.asInstanceOf[scala.reflect.internal.Symbols#Symbol]
//    val descendants = internal.children.map(_.asInstanceOf[Symbol])
//    println(s"descendants: $descendants")

    val tm: String = symbol.fullName
    println(s"tm: $tm")

    val decs = c.mirror.staticPackage("com.secretapp.backend.data.message").typeSignature.declarations

    def deepSearch(p: Type, space: String = ""): Set[Symbol] = {
      val declarations = p.declarations
      val child = declarations.filter(c => c.isClass && c.asClass.baseClasses.exists(_.fullName == tm) && c.asClass.isCaseClass && !c.asClass.isAbstractClass)
      val nested = declarations.filter(_.isPackage) flatMap { p =>
        println(s"${space}${p.fullName}")
        deepSearch(p.typeSignature, space.concat(" "))
      }
      child.toSet ++ nested
    }

    val pkg = c.mirror.staticPackage("com.secretapp.backend.data")
//    println(pkg.typeSignature.declarations.filter(_.isPackage))
    val child = deepSearch(pkg.typeSignature)
    println(s"child: $child")
    for (klass <- child) {
      val vals = klass.typeSignature.declarations.collect {
        case m: MethodSymbol if m.isCaseAccessor => m
      } map(_.name)
      println(s"$klass: $vals")
    }

//    val cclasses = decs.filter(c => c.isClass && c.asClass.baseClasses.exists(_.fullName == tm) && c.asClass.isCaseClass && !c.asClass.isAbstractClass)
//    for (klass <- cclasses) {
//      println(s"klass: ${klass}")
//    }

//    println(s"TransportMessage: ${c.mirror.staticClass("com.secretapp.backend.data.message.TransportMessage").typeSignature.declarations}")
//    println(s"declarations: ${c.mirror.staticPackage("com.secretapp.backend.data.message").typeSignature.declarations}")
//
//    //    if (!symbol.asClass.isTrait) c.abort(
////      c.enclosingPosition,
////      "Can only trait."
////    ) else {
//      val children = symbol.asClass.knownDirectSubclasses.toList
//      println(s"children: ${children}")
//
//      if (!children.forall(_.isModuleClass)) c.abort(
//        c.enclosingPosition,
//        "All children must be objects."
//      ) else c.Expr[Set[A]] {
//        def sourceModuleRef(sym: Symbol) = Ident(
//          sym.asInstanceOf[
//            scala.reflect.internal.Symbols#Symbol
//            ].sourceModule.asInstanceOf[Symbol]
//        )
//
//        Apply(
//          Select(
//            reify(Set).tree,
//            newTermName("apply")
//          ),
//          children.map(sourceModuleRef(_))
//        )
//      }

      c.abort(c.enclosingPosition, "test")
    }
//  }
}

object Main extends App {
  /*
SealedExample.values[com.secretapp.backend.data.message.TransportMessage]
SealedExample.values[com.secretapp.backend.data.message.ProtobufMessage]
SealedExample.values[com.secretapp.backend.data.message.rpc.RpcRequestMessage]
SealedExample.values[com.secretapp.backend.data.message.rpc.RpcResponseMessage]
SealedExample.values[com.secretapp.backend.data.message.update.UpdateMessage]
SealedExample.values[com.secretapp.backend.data.message.update.SeqUpdateMessage]
SealedExample.values[com.secretapp.backend.data.message.update.WeakUpdateMessage]
  */
  println("hello!")
}