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


  override def close(): Unit = {
    super.close()

    logger.info("Trips: " + getAllTrips.size())
    getAllTrips.asScala.foreach(trip => {
      val info = TripInfo(
        trip.getId.getId,
        trip.getRoute.getShortName + " " + trip.getRoute.getLongName,
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
  reader.setInputSource(new ZipFileCsvInputSource(new ZipFile(
    download("http://gtfs.ovapi.nl/openov-nl/gtfs-openov-nl.zip")
  )))
//  reader.setInputSource(new ZipFileCsvInputSource(new ZipFile(
//    new File("/Users/koen/Downloads/gtfs-openov-nl.zip")
//  )))

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

case class DoUpdate()
case class GetAllPositions()
case class AllPositions(tripPositions: Seq[TripPosition])

class PositionPublisher extends Actor {
  val gtfsRt = new GtfsRtScanner()
  var mostRecent = Map.empty[String, TripPosition]

  private def publish(position: TripPosition): Unit = {
    if (position.info.active) {
      mostRecent = mostRecent + (position.info.tripId -> position)
    } else {
      mostRecent = mostRecent - position.info.tripId
    }
    context.system.eventStream.publish(position)
  }

  private def checkUpdate(position: TripPosition): Unit = {
    mostRecent.get(position.info.tripId) match {
      case Some(value) if value == position =>
      case _ => publish(position)
    }
  }

  def receive: Receive = {
    case GetAllPositions() => sender() ! AllPositions(mostRecent.values.toSeq)
    case DoUpdate() =>
      val positions = gtfsRt.update()
      var removed = mostRecent.keySet -- positions.flatMap(_.trip).flatMap(_.tripId).toSet

      for {
        update <- removed
      } checkUpdate(TripPosition(TripRef(update, active = false), 0, 0))

      for {
        update <- positions
        position <- update.position
      } checkUpdate(TripPosition(TripRef(update.trip.get.tripId.get, active = true), position.latitude, position.longitude))
  }
}