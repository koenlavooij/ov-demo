package nl.trivento.xml

import scala.collection.AbstractIterator
import scala.io.Source
import scala.xml.pull.{EvElemEnd, EvElemStart, XMLEvent, XMLEventReader}

object Result {
  val empty = Result(Map.empty)
}

case class Result(map: Map[String, Seq[_]]) {
  def getSingleString(key: String): Option[String] = map.get(key).map(_.map(_.toString()).reduce(_ + _))
  def getAs[T](key: String): Option[Seq[T]] = map.get(key).map(_.map(_.asInstanceOf[T]))

  def merge(other: Result): Result = Result((map.toSeq ++ other.map.toSeq).groupBy(_._1).mapValues(_.flatMap(_._2)))
  def include(key: String, value: Any): Result = merge(Result(Map(key -> Seq(value))))
}

sealed trait ParseResult
case class EmbeddedResult[T](key: String, parser: PartialFunction[Seq[XMLEvent], ParseResult], f: Result => Option[T]) extends ParseResult
case class EntityResult(values: Result) extends ParseResult
case class Done() extends ParseResult
case class NotDefined() extends ParseResult
case class Value(key: String, value: String) extends ParseResult


class XMLParser(val pf: PartialFunction[Seq[XMLEvent], ParseResult]) {
  private def parse(it: AbstractIterator[XMLEvent],
                    path: Seq[XMLEvent],
                    pf: PartialFunction[Seq[XMLEvent], ParseResult]): Result = {
    var returnValue = Result.empty

    while (it.hasNext) {
      val result: ParseResult = pf
        .orElse(PartialFunction[Seq[XMLEvent], ParseResult] {
          case (_: EvElemEnd) :: _ => Done()
          case (s: EvElemStart) :: _ => EntityResult(parse(it, s +: path, pf))
          case _ => NotDefined()
        })
        .apply(it.next() +: path)

      result match {
        case Done() => return returnValue
        case EntityResult(m) => returnValue = returnValue.merge(m)
        case EmbeddedResult(key, parser, f) =>
          f(parse(it, Nil, parser)).foreach(value => returnValue = returnValue.include(key, value))
        case Value(key, value) =>
          returnValue = returnValue.include(key, value)
        case NotDefined() =>
      }
    }

    returnValue
  }

  def parse(it: AbstractIterator[XMLEvent]): Result = parse(it, Nil, pf)

  def parse(source: Source): Result = parse(new XMLEventReader(source))
}
