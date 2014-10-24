package im.actor.export2js.macros

import JsonType._
import com.secretapp.backend.data.message.ProtoMessageWithHeader
import language.experimental.macros
import scala.reflect.macros.Context

object SealedMacros {
  def getSealedClass[A]: JsonSealedClass = macro getSealedClass_impl[A]

  def getSealedClass_impl[A: c.WeakTypeTag](c: Context) = {
    import c.universe._

    val symbol = weakTypeOf[A].typeSymbol
    val tm: String = symbol.fullName
    val protoMessageT = weakTypeOf[ProtoMessageWithHeader].typeSymbol.fullName
    val rootPackage = c.mirror.staticPackage("com.secretapp.backend.data")

    def deepSearch(p: Type, space: String = ""): Set[Symbol] = {
      val declarations = p.declarations
      val child = declarations.filter(c => c.isClass && c.asClass.baseClasses.exists(_.fullName == tm) && c.asClass.isCaseClass && !c.asClass.isAbstractClass)
      val nested = declarations.filter(_.isPackage) flatMap { p => deepSearch(p.typeSignature, space.concat(" ")) }
      child.toSet ++ nested
    }
    val childSymbols = deepSearch(rootPackage.typeSignature)

    def normilizeKind(name: String) = name.replaceFirst("=> ", "").replaceAll(".*\\.", "").replaceAll("(Vector|Set|List|Array)\\[", "Seq[").trim

    val child = childSymbols map { klass =>
      val fields = klass.typeSignature.declarations.collect {
        case m: MethodSymbol if m.isCaseAccessor => m
      } map { v =>
        val kind = normilizeKind(klass.typeSignature.declaration(v.name).typeSignature.toString)
        reify { JsonField(c.literal(v.name.decoded).splice, c.literal(kind).splice) }
      }
      val header =
        if (klass.asClass.baseClasses.exists(_.fullName == protoMessageT)) {
          val code = c.eval(c.Expr[Int](c.parse(s"${klass.fullName}.header")))
          reify(Some(c.literal(code).splice))
        } else reify(None)

      val fieldsExpr = c.Expr[Seq[c.Expr[JsonField]]](Apply( Select(reify(Seq).tree, newTermName("apply")), fields.map(_.tree).toList ))
      c.Expr[JsonClass](
        Apply(Select(reify(JsonClass).tree, newTermName("apply")),
          List(
            reify { c.literal(klass.name.decoded).splice }.tree,
            header.tree,
            fieldsExpr.tree
          ))
      )
    }
    val klassExpr = c.Expr[Seq[c.Expr[JsonClass]]](Apply( Select(reify(Seq).tree, newTermName("apply")), child.map(_.tree).toList ))
    c.Expr[JsonSealedClass](
      Apply(
        Select(reify(JsonSealedClass).tree, newTermName("apply")),
        List(
          reify { c.literal(symbol.name.decoded).splice }.tree,
          klassExpr.tree
        )))
  }
}
