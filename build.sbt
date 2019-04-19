enablePlugins(PackPlugin)

name := "NetLogo-HPC"
organization := "nl.tudelft.tbm"
version := "0.1.4"

resolvers += Resolver.bintrayRepo("netlogo", "NetLogo-JVM")
resolvers += "sim0mq Public Repository" at "http://sim0mq.org/maven"
resolvers += "DSOL Public Repository" at "http://simulation.tudelft.nl/maven"
resolvers += "djunits Public Repository" at "http://djunits.org/maven"
resolvers += "Artima Maven Repository" at "http://repo.artima.com/releases"

libraryDependencies += "org.nlogo" % "netlogo" % "6.0.4"
libraryDependencies += "org.sim0mq" % "sim0mq" % "0.05.00"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"

packMain := Map("netlogo-hpc" -> "nl.tudelft.tbm.netlogo_hpc.Main")
packJvmOpts := Map("netlogo-hpc" -> Seq("-Xmx1536m", "-XX:+UseParallelGC", "-Dfile.encoding=UTF-8"))

