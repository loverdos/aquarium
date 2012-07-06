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
import com.mongodb.{MongoException, Mongo, MongoOptions, ServerAddress}
import gr.grnet.aquarium.store._
import gr.grnet.aquarium.{AquariumAwareSkeleton, AquariumException, Configurable}
import gr.grnet.aquarium.service.event.AquariumCreatedEvent
import com.google.common.eventbus.Subscribe
import com.ckkloverdos.key.{IntKey, StringKey}
import gr.grnet.aquarium.util.Loggable

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

class MongoDBStoreProvider extends StoreProvider with Configurable with Loggable with AquariumAwareSkeleton {
  private case class ConnectionData(
      database: String,
      host: String,
      port: Int,
      username: String,
      password: String,
      connectionsPerHost: Int,
      threadsAllowedToBlockForConnectionMultiplier: Int
  )

  private[this] var _mongo: Mongo = _
  private[this] var _connectionData: ConnectionData = _
  private[this] var _mongoDBStore: MongoDBStore = _

  def propertyPrefix = Some(MongoDBStoreProvider.Prefix)

  def configure(props: Props) = {
    import MongoDBStoreProvider.EnvKeys

    this._connectionData = ConnectionData(
      database = props.getEx(EnvKeys.mongodbDatabase.name),
      host =  props.getEx(EnvKeys.mongodbHost.name),
      port = props.getIntEx(EnvKeys.mongodbPort.name),
      username = props.getEx(EnvKeys.mongodbUsername.name),
      password = props.getEx(EnvKeys.mongodbPassword.name),
      connectionsPerHost = props.getInt(EnvKeys.mongodbConnectionsPerHost.name).getOr(20),
      threadsAllowedToBlockForConnectionMultiplier = props.getInt(
        EnvKeys.mongodbThreadsAllowedToBlockForConnectionMultiplier.name).getOr(5)
    )
  }

  @Subscribe
  override def awareOfAquarium(event: AquariumCreatedEvent) {
    super.awareOfAquarium(event)

    doSetup()
  }

  private def doSetup() {
    try {
      val host = this._connectionData.host
      val port = this._connectionData.port
      val serverAddress = new ServerAddress(host, port)

      val opt = new MongoOptions()
      opt.connectionsPerHost = this._connectionData.connectionsPerHost
      opt.threadsAllowedToBlockForConnectionMultiplier = this._connectionData.threadsAllowedToBlockForConnectionMultiplier

      this._mongo = new Mongo(serverAddress, opt)
      this._mongoDBStore = new MongoDBStore(
        aquarium,
        this._mongo,
        this._connectionData.database,
        this._connectionData.username,
        this._connectionData.password
      )
    } catch {
      case e: MongoException ⇒
        throw new AquariumException("While connecting to MongoDB using %s".format(this._connectionData), e)

      case e: Exception ⇒
        throw new AquariumException("While connecting to MongoDB using %s".format(this._connectionData), e)
    }
  }

  def userStateStore = _mongoDBStore
  def resourceEventStore = _mongoDBStore
  def imEventStore = _mongoDBStore
  def policyStore = _mongoDBStore
}

/**
 * Provides configuration keys.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
object MongoDBStoreProvider {
  final val Prefix = "mongodb"
  final val PrefixAndDot = Prefix + "."

  object EnvKeys {
    /**
     * Hostname for the MongoDB
     */
    final val mongodbHost = StringKey(PrefixAndDot + "host")


    final val mongodbPort     = IntKey   (PrefixAndDot + "port")

    /**
     * Username for connecting to the MongoDB.
     */
    final val mongodbUsername = StringKey(PrefixAndDot + "username")

    /**
     *  Password for connecting to the MongoDB.
     */
    final val mongodbPassword = StringKey(PrefixAndDot + "password")

    /**
     *  The MongoDB database to use.
     */
    final val mongodbDatabase = StringKey(PrefixAndDot + "database")

    final val mongodbConnectionsPerHost = IntKey(PrefixAndDot + "connections.per.host")

    final val mongodbThreadsAllowedToBlockForConnectionMultiplier =
      IntKey("threads.allowed.to.block.for.connection.multiplier")
  }
}