package nl.trivento.fastdata.travelclear.gtfs

import java.io.{File, FileOutputStream}
import java.net.URL
import java.util
import java.util.zip.ZipFile

import akka.actor._
import org.onebusaway.csv_entities.ZipFileCsvInputSource
import org.onebusaway.gtfs.impl.GtfsDaoImpl
import org.onebusaway.gtfs.model.AgencyAndId
import org.onebusaway.gtfs.serialization.GtfsReader
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.language.postfixOps

class TripInfoCollector extends GtfsDaoImpl {
  private val logger = LoggerFactory.getLogger(classOf[TripInfoCollector])
  private val trips = mutable.Map.empty[String, TripInfo]
  private val tripsRt = mutable.Map.empty[String, TripInfo]
  private val stops = mutable.Map.empty[AgencyAndId, Stop]
  private val stopTimes = mutable.Map.empty[AgencyAndId, mutable.ListBuffer[StopTime]]
  private val shapePoints = mutable.Map.empty[AgencyAndId, mutable.ListBuffer[ShapePoint]]

  override def saveEntity(entity: scala.Any): Unit = {
    entity match {
      case s: org.onebusaway.gtfs.model.Stop =>
        stops.put(s.getId, Stop(s.getLat, s.getLon, s.getName))
        super.saveEntity(entity)
      case s: org.onebusaway.gtfs.model.StopTime =>
        stops.get(s.getStop.getId).foreach(
          stop => stopTimes.getOrElseUpdate(s.getTrip.getId, new mutable.ListBuffer[StopTime])
            .append(StopTime(s.getStopSequence, stop, s.getArrivalTime, s.getDepartureTime))
        )
      case s: org.onebusaway.gtfs.model.ShapePoint =>
        shapePoints.getOrElseUpdate(s.getShapeId, new mutable.ListBuffer[ShapePoint]())
          .append(ShapePoint(s.getSequence, s.getLat, s.getLon))
      case _ =>
        super.saveEntity(entity)
    }
  }

  def textColor(routeType: Int): String = routeType match {
    case 0 => "white" //tram
    case 1 => "white" //metro
    case 2 => "#003082" //rail (NS)
    case 3 => "white" // bus
    case 4 => "black" //ferry
    case 5 => "black" //cable car
    case 6 => "white" //gondola
    case 7 => "black" //Funicular... yep, that is a thing
  }

  def color(routeType: Int): String = routeType match {
    case 0 => "red" //tram
    case 1 => "brown" //metro
    case 2 => "#ffc917" //rail (NS)
    case 3 => "blue" // bus
    case 4 => "#66ff99" //ferry
    case 5 => "#cccccc" //cable car
    case 6 => "#660000" //gondola
    case 7 => "#FFFFFF" //Funicular... yep, that is a thing
  }

  def agencyColor: PartialFunction[String, String] = {
    case "ARR" => "#00B6C6"
    case "BRAVO:ARR" => "#970000"
    case "BRAVO:CXX" => "#970000"
    case "BRENG" => "#0094DC"
    case "CXX" => "#0F9AAC"
    case "DOEKSEN" => "#004085"
    case "EBS" => "#E8E8E8"
    case "FF" => "#7F6E53"
    case "GVB" => "#205EAA"
    case "HTM" => "#F40000"
    case "HTMBUZZ" => "#F40000"
    case "IFF:ABRN" => "#C01513"
    case "IFF:BN" => "#0091D1"
    case "IFF:BRENG" => "#0094DC"
    case "IFF:NOORD" => "#00B6C6"
    case "IFF:NS" => "#ffc917"
    case "IFF:NSI" => "#ffc917"
    case "IFF:RNET" => "#0F9AAC"
    case "IFF:SYN" => "#00A0BB"
    case "IFF:VALLEI" => "#88CEE0"
    case "LEVELINK" => "#00386C"
    case "NIAG" => "#59A46E"
    case "OVREGIOY" => "#0087CE"
    case "QBUZZ" => "white"
    case "RET" => "#EA1728"
    case "SYNTUS" => "#EA1728"
    case "TESO" => "#FFDC5E"
    case "TWENTS" => "#E20007"
    case "UOV" => "#FCD307"
    case "WATERBUS" => "#1D3C87"
    case "WPD" => "#004F89"
  }

