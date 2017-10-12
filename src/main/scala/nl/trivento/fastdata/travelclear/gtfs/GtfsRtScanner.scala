package nl.trivento.fastdata.travelclear.gtfs

import java.net.URL

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