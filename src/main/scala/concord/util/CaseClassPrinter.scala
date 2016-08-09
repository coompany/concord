package concord.util

import scala.reflect.runtime.universe._


trait CaseClassPrinter {

    private val m = runtimeMirror(getClass.getClassLoader)
    private val im = m.reflect(this)

    def toString(indent: String): String

    override def toString: String = toString("")

    protected def prettyStr[T: TypeTag](indent: String = ""): String =
        typeOf[T].members.collect {
            case m: MethodSymbol if m.isCaseAccessor => m
        }.map { decl =>
            val value = im.reflectField(decl.asTerm).get
            val valueStr = value match {
                case printer: CaseClassPrinter => s"\n${printer.toString(s"$indent    ")}"
                case _ => s" $value"
            }
            s"$indent${decl.name}:$valueStr"
        }.mkString("\n")

}
