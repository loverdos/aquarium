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

package gr.grnet.aquarium.store
package mongodb

import confmodel.MongoDBConnectionModel
import gr.grnet.aquarium.util.Loggable
import scala.collection.JavaConversions._
import com.mongodb.{Mongo, WriteConcern, ServerAddress}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
class MongoDBConnection(val confModel: MongoDBConnectionModel) extends Loggable {

  private[mongodb] lazy val _mongo = {
    val hosts = confModel.hosts

    val mongo = if(hosts.size == 1) {
      val sacm = hosts.head
      new Mongo(sacm.host, sacm.port)
    } else {
      val serverAddresses = hosts.map(sacm => new ServerAddress(sacm.host, sacm.port))
      new Mongo(serverAddresses)
    }

    mongo
  }


  private lazy val _collections = confModel.collections.map(new MongoDBCollection(this, _))
  def collections: List[MongoDBCollection] = _collections

  def findCollection(name: String): Option[MongoDBCollection] = collections.find(_.name == name)
}

object MongoDBConnection {
  object RCFolders {
    val mongodb = "mongodb"
  }

  object PropFiles {
    val local_message_store = "local-message-store.xml"
    val aquarium_message_store = "aquarium-message-store.xml"
  }
  
  object DBNames {
    val test = "test"
    val aquarium = "aquarium"
  }

  object CollectionNames {
    val test = "test"
    val events = "events"
  }
}