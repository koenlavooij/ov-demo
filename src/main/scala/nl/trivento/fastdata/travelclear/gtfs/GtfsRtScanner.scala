package nl.trivento.fastdata.travelclear.gtfs

import java.net.URL

import akka.actor.{Actor, ActorSystem}
import com.google.transit.realtime.gtfs_realtime.{FeedMessage, VehiclePosition}
import org.slf4j.LoggerFactory

class GtfsRtScanner {
  private val logger = LoggerFactory.getLogger(classOf[GtfsRtScanner])

  def update(): Seq[VehiclePosition] = {
    val feed = FeedMessage.parseFrom(new URL("http://gtfs.ovapi.nl/new/vehiclePositions.pb").openStream())
    logger.info("Found feed: " + feed.header.timestamp + " V" + feed.header.gtfsRealtimeVersion)

    feed.entity.flatMap(entity => entity.vehicle)
  }
}

class PositionPublisher(tripReader: TripReader, actorSystem: ActorSystem) {
  val gtfsRt = new GtfsRtScanner()
  var mostRecent = Map.empty[String, TripPosition]

  private def publish(position: TripPosition): Unit = {
    if (position.info.active) {
      mostRecent = mostRecent + (position.info.tripId -> position)
    } else {
      mostRecent = mostRecent - position.info.tripId
    }
    actorSystem.eventStream.publish(position)
  }

  private def checkUpdate(position: TripPosition): Unit = {
    mostRecent.get(position.info.tripId) match {
      case Some(value) if value == position =>
      case _ => publish(position)
    }
  }

  def update(): Unit = {
    val positions = gtfsRt.update()
    var removed = mostRecent.keySet -- positions.flatMap(_.trip).flatMap(_.tripId).toSet

    for {
      update <- removed
      tripInfo <- tripReader.getTripInfo(update)
    } checkUpdate(TripPosition(TripRef(update, active = false, tripInfo.name, tripInfo.color, tripInfo.textColor), 0, 0))

    for {
      update <- positions
      position <- update.position
      tripInfo <- tripReader.getTripInfo(update.trip.get.tripId.get)
    } checkUpdate(TripPosition(TripRef(update.trip.get.tripId.get, active = true, tripInfo.name, tripInfo.color, tripInfo.textColor), position.latitude, position.longitude))
  }
}