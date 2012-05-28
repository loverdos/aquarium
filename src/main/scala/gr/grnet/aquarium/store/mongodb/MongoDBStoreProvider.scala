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

  private[this] var _mongoDBStore: MongoDBStore = _

  def propertyPrefix = Some(MongoDBStoreProvider.MongoDBKeys.Prefix)

  def configure(props: Props) = {
    import MongoDBStoreProvider.MongoDBKeys

    this._database = props.getEx(MongoDBKeys.dbschema)
    this._username = props.getEx(MongoDBKeys.username)
    this._password = props.getEx(MongoDBKeys.password)
    val host = props.getEx(MongoDBKeys.host)
    val port = props.getEx(MongoDBKeys.port).toInt

    try {
      val addr = new ServerAddress(host, port)

      val opt = new MongoOptions()
      opt.connectionsPerHost = props.getEx(MongoDBKeys.connection_pool_size).toInt
      opt.threadsAllowedToBlockForConnectionMultiplier = 8

      this._mongo = new Mongo(addr, opt)
      this._mongoDBStore = new MongoDBStore(this._mongo, this._database, this._username, this._password)
    } catch {
      case e: MongoException =>
        throw new AquariumException("Cannot connect to mongo at %s:%s".format(host, port), e)
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

  /**
   * Note that these keys must be prefixed by `mongodb` in the configuration file
   *
   * @author Christos KK Loverdos <loverdos@gmail.com>
   */
  object MongoDBKeys {
    final val Prefix = "mongodb"
    final val PrefixAndDot = Prefix + "."

    private[this] def p(name: String) = PrefixAndDot + name

    /**
     * Hostname for the MongoDB
     */
    final val host = p("host")

    /**
     * Username for connecting to the MongoDB
     */
    final val username = p("username")

    /**
     *  Password for connecting to the MongoDB
     */
    final val password = p("password")

    /**
     *  Password for connecting to the MongoDB
     */
    final val port = p("port")

    /**
     *  The DB schema to use
     */
    final val dbschema = p("dbschema")

    /**
     * Maximum number of open connections to MongoDB
     */
    final val connection_pool_size = p("connection.pool.size")

  }
}