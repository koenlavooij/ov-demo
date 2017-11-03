package nl.trivento.fastdata.travelclear.server

import akka.actor.Actor
import nl.trivento.fastdata.travelclear.gtfs.TripPosition

import scala.collection.mutable

case class GetState()
case class State(tripPositions: Seq[TripPosition])

class PositionState extends Actor {
  val state = mutable.Map.empty[String, TripPosition]

  override def preStart(): Unit = {
    context.system.eventStream.subscribe(self, classOf[TripPosition])
  }

  override def receive = {
    case t: TripPosition if t.info.active => state.put(t.info.tripId, t)
    case t: TripPosition if !t.info.active => state.remove(t.info.tripId)
    case GetState() => sender ! State(state.values.toList)
  }
}
