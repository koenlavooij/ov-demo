package nl.trivento.fastdata.travelclear.ndovloket

import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream

import akka.actor.ActorSystem
import nl.trivento.fastdata.travelclear.gtfs.TripReader
import org.apache.commons.io.input.BOMInputStream
import org.zeromq.ZMQ

import scala.io.Source

case class Subscription(url: String, msgHandler: Source => Unit, names: String*)

class OvLoketZmqClient(tripReader: TripReader) {
  private val context = ZMQ.context(1)
  private val requester = context.socket(ZMQ.SUB)

  def connect(subscriptions: Subscription*) {
    val subMap = subscriptions.flatMap(subscription => subscription.names.map(name => name -> subscription)).toMap

    subscriptions.foreach(subscription => {
      requester.connect(subscription.url)
      subscription.names.foreach(requester.subscribe)
    })

    new Thread(() => {
      while (true) {
        val topic = new String(requester.recv())
        println(topic)
        subMap
          .get(topic)
          .foreach(_.msgHandler(
            Source.fromInputStream(
              new BOMInputStream(
                new GZIPInputStream(
                  new ByteArrayInputStream(requester.recv())
                )
              )
            )
          ))
      }
    }).start()
  }
}

object OvLoketZmqClient {
  def main(args: Array[String]): Unit = {
    val tripReader = new TripReader()
    val kv6Parser = KV6PositiesParser.create(tripReader)
    val nsParser = NsPositiesParser.create(tripReader)
    kv6Parser.parse(Source.fromResource("kv6-example-arr.xml"))

    new OvLoketZmqClient(null).connect(
      Subscription(
        "tcp://pubsub.besteffort.ndovloket.nl:7658",
        (source: Source) => println(kv6Parser.parse(source).getAs[Position]("ov")),
        "/CXX/KV6posinfo", "/ARR/KV6posinfo", "/DITP/KV6posinfo", "/GVB/KV6posinfo", "/QBUZZ/KV6posinfo", "/RIG/KV6posinfo", "/Syntus/KV6posinfo"),
      Subscription(
        "tcp://pubsub.besteffort.ndovloket.nl:7664",
        (source: Source) => println(nsParser.parse(source).getAs[Position]("trein")),
        "/RIG/NStreinpositiesInterface5"))
  }
}

case class Position(identifier: String, latitude: Double, longitude: Double)
