import sbtassembly.Plugin._
import AssemblyKeys._

assemblySettings

name := "sharingan"
 
version := "1.1"
 
scalaVersion := "2.10.1"
 
resolvers ++= Seq("Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
		  "releases" at "http://oss.sonatype.org/content/repositories/releases",
                  "Eligosource Releases" at "http://repo.eligotech.com/nexus/content/repositories/eligosource-releases",
		  "Eligosource Snapshots" at "http://repo.eligotech.com/nexus/content/repositories/eligosource-snapshots")
 
libraryDependencies ++= Seq("com.typesafe.akka" %% "akka-actor" % "2.2.0-RC1",
			"com.typesafe.akka" % "akka-kernel_2.10" % "2.2.0-RC1",
			"org.mashupbots.socko" %% "socko-webserver" % "0.3.0",
			"net.liftweb" %% "lift-json" % "2.5-RC6",
			"org.eligosource" %% "eventsourced-core" % "0.6-SNAPSHOT",
			"org.eligosource" %% "eventsourced-journal-journalio" % "0.6-SNAPSHOT",
			"org.eligosource" %% "eventsourced-journal-leveldb" % "0.6-SNAPSHOT",
			"org.eligosource" %% "eventsourced-journal-inmem" % "0.6-SNAPSHOT")



fork := true

javaOptions += "-Xbootclasspath/p:/home/bongani/Documents/npn-boot-1.1.3.v20130313.jar"

mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
  {
    case PathList("javax", "servlet", xs @ _*) => MergeStrategy.first
    case x if x endsWith "pom.properties" => MergeStrategy.first
    case x if x endsWith "pom.xml" => MergeStrategy.concat
    case "reference.conf" | "rootdoc.txt" => MergeStrategy.concat
    case x if x endsWith "NOTICE.txt" => MergeStrategy.concat
    case x if x endsWith "NOTICE" => MergeStrategy.concat
    case x if x endsWith "LICENSE.txt" => MergeStrategy.concat
    case x if x endsWith "LICENSE" => MergeStrategy.concat
    case PathList("META-INF", xs @ _*) =>
      (xs map {_.toLowerCase}) match {
        case ("manifest.mf" :: Nil) | ("index.list" :: Nil) | ("dependencies" :: Nil) =>
          MergeStrategy.discard
        case ps @ (x :: xs) if ps.last.endsWith(".sf") || ps.last.endsWith(".dsa") =>
          MergeStrategy.discard
        case "plexus" :: xs =>
          MergeStrategy.discard
        case "services" :: xs =>
          MergeStrategy.filterDistinctLines
        case ("spring.schemas" :: Nil) | ("spring.handlers" :: Nil) =>
          MergeStrategy.filterDistinctLines
        case _ => MergeStrategy.deduplicate
      }
    case x if x endsWith "IsNull.class" => MergeStrategy.concat
    case x => MergeStrategy.first
  }
}
