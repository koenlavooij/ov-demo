package nl.trivento.fastdata.travelclear.gtfs

case class Stop(lat: Double, lng: Double, name: String)
case class StopTime(seq: Int, stop: Stop, arrival: Int, departure: Int)
case class ShapePoint(seq: Int, lat: Double, lng: Double)
case class TripRef(trip: String, active: Boolean)
case class TripInfo(trip: String, stopTime: List[StopTime], shapePoints: List[ShapePoint])
case class TripPosition(info: TripRef, lat: Double, lng: Double)