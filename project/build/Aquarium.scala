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
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
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

class Aquarium(info: ProjectInfo) extends ParentProject(info) {
	override def parallelExecution = false

	val repo0 = "aquarium nexus" at "http://aquarium.dev.grnet.gr:8081/nexus/content/groups/public"
	//val repo1 = "java.net.maven2" at "http://download.java.net/maven/2/"
	//val repo2 = "EclipseLink Repo" at "http://download.eclipse.org/rt/eclipselink/maven.repo"
	//val repo3 = "jboss" at "http://repository.jboss.org/nexus/content/groups/public/"
	//val repo4 = "sonatype" at "http://oss.sonatype.org/content/groups/public/"
	//val repo5 = "jcrontab" at "http://kenai.com/projects/crontab-parser/sources/maven-repo/content/"

	val lib_slf4j     = "org.slf4j"      % "slf4j-api"            % "1.6.1"   withSources()
	val lib_h2        = "com.h2database" % "h2"                   % "1.3.160" withSources()
	val lib_mysql     = "mysql"          % "mysql-connector-java" % "5.1.17"  
	val lib_scalajpa  = "org.scala-libs" % "scalajpa_2.9.1"       % "1.4"     withSources()
	val lib_elink     = "org.eclipse.persistence"  % "eclipselink" % "2.2.0"   withSources()

	lazy val logic = project("logic", "logic", new Logic(_), model)
	lazy val model = project("model", "model", new Model(_))
	lazy val shared = project("shared", "shared", new Shared(_))

	class Logic(info: ProjectInfo) extends DefaultProject(info) {
		val lib_scalajpa  = "org.scala-libs" % "scalajpa_2.9.1"  % "1.4"            withSources()
		val lib_liftjson  = "net.liftweb"    % "lift-json_2.9.1" % "2.4-M5"         withSources()
		val lib_liftjsonX = "net.liftweb"    % "lift-json-ext_2.9.1" % "2.4-M5"         withSources()
		val lib_yaml      = "org.yaml"       % "snakeyaml"       % "1.9"  withSources()
		val lib_jcrontab  = "com.kenai.crontab-parser" % "crontab-parser" % "1.0.1" withSources()
		val lib_xstream   = "com.thoughtworks.xstream" % "xstream"     % "1.4.1" withSources()
		val lib_rabbit    = "com.rabbitmq"   % "amqp-client"     % "2.7.0" withSources()

		val lib_converter      = "com.ckkloverdos" % "converter_2.9.1"      % "0.3.0" withSources()
		val lib_streamresource = "com.ckkloverdos" % "streamresource_2.9.1" % "0.2.0" withSources()


		// val model = project(Path.fromFile("../model"), "model", new Model(_))
	}

	class Model(info: ProjectInfo) extends DefaultProject(info) {
		val lib_scalajpa  = "org.scala-libs" % "scalajpa_2.9.1"  % "1.4"            withSources()
		val lib_persist = "org.eclipse.persistence" % "javax.persistence" % "2.0.3" withSources()

		val lib_junit     = "junit" % "junit" % "4.10" % "test" withSources()
	}

	class Shared(info: ProjectInfo) extends DefaultProject(info) {
		// dummy, to avoid looking at pom.xml
		val lib_persist = "org.eclipse.persistence" % "javax.persistence" % "2.0.3" withSources()
	}
}