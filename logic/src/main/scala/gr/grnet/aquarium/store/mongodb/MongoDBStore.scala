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

package gr.grnet.aquarium.store.mongodb

import gr.grnet.aquarium.util.Loggable
import com.mongodb._
import gr.grnet.aquarium.store.{MessageStoreException, MessageStore}

/**
 * Mongodb implementation of the message store.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
class MongoDBStore(host: String, port: String,
                   username: String, passwd: String)
  extends MessageStore with Loggable {

  private[mongo] object Connection {
    lazy val mongo: Option[Mongo] = {
      try {
        val addr = new ServerAddress(host, port.toInt)
        val opt = new MongoOptions()
        Some(new Mongo(addr, opt))
      } catch {
        case e: MongoException =>
          logger.error(("Cannot connect to mongo at %s:%s (uname=%s). " +
            "Cause:").format(host,port,username, e))
          None
        case nfe: NumberFormatException =>
          logger.error("%s is not a valid port number".format(port))
          None
      }
    }
  }

  private[store] lazy val events: DB = {
    Connection.mongo match {
      case Some(x) => x.getDB("events")
      case None => throw new MessageStoreException("No connection to Mongo")
    }
  }

  private[store] lazy val users: DB = {
    Connection.mongo match {
      case Some(x) => x.getDB("users")
      case None => throw new MessageStoreException("No connection to Mongo")
    }
  }

  def storeString(message: String) = {

  }
}