  def agencyTextColor: PartialFunction[String, String] = {
    case "ARR" => "White"
    case "BRAVO:ARR" => "#B2B2B2"
    case "BRAVO:CXX" => "#B2B2B2"
    case "BRENG" => "#DE006F"
    case "CXX" => "#5C5F1B"
    case "DOEKSEN" => "white"
    case "EBS" => "#61B53D"
    case "FF" => "#3D4E6B"
    case "GVB" => "#EF8500"
    case "HTM" => "white"
    case "HTMBUZZ" => "white"
    case "IFF:ABRN" => "#4B5560"
    case "IFF:BN" => "#1F5A8B"
    case "IFF:BRENG" => "#DE006F"
    case "IFF:NOORD" => "White"
    case "IFF:NS" => "#003082"
    case "IFF:NSI" => "#003082"
    case "IFF:RNET" => "#5C5F1B"
    case "IFF:SYN" => "#6D6359"
    case "IFF:VALLEI" => "white"
    case "LEVELINK" => "white"
    case "NIAG" => "#005294"
    case "OVREGIOY" => "#AFA291"
    case "QBUZZ" => "#4E4F4F"
    case "RET" => "white"
    case "SYNTUS" => "#6D6359"
    case "TESO" => "#00944E"
    case "TWENTS" => "#0F2C87"
    case "UOV" => "#19171A"
    case "WATERBUS" => "white"
    case "WPD" => "#002C3E"
  }

  override def close(): Unit = {
    super.close()

    logger.info("Trips: " + getAllTrips.size())
    getAllTrips.asScala.foreach(trip => {
      val info = TripInfo(
        trip.getId.getId,
        trip.getRoute.getShortName + " " + trip.getRoute.getLongName,
        agencyColor.applyOrElse(trip.getRoute.getAgency.getId, (_: String) => color(trip.getRoute.getType)),
        agencyTextColor.applyOrElse(trip.getRoute.getAgency.getId, (_: String) => textColor(trip.getRoute.getType)),
        stopTimes.get(trip.getId).map(_.sortBy(_.seq).toList),
        shapePoints.get(trip.getShapeId).map(_.sortBy(_.seq).toList),
      )
      trips.put(trip.getId.getId, info)
      tripsRt.put(trip.getRealtimeTripId(), info)
    })

    stopTimes.clear()
    shapePoints.clear()
    logger.info("Normalized trips: " + trips.size)

    super.clear()
  }

  def getTripInfo(tripId: String): Option[TripInfo] = trips.get(tripId)
  def getTripInfoByRtId(tripId: String): Option[TripInfo] = tripsRt.get(tripId)
}

class TripReader {
  private val logger = LoggerFactory.getLogger(classOf[TripReader])
  private val dao = new TripInfoCollector()

  private var reader = new GtfsReader()
  reader.setEntityClasses(new util.ArrayList(Seq(
    classOf[org.onebusaway.gtfs.model.Agency],
    classOf[org.onebusaway.gtfs.model.ShapePoint],
    classOf[org.onebusaway.gtfs.model.Route],
    classOf[org.onebusaway.gtfs.model.Stop],
    classOf[org.onebusaway.gtfs.model.Trip],
    classOf[org.onebusaway.gtfs.model.ServiceCalendarDate],
    classOf[org.onebusaway.gtfs.model.ServiceCalendar],
    classOf[org.onebusaway.gtfs.model.StopTime]).toList.asJavaCollection))
  reader.setEntityStore(dao)
//  reader.setInputSource(new ZipFileCsvInputSource(new ZipFile(
//    download("http://gtfs.ovapi.nl/openov-nl/gtfs-openov-nl.zip")
//  )))
  reader.setInputSource(new ZipFileCsvInputSource(new ZipFile(
    new File("/Users/koen/Downloads/gtfs-openov-nl.zip")
  )))

  logger.info("Reading file")
  reader.run()
  logger.info("Loaded all the trips")

  def download(url: String): File = {
    logger.info("Downloading " + url)
    val zipFile = File.createTempFile("gtfs", "zip")
    val input = new URL(url).openStream()
    val output = new FileOutputStream(zipFile)

    val buffer = new Array[Byte](4096)
    var bytesRead = 0
    while ({ bytesRead = input.read(buffer); bytesRead > 0 }) {
      output.write(buffer, 0, bytesRead)
    }

    output.close()
    input.close()

    logger.info("Downloaded " + url + " of " + zipFile.length() + " bytes")
    zipFile
  }

  def getTripInfo(tripId: String): Option[TripInfo] = dao.getTripInfo(tripId)
  def getTripInfoByRtId(tripId: String): Option[TripInfo] = dao.getTripInfoByRtId(tripId)
}
