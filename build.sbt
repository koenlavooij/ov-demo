name := "GtfsRt"

version := "0.1"

scalaVersion := "2.12.3"

libraryDependencies += "org.onebusaway" % "onebusaway-gtfs" % "1.3.4"
libraryDependencies += "org.slf4j" % "slf4j-log4j12" % "1.7.25"
libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.25"
libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.0.10"
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.10"
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.5.6"
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.6"
libraryDependencies += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.8.7"

PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)