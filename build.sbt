name := "sharingan"
 
version := "1.0"
 
scalaVersion := "2.10.0"
 
resolvers ++= Seq("Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
		  "releases" at "http://oss.sonatype.org/content/repositories/releases")
 
libraryDependencies ++= Seq("com.typesafe.akka" %% "akka-actor" % "2.1.0",
				"org.mashupbots.socko" %% "socko-webserver" % "0.3.0",
				 "net.liftweb" %% "lift-json" % "2.5-RC6")

fork := true

javaOptions += "-Xbootclasspath/p:/home/bongani/Documents/npn-boot-1.1.3.v20130313.jar"