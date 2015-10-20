name := "json-rest-client"

organization := "com.sparetimecoders"

libraryDependencies ++= Seq(
  "org.codehaus.jackson" % "jackson-mapper-asl" % "1.8.5",
  "org.apache.httpcomponents" % "httpclient" % "4.3",
  "org.apache.commons" % "commons-lang3" % "3.3.2",
  "joda-time" % "joda-time" % "2.5",
  "org.slf4j" % "jul-to-slf4j" % "1.7.10",
  "com.ning" % "async-http-client" % "1.7.12",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.1.4",
  "junit" % "junit" % "4.11" % "test",
  "org.mockito" % "mockito-all" % "1.10.8" % "test",
  "com.github.tomakehurst" % "wiremock" % "1.53" % "test",
  "com.damnhandy" % "handy-uri-templates" % "1.1.7",
  "com.novocode" % "junit-interface" % "0.8" % "test->default"
)

publishArtifact := true

publishMavenStyle := true

crossPaths := false

autoScalaLibrary := false

parallelExecution in Test := false

fork in Test := true

javacOptions in compile ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")

javacOptions in doc ++= Seq("-source", "1.8", "-Xdoclint:none")

compileOrder := CompileOrder.JavaThenScala

