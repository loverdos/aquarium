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

package gr.grnet.aquarium.processor.actor

import org.junit.Test
import org.junit.Assert._
import org.junit.Assume.assumeTrue

import akka.actor.Actor
import java.lang.Object

//import akka.actor.Actor.

object Constants {
  val RemoteHost = "localhost"
  val RemotePort = 2552
  val LocalHost  = "localhost"
  val LocalPort  = 2551
  val ActorNameEcho = "echo"
  val ActorNameSilent = "silent"
}

class EchoActor extends Actor {
  def receive = {
    case message =>
      println("%s received: %s".format(this, message))
      //self.reply("REPLY from EchoActor for '%s'".format(message))
  }
}

class SilentActor extends Actor {
  def receive = {
    case message =>
  }
}

abstract class ActorProxy(name: String, host: String = Constants.RemoteHost, port: Int = Constants.RemotePort) extends Actor {
  val remote = Actor.remote.actorFor(name, host, port)

  def receive = {
    case message =>
      remote ! message
  }
}

class EchoProxy extends ActorProxy("echo")
class SilentProxy extends ActorProxy("silent")

object ClientPart {
  import Constants._

  def start(): Unit = Actor.remote.start(LocalHost, LocalPort)
  def stop(): Unit  = Actor.remote.shutdownClientModule()

  val echo   = Actor.actorOf[EchoProxy].start()
  val silent = Actor.actorOf[SilentProxy].start()
}

object ServerPart {
  import Constants._

  def start(): Unit = {
    Actor.remote.start(RemoteHost, RemotePort)

    Actor.remote.register(ActorNameEcho, Actor.actorOf[EchoActor])
    Actor.remote.register(ActorNameSilent, Actor.actorOf[SilentActor])
  }

  def stop(): Unit  = Actor.remote.shutdownServerModule()
}


/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
class RemoteActorTest {
  @Test
  def testSendMessage: Unit = {
    try {
      ServerPart.start()
      ClientPart.start()

      ClientPart.echo ! "one"
      ClientPart.echo ! "two"
      ClientPart.echo ! "three"

      // Give us some delay to print to the console...
      Thread.sleep(100)
    }
    finally {
      ServerPart.stop()
      ClientPart.stop()
    }
  }
}