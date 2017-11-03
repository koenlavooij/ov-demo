package nl.trivento.fastdata.travelclear.ndovloket

import nl.trivento.fastdata.travelclear.gtfs.{TripPosition, TripReader, TripRef}
import nl.trivento.xml._

import scala.xml.pull.{EvText, XMLEvent}

object NsPositiesParser {
  val trein: XPathPatternMatcher = "ArrayOfTreinLocation/TreinLocation"
  val treinNummer: XPathPatternMatcher = "TreinNummer/$"

  val treinMaterieelDelen: XPathPatternMatcher = "TreinMaterieelDelen"
  val materieelDeelNummer: XPathPatternMatcher = "MaterieelDeelNummer/$"
  val treinDeelLatitude: XPathPatternMatcher = "Latitude/$"
  val treinDeelLongitude: XPathPatternMatcher = "Longitude/$"

  def create(tripReader: TripReader): XMLParser = {
    new XMLParser({
      case trein(path) => EmbeddedResult[TripPosition](
        "trein", {
          case treinNummer((t: EvText) :: _) => Value("treinnummer", t.text)
          case treinMaterieelDelen(_) => EmbeddedResult[Position](
            "treindeel", {
              case materieelDeelNummer((t: EvText) :: _) => Value("identifier", t.text)
              case treinDeelLatitude((t: EvText) :: _) => Value("lat", t.text)
              case treinDeelLongitude((t: EvText) :: _) => Value("lng", t.text)
            },
            result => for {
              id <- result.getSingleString("identifier")
              lat <- result.getSingleString("lat") if lat.toDouble > 0
              lng <- result.getSingleString("lng") if lng.toDouble > 0
            } yield Position(id, lat.toDouble, lng.toDouble)
          )
        },
        result => for {
          nummer <- result.getSingleString("treinnummer")
          delen <- result.getAs[Position]("treindeel").map(_.head)
          tripInfo <- tripReader.getTripInfoByRtId(s"IFF:$nummer")
        } yield TripPosition(TripRef(tripInfo.tripId, active = true, tripInfo.name, tripInfo.color, tripInfo.textColor), delen.latitude, delen.longitude)
      )
    })
  }
}

object KV6PositiesParser {
  val onRoute: XPathPatternMatcher = "VV_TM_PUSH/KV6posinfo/ONROUTE"
  val onEnd: XPathPatternMatcher = "VV_TM_PUSH/KV6posinfo/END"
  val owner: XPathPatternMatcher = "dataownercode/$"
  val line: XPathPatternMatcher = "lineplanningnumber/$"
  val journey: XPathPatternMatcher = "journeynumber/$"
  val x: XPathPatternMatcher = "rd-x/$"
  val y: XPathPatternMatcher = "rd-y/$"

  def create(tripReader: TripReader): XMLParser = {
    new XMLParser({
      case onRoute(_) => EmbeddedResult[TripPosition](
        "ov", {
          case owner((t: EvText) :: _) => Value("owner", t.text)
          case line((t: EvText) :: _) => Value("line", t.text)
          case journey((t: EvText) :: _) => Value("journey", t.text)
          case x((t: EvText) :: _) => Value("x", t.text)
          case y((t: EvText) :: _) => Value("y", t.text)
        },
        result => for {
          owner <- result.getSingleString("owner")
          line <- result.getSingleString("line")
          journey <- result.getSingleString("journey")
          x <- result.getSingleString("x")
          y <- result.getSingleString("y")
          tripInfo <- tripReader.getTripInfoByRtId(s"$owner:$line:$journey")
          ll = LatLng.fromRijksdriehoek(x.toInt, y.toInt)
        } yield TripPosition(TripRef(tripInfo.tripId, active = true, tripInfo.name, tripInfo.color, tripInfo.textColor), ll.getLatitude, ll.getLongitude)
      )
      case onEnd(_) => EmbeddedResult[TripPosition](
        "ov", {
          case owner((t: EvText) :: _) => Value("owner", t.text)
          case line((t: EvText) :: _) => Value("line", t.text)
          case journey((t: EvText) :: _) => Value("journey", t.text)
        },
        result => for {
          owner <- result.getSingleString("owner")
          line <- result.getSingleString("line")
          journey <- result.getSingleString("journey")
          tripInfo <- tripReader.getTripInfoByRtId(s"$owner:$line:$journey")
        } yield TripPosition(TripRef(tripInfo.tripId, active = false, tripInfo.name, "#ffc917", "#003082"), 0, 0)
      )
    })
  }
}
