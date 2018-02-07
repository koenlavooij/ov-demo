name := "GtfsRt"

version := "0.1"

scalaVersion := "2.12.3"

libraryDependencies += "org.slf4j" % "slf4j-log4j12" % "1.7.25"
libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.25"
libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.0.10"
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.10"
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.5.6"
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.6"
libraryDependencies += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.8.7"
libraryDependencies += "org.zeromq" % "jeromq" % "0.4.2"
libraryDependencies += "org.scala-lang.modules" % "scala-xml_2.12" % "1.0.6"
libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.6"
libraryDependencies += "commons-io" % "commons-io" % "2.3"
libraryDependencies += "org.mapdb" % "mapdb" % "3.0.5"
libraryDependencies += "org.lmdbjava" % "lmdbjava" % "0.6.0"

libraryDependencies += "com.googlecode.concurrentlinkedhashmap" % "concurrentlinkedhashmap-lru" % "1.4.2"
libraryDependencies += "org.onebusaway" % "onebusaway-gtfs" % "1.3.4"
libraryDependencies += "org.openstreetmap.osmosis" % "osmosis-pbf" % "0.46"

libraryDependencies += "org.scalatest" % "scalatest_2.12" % "3.0.4" % "test"

PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)