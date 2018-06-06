name := "NetLogoCluster"
organization := "nl.tudelft.tbm"
version := "0.1.0"

resolvers += Resolver.bintrayRepo("netlogo", "NetLogo-JVM")
libraryDependencies += "org.nlogo" % "netlogo" % "6.0.3"
