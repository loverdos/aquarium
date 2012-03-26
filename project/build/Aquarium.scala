/*
 * Copyright 2011 GRNET S.A. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 *   1. Redistributions of source code must retain the above
 *      copyright notice, this list of conditions and the following
 *      disclaimer.
 *
 *   2. Redistributions in binary form must reproduce the above
 *      copyright notice, this list of conditions and the following
 *      disclaimer in the documentation and/or other materials
 *      provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY GRNET S.A. ``AS IS'' AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT l TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL GRNET S.A OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and
 * documentation are those of the authors and should not be
 * interpreted as representing official policies, either expressed
 * or implied, of GRNET S.A.
 */

import sbt._

class Aquarium(info: ProjectInfo) extends DefaultProject(info) {
  object Versions {
    final val akka  = "1.3.1"
    final val lift  = "2.4"
    final val maybe = "0.3.0"
  }

  def akkaModule(what: String): String = "akka-%s".format(what)

  def liftModule(what: String): String = "lift-%s".format(what)

  object Deps {
    def akka(what: String) = "se.scalablesolutions.akka" % akkaModule(what) % Versions.akka

    def lift(what: String) = "net.liftweb" %% liftModule(what) % Versions.lift
  }

  override def compileOptions = super.compileOptions ++
    Seq("-deprecation",
      "-Xmigration",
      "-Xcheckinit",
      "-optimise",
      "-explaintypes",
      "-unchecked",
      "-encoding", "utf8")
      .map(CompileOption(_))

  override def testOptions =
    super.testOptions ++
      Seq(TestArgument(TestFrameworks.JUnit, "-q", "-v"))

  override def packageDocsJar = defaultJarPath("-javadoc.jar")

  override def packageSrcJar = defaultJarPath("-sources.jar")

  val sourceArtifact = Artifact.sources(artifactID)
  val docsArtifact = Artifact.javadoc(artifactID)

  override def packageToPublishActions = super.packageToPublishActions ++ Seq(packageDocs, packageSrc)

  override def packageAction = super.packageAction dependsOn test

  override def managedStyle = ManagedStyle.Maven

  val localDestRepo = Resolver.file("maven-local", Path.userHome / ".m2" / "repository" asFile)

  override def parallelExecution = false

  val repo0 = "aquarium nexus" at "http://aquarium.dev.grnet.gr:8081/nexus/content/groups/public"
  val repo1 = "java.net.maven2" at "http://download.java.net/maven/2/"
  val repo2 = "EclipseLink Repo" at "http://download.eclipse.org/rt/eclipselink/maven.repo"
  val repo3 = "jboss" at "http://repository.jboss.org/nexus/content/groups/public/"
  val repo4 = "sonatype" at "http://oss.sonatype.org/content/groups/public/"
  val repo5 = "jcrontab" at "http://kenai.com/projects/crontab-parser/sources/maven-repo/content/"
  val repo6 = "typsafe" at "http://repo.typesafe.com/typesafe/releases/"
  val repo7 = "akka" at "http://akka.io/repository/"
  val repo8 = "twitter" at "http://maven.twttr.com"
  val repo9 = "tools-snapshots" at "http://scala-tools.org/repo-snapshots"
  val repoA = "tools-releases" at "http://scala-tools.org/repo-releases"
  val repoB = "sonatype-snapshots" at "http://oss.sonatype.org/content/repositories/snapshots"
  val repoC = "jboss scala-tools" at "https://repository.jboss.org/nexus/content/repositories/scala-tools-releases"
  val repoD = "spray" at "http://repo.spray.cc/"

  val lib_slf4j = "org.slf4j" % "slf4j-api" % "1.6.1" withSources()

  val lib_liftjson  = Deps.lift("json")     withSources()
  val lib_liftjsonX = Deps.lift("json-ext") withSources()

  val lib_akka_actor  = Deps.akka("actor")            withSources()
  val lib_akka_remote = Deps.akka("remote")           withSources()
  val lib_akka_test   = Deps.akka("testkit") % "test" withSources()
  val lib_akka_amqp   = Deps.akka("amqp")             withSources()

  val lib_yaml = "org.yaml" % "snakeyaml" % "1.9" withSources()
  val lib_jcrontab = "com.kenai.crontab-parser" % "crontab-parser" % "1.0.1" withSources()
  val lib_xstream = "com.thoughtworks.xstream" % "xstream" % "1.4.1" withSources()
  val lib_rabbit = "com.rabbitmq" % "amqp-client" % "2.5.0" withSources()
  val lib_mongo = "org.mongodb" % "mongo-java-driver" % "2.7.2" withSources()


//  val lib_javaxrs = "javax.ws.rs" % "jsr311-api" % "1.1.1" withSources()
  val lib_spray_can = "cc.spray" % "spray-can" % "0.9.2" withSources()

  val lib_maybe = "com.ckkloverdos" %% "maybe" % Versions.maybe withSources()
  val lib_converter = "com.ckkloverdos" %% "converter" % "0.3.0" withSources()
  val lib_sysprop = "com.ckkloverdos" %% "sysprop" % "0.1.0" withSources()
  val lib_streamresource = "com.ckkloverdos" %% "streamresource" % "0.2.0" withSources()

  val lib_lucene_core = "org.apache.lucene" % "lucene-core" % "3.5.0" withSources()
  val lib_solr_core = "org.apache.solr" % "solr-core" % "3.5.0" withSources()

  val lib_scopt = "com.github.scopt" %% "scopt" % "2.0.1" withSources()

  val lib_joda = "joda-time" % "joda-time" % "2.0" withSources()
  val lib_joda_conv = "org.joda" % "joda-convert" % "1.1" withSources()

  val lib_test = "com.novocode" % "junit-interface" % "0.7" % "test->default"

  val lib_guava = "com.google.guava" % "guava" % "11.0.2" withSources()
}