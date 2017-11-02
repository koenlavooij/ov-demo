package nl.trivento.xml

import org.scalatest.FunSuite

import scala.util.Try
import scala.xml.{Null, UnprefixedAttribute}
import scala.xml.pull.EvElemStart

class XPathSpec extends FunSuite {
  test("An XPath element should not be empty") {
    assert(Try(XPathParser("")).isFailure)
  }

  test("An XPathParser can contain one element") {
    val value = XPathParser("a")
    assert(value.nonEmpty)
    assert(value.length == 1)
  }

  test("An XPathParser should be able to match an element called a") {
    val value = XPathParser("a")
    assert(value.nonEmpty)
    assert(value.length == 1)
    assert(value.head(EvElemStart("", "a", Null, null)))
  }

  test("An XPathParser should not to match an element with a different label") {
    val value = XPathParser("a")
    assert(value.nonEmpty)
    assert(value.length == 1)
    assert(!value.head(EvElemStart("", "b", Null, null)))
  }

  test("An XPathParser should be able to match on an attribute") {
    val value = XPathParser("[@x='1']")
    assert(value.nonEmpty)
    assert(value.length == 1)
    assert(value.head(EvElemStart("", "a", new UnprefixedAttribute("x", "1", Null), null)))
  }

  test("An XPathParser should be able to match on an attribute, when there are multiple on 1 key") {
    val value = XPathParser("[@x='2']")
    assert(value.nonEmpty)
    assert(value.length == 1)
    assert(value.head(EvElemStart("", "a", new UnprefixedAttribute("x", "1", new UnprefixedAttribute("x", "2", Null)), null)))
    assert(value.head(EvElemStart("", "a", new UnprefixedAttribute("x", "2", new UnprefixedAttribute("x", "1", Null)), null)))
    assert(value.head(EvElemStart("", "a", new UnprefixedAttribute("x", "2", new UnprefixedAttribute("x", "2", Null)), null)))
  }

  test("An XPathParser should be able to match on multiple attributes") {
    val value = XPathParser("[@x='1'][@y='2']")
    assert(value.nonEmpty)
    assert(value.length == 1)
    assert(value.head(EvElemStart("", "a", new UnprefixedAttribute("x", "1", new UnprefixedAttribute("y", "2", Null)), null)))
  }

  test("An XPathParser should not match on multiple attributes when at least one of them differs") {
    val value = XPathParser("[@x='1'][@y='2']")
    assert(value.nonEmpty)
    assert(value.length == 1)
    assert(!value.head(EvElemStart("", "a", new UnprefixedAttribute("x", "2", new UnprefixedAttribute("y", "2", Null)), null)))
    assert(!value.head(EvElemStart("", "a", new UnprefixedAttribute("x", "1", new UnprefixedAttribute("y", "1", Null)), null)))
    assert(!value.head(EvElemStart("", "a", new UnprefixedAttribute("x", "2", new UnprefixedAttribute("y", "1", Null)), null)))
  }

  test("An XPathParser should not match an element with different attributes") {
    val value = XPathParser("[@x='1']")
    assert(value.nonEmpty)
    assert(value.length == 1)
    assert(!value.head(EvElemStart("", "a", new UnprefixedAttribute("x", "2", Null), null)))
    assert(!value.head(EvElemStart("", "a", new UnprefixedAttribute("y", "1", Null), null)))
  }

  test("An XPathParser should be able to match on tag and attributes") {
    val value = XPathParser("a[@x='1']")
    assert(value.nonEmpty)
    assert(value.length == 1)
    assert(value.head(EvElemStart("", "a", new UnprefixedAttribute("x", "1", Null), null)))
  }

  test("An XPathParser should not match elements with different tag and attributes") {
    val value = XPathParser("a[@x='1']")
    assert(value.nonEmpty)
    assert(value.length == 1)
    assert(!value.head(EvElemStart("", "b", new UnprefixedAttribute("x", "1", Null), null)))
    assert(!value.head(EvElemStart("", "a", new UnprefixedAttribute("x", "2", Null), null)))
    assert(!value.head(EvElemStart("", "a", new UnprefixedAttribute("y", "1", Null), null)))
  }

  test("An XPathParser should be able to have an actual path") {
    val value = XPathParser("a[@x='1']/b[@y='1']")
    assert(value.nonEmpty)
    assert(value.length == 2)
  }

  test("A pattern match can be made") {
    val value = XPathParser("a[@x='1']/b[@y='1']")
    assert(value.nonEmpty)
    assert(value.length == 2)

    val path: Seq[EvElemStart] =
        EvElemStart("", "b", new UnprefixedAttribute("y", "1", Null), null) ::
        EvElemStart("", "a", new UnprefixedAttribute("x", "1", Null), null) ::
        Nil

    val pattern: XPathPatternMatcher = "a[@x='1']/b[@y='1']"
    val m = path match {
      case pattern(v) => v
      case _ => Nil
    }
    assert(m == path)
  }

  test("A pattern match can fail") {
    val value = XPathParser("a[@x='1']/b[@y='1']")
    assert(value.nonEmpty)
    assert(value.length == 2)

    val path: Seq[EvElemStart] =
      EvElemStart("", "a", new UnprefixedAttribute("x", "1", Null), null) ::
        EvElemStart("", "b", new UnprefixedAttribute("y", "1", Null), null) ::
        Nil

    val pattern: XPathPatternMatcher = "a[@x='1']/b[@y='2']"
    val m = path match {
      case pattern(v) => path.last
      case _ => Nil
    }
    assert(m == Nil)
  }
}