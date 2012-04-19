/*
 * Copyright 2011-2012 GRNET S.A. All rights reserved.
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

import com.ckkloverdos.props.Props
import gr.grnet.aquarium.Configurator.Keys
import com.mongodb.{MongoException, Mongo, MongoOptions, ServerAddress}
import gr.grnet.aquarium.store._
import gr.grnet.aquarium.{AquariumException, Configurable}

/**
 * 
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

class MongoDBStoreProvider extends StoreProvider with Configurable {
  private[this] var _mongo: Mongo = _
  private[this] var _database: String = _
  private[this] var _username: String = _
  private[this] var _password: String = _

  def configure(props: Props) = {
    this._database = props.getEx(Keys.persistence_db)
    this._username = props.getEx(Keys.persistence_username)
    this._password = props.getEx(Keys.persistence_password)
    val host = props.getEx(Keys.persistence_host)
    val port = props.getEx(Keys.persistence_port).toInt

    try {
      val addr = new ServerAddress(host, port)

      val opt = new MongoOptions()
      opt.connectionsPerHost = props.getEx(Keys.mongo_connection_pool_size).toInt
      opt.threadsAllowedToBlockForConnectionMultiplier = 8

      this._mongo = new Mongo(addr, opt)
    } catch {
      case e: MongoException =>
        throw new AquariumException("Cannot connect to mongo at %s:%s".format(host, port), e)
    }
  }

  def userStateStore = new MongoDBStore(this._mongo, this._database, this._username, this._password)
  def resourceEventStore = new MongoDBStore(this._mongo, this._database, this._username, this._password)
  def walletEntryStore = new MongoDBStore(this._mongo, this._database, this._username, this._password)
  def userEventStore = new MongoDBStore(this._mongo, this._database, this._username, this._password)
  def policyStore = new MongoDBStore(this._mongo, this._database, this._username, this._password)
}