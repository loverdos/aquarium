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

import confmodel.MongoDBConfigurationModel
import com.mongodb.casbah.MongoConnection
import com.mongodb.{WriteConcern, ServerAddress}
import gr.grnet.aquarium.util.Loggable

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
class MongoDBConnection(val confModel: MongoDBConfigurationModel) extends Loggable {

  private[mongodb] lazy val _mongoConnection = {
    val hosts = confModel.hosts
    val serverAddresses = hosts.map(sacm => new ServerAddress(sacm.host, sacm.port))

    val mongo = MongoConnection(serverAddresses)
    logger.info("Created MongoDB connection %s".format(mongo))

    if(confModel.slaveOK) {
      mongo.slaveOk()
      logger.info("Set slaveOK for MongoDB connection %s".format(mongo))
    }
    val writeConcern = WriteConcern.valueOf(confModel.writeConcern)
    mongo.setWriteConcern(writeConcern)
    logger.info("Set WriteConcern %s for MongoDB connection %s".format(confModel.writeConcern, mongo))

    mongo
  }
}

object MongoDBConnection {
  object RCFolders {
    val mongodb = "mongodb"
  }

  object PropFiles {
    val local_message_store = "local-message-store.xml"
  }
  
  object DBNames {
    val test = "test"
  }

  object CollectionNames {
      val test = "test"
    }
}