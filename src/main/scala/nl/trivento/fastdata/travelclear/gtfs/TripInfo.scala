package nl.trivento.fastdata.travelclear.gtfs

case class Stop(lat: Double, lng: Double, name: String)
case class StopTime(seq: Int, stop: Stop, arrival: Int, departure: Int)
case class ShapePoint(seq: Int, lat: Double, lng: Double)
case class TripRef(tripId: String, active: Boolean)
case class TripInfo(tripId: String, name: String, stopTime: Option[List[StopTime]], shapePoints: Option[List[ShapePoint]])
case class TripPosition(info: TripRef, lat: Double, lng: Double)