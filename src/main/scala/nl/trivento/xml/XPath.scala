package nl.trivento.xml

import scala.collection.AbstractIterator
import scala.util.matching.Regex
import scala.util.parsing.combinator.RegexParsers
import scala.xml.pull._

sealed trait Token
case class Identifier(value: String) extends Token
case class Literal(value: String) extends Token
case object Open extends Token
case object Close extends Token
case object Slash extends Token
case object Equal extends Token
case object NotEqual extends Token
case object At extends Token
case object Text extends Token

object Selector {
  val TRUE: Selector = (e) => true
  val FALSE: Selector = (e) => false
  val TEXT: Selector = (e) => e.isInstanceOf[EvText]

  def apply(f: XMLEvent => Boolean, desc: String = ""): Selector = new Selector {
    override def apply(event: XMLEvent): Boolean = f(event)
    override def toString: String = desc
  }
}

trait Selector {
  def apply(event: XMLEvent): Boolean

  def &&(other: Selector): Selector = Selector((e: XMLEvent) => this(e) && other(e))
  def ||(other: Selector): Selector = Selector((e: XMLEvent) => this(e) || other(e))
}
//
//object XPath {
//  def apply(path: String) = new XPath(path)
//}
//
//class XPath(path: String) {
//  val selectors: Seq[Selector] = XPathParser(path)
//
//  def unapply(in: Seq[EvElemStart]): Option[Seq[EvElemStart]] =
//    if (in.length != selectors.length)
//      None
//  else
//      selectors.zip(in).find((zipped: Tuple2[Selector, EvElemStart]) => zipped._1(zipped._2)).map(_ => in)
//}

class XPathException(msg: String) extends Exception(msg: String)

object XPathParser extends RegexParsers {
  sealed trait Value
  case class TextValue() extends Value
  case class AttributeValue(name: String) extends Value

  override def skipWhitespace = true
  override protected val whiteSpace: Regex = "[ \t]".r

  private def identifier:   Parser[Identifier] =    "[a-zA-Z_][-a-zA-Z0-9_]*".r ^^ { str => Identifier(str)}
  private def literal:      Parser[Literal] =       "'[^']*'".r ^^ { str => Literal(str.substring(1, str.length - 1)) }
  private def open:         Parser[Open.type] =     "[" ^^ { _ => Open }
  private def close:        Parser[Close.type] =    "]" ^^ { _ => Close }
  private def slash:        Parser[Slash.type] =    "/" ^^ { _ => Slash }
  private def equal:        Parser[Equal.type] =    "=" ^^ { _ => Equal }
  private def notEqual:     Parser[NotEqual.type] = "!=" ^^ { _ => NotEqual }
  private def at:           Parser[At.type] =       "@" ^^ { _ => At }
  private def content:      Parser[Text.type] =     "$" ^^ { _ => Text }
  private def operator:     Parser[Token] =         equal | notEqual

  private def paramValueFilter(key: String, valueMatcher: (String) => Boolean, desc: String): Selector =
    Selector((elem: XMLEvent) => elem.isInstanceOf[EvElemStart] && elem.asInstanceOf[EvElemStart].attrs
        .filter(a => {
          a.key == key && valueMatcher(a.value.head.toString)
        }).nonEmpty, desc)
  private def paramFilter: Parser[Selector] = (open ~ at ~ identifier ~ operator ~ literal ~ close) ^^ {
    case open ~ at ~ identifier ~ Equal ~ literal ~ close =>
      paramValueFilter(identifier.value, (v: String) => v == literal.value, s"$identifier == $literal")
    case open ~ at ~ identifier ~ NotEqual ~ literal ~ close =>
      paramValueFilter(identifier.value, (v: String) => v != literal.value, s"$identifier != $literal")
  }
  private def paramsFilter: Parser[Selector] = paramFilter.+ ^^ {
    params => params.reduce((left, right) => left && right)
  }
  private def tagFilter: Parser[Selector] = identifier ^^ {
    tag => Selector((elem: XMLEvent) => elem.isInstanceOf[EvElemStart] && elem.asInstanceOf[EvElemStart].label == tag.value, s"$tag")
  }
  private def tagAndParams: Parser[Selector] = tagFilter ~ paramsFilter ^^ {
    case tag ~ params => tag && params
  }
  private def elementFilter: Parser[Selector] = tagAndParams | tagFilter | paramsFilter | textValue
  private def attributeValue: Parser[AttributeValue] = at ~ identifier ^^ {
    case at ~ identifier => AttributeValue(identifier.value)
  }
  private def textValue: Parser[Selector] = content ^^ { _ => Selector.TEXT }
  private def possibleFilter: Parser[Option[Seq[Selector]]] = filter.?

