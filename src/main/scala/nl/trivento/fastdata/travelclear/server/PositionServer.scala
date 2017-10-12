package nl.trivento.fastdata.travelclear.server

import akka.NotUsed
import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Route, StandardRoute}
import akka.stream.{ActorMaterializer, Materializer, OverflowStrategy, ThrottleMode}
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import nl.trivento.fastdata.travelclear.gtfs._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import spray.json._

case class Subscribe()

// collect your json format instances into a support trait:
trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val shapePointFormat = jsonFormat3(ShapePoint)
  implicit val stopFormat = jsonFormat3(Stop)
  implicit val stopTimeFormat = jsonFormat4(StopTime)
  implicit val tripInfoFormat = jsonFormat3(TripInfo)
}

object PositionServer extends JsonSupport with DefaultJsonProtocol {
  private implicit val actorSystem: ActorSystem = ActorSystem("connexxion")
  private implicit val executionContext: ExecutionContext = ExecutionContext.global
  private implicit val materializer: Materializer = ActorMaterializer.create(actorSystem)
  private val objectMapper = new ObjectMapper()
  objectMapper.registerModule(DefaultScalaModule)

  def main(args: Array[String]): Unit = {
    start()
  }

  def start(): Unit = {
    val reader = new TripReader()
    val publisher = actorSystem.actorOf(Props[PositionPublisher].withMailbox("minimal-mailbox"))

    actorSystem.scheduler.schedule(1.seconds, 30.seconds) {
      publisher ! DoUpdate()
    }

    val route: Flow[HttpRequest, HttpResponse, NotUsed] = Route.handlerFlow(
      get {
        pathSingleSlash {
          getFromFile("src/main/resources/index.html")
        } ~
        pathPrefix("static") {
          path(Remaining) {
            tail => getFromResource("static/" + tail)
          }
        } ~
        pathPrefix("trip") {
          path(Remaining) {
            tail => reader.getTripInfo(tail)
              .map(ti => complete(ti))
              .getOrElse(reject)
          }
        } ~
        path("listen") {
          val state: Source[TextMessage, NotUsed] =
            Source.actorRef[AllPositions](65536, OverflowStrategy.dropHead).mapMaterializedValue {
              outActor => {
                publisher.tell(GetAllPositions(), outActor)
                NotUsed
              }
            }
            .map(list => {
              println(list.tripPositions.length)
              TextMessage(objectMapper.writeValueAsString(list.tripPositions))
            })

          val updates: Source[TextMessage, NotUsed] =
            Source.actorRef[TripPosition](65536, OverflowStrategy.dropHead).mapMaterializedValue {
              outActor => {
                actorSystem.eventStream.subscribe(outActor, classOf[TripPosition])
                NotUsed
              }
            }
            .map(batch => objectMapper.writeValueAsString(batch))
            .map(v => ArrayBuffer[String](v))
            .batch(65536, v => v)((v, n) => { v.appendAll(n); v })
            .throttle(1, 1.second, 1, ThrottleMode.shaping)
            .map(l => TextMessage(l.addString(StringBuilder.newBuilder, "[", ",", "]").toString()))

          val cx = state.merge(updates, eagerComplete = true)

          handleWebSocketMessages(Flow.fromSinkAndSource(Sink.ignore, cx))
        }
      }
    )
    Http().bindAndHandle(route, "localhost", 8888)
  }
}