package nl.trivento.fastdata.travelclear.server

import akka.NotUsed
import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.{ActorMaterializer, Materializer, OverflowStrategy, ThrottleMode}
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import nl.trivento.fastdata.travelclear.gtfs._
import nl.trivento.fastdata.travelclear.ndovloket._

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
  implicit val tripRefFormat = jsonFormat5(TripRef)
  implicit val tripInfoFormat = jsonFormat6(TripInfo)
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
    val tripReader = new TripReader()
    val kv6Parser = KV6PositiesParser.create(tripReader)
    val nsParser = NsPositiesParser.create(tripReader)

    new OvLoketZmqClient(null).connect(
      Subscription(
        "tcp://pubsub.besteffort.ndovloket.nl:7658",
        (source: scala.io.Source) => kv6Parser
          .parse(source)
          .getAs[TripPosition]("ov")
          .foreach(_.foreach(actorSystem.eventStream.publish(_))),
        "/CXX/KV6posinfo", "/ARR/KV6posinfo", "/DITP/KV6posinfo", "/GVB/KV6posinfo", "/QBUZZ/KV6posinfo",
        "/RIG/KV6posinfo", "/Syntus/KV6posinfo"),

      Subscription(
        "tcp://pubsub.besteffort.ndovloket.nl:7664",
        (source: scala.io.Source) => nsParser
          .parse(source)
          .getAs[TripPosition]("trein")
          .foreach(_.foreach(actorSystem.eventStream.publish(_))),
        "/RIG/NStreinpositiesInterface5"))

    val publisher = new PositionPublisher(tripReader, actorSystem)
    val positions = actorSystem.actorOf(Props[PositionState])

    actorSystem.scheduler.schedule(1.seconds, 30.seconds) {
      publisher.update()
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
            tail => tripReader.getTripInfo(tail)
              .map(ti => complete(ti))
              .getOrElse(reject)
          }
        } ~
        path("listen") {
          val state: Source[TextMessage, NotUsed] =
            Source.actorRef[State](65536, OverflowStrategy.dropHead).mapMaterializedValue {
              outActor => {
                positions.tell(GetState(), outActor)
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