  private def filter: Parser[Seq[Selector]] = elementFilter ~ (slash ~ filter).? ^^ {
    case element ~ tail => tail.map(element +: _._2).getOrElse(element :: Nil)
  }

  private def build[T](result: ParseResult[T]): T = {
    result match {
      case Success(r, _) => r
      case Error(msg, _) => throw new XPathException(msg)
      case Failure(msg, _) => throw new XPathException(msg)
    }
  }

  def apply(in: String): Seq[Selector] = build(parseAll(filter, in))
}

case class XPathPattern(selectors: Seq[Selector]) {
  def matches(path: Seq[XMLEvent]): Boolean = selectors.length == path.length && !selectors.zip(path).exists(t => !t._1(t._2))
}

object XPathPatternMatcher {
  implicit def x(pattern: String): XPathPatternMatcher = {
    println("Matcher for " + pattern)
    new XPathPatternMatcher(pattern)
  }
}

trait XPathMatcher {
  def unapply(path: Seq[XMLEvent]): Option[Seq[XMLEvent]]
}

class XPathPatternMatcher(pattern: XPathPattern) extends XPathMatcher {
  def this(str: String) = this(XPathPattern(XPathParser(str).reverse))
  def unapply(path: Seq[XMLEvent]): Option[Seq[XMLEvent]] = Some(path).filter(pattern.matches)
}

class PathEventIterator(in: Iterator[XMLEvent]) extends AbstractIterator[Seq[XMLEvent]] {
  var path: Seq[EvElemStart] = Nil

  override def hasNext: Boolean = in.hasNext
  override def next(): (Seq[XMLEvent]) = {
    val next = in.next()
    next match {
      case o: EvElemStart =>
        path = o +: path
        path
      case c: EvElemEnd =>
        path = path.tail
        c +: path
      case t: EvText =>
        t +: path
      case ref: EvEntityRef =>
        ref +: path
      case p: EvProcInstr =>
        p +: path
      case c: EvComment =>
        c +: path
    }
  }
}

abstract class EmittingParser[T](handler: T => Unit) extends PartialFunction[Seq[XMLEvent], EmittingParser[_]]
//
//object XMLParser {
//  type Parser = PartialFunction[Seq[XMLEvent], Parser]
//
//  def emit[A](elems: A*): Iterable[A] = Iterable[A](elems: _*)
//  def getAttr(meta: MetaData, default: String = "")(key: String): String = meta.asAttrMap.getOrElse(key, default)
//  def text(p: Seq[XMLEvent]): String = {
//    p.head.asInstanceOf[EvText].text
//  }
//  def flow[A](parser: EmittingParser[A]) = apply(parser)
//  def apply[A](parser: EmittingParser[A]) = {
//    //Flow[XMLEvent].statefulMapConcat[A](() => xml => Try(parser(xml)).getOrElse(emit()))
//    Flow[Seq[XMLEvent]].statefulMapConcat(() => xml => parser.applyOrElse(path))
//  }
//
//  def source(source: scala.io.Source): Source[Seq[XMLEvent], NotUsed] = {
//    Source.fromIterator(() => new PathEventIterator(new XMLEventReader(source)))
//  }
//}

