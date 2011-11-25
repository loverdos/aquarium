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

package gr.grnet.aquarium
package store
package mongodb

import confmodel.{ServerAddressConfigurationModel, MongoDBConnectionModel}
import org.junit.Test
import org.junit.Assert._
import org.junit.Assume.assumeTrue

import com.ckkloverdos.resource.DefaultResourceContext
import MongoDBConnection.{RCFolders, PropFiles, DBNames, CollectionNames}
import gr.grnet.aquarium.util.xstream.XStreamHelpers
import com.ckkloverdos.sys.SysProp
import util.Loggable
import com.mongodb.{BasicDBObject, DBObject}
import com.ckkloverdos.maybe.Failed

/**
 * 
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
class MongoDBStoreTest extends Loggable {
  val baseRC = DefaultResourceContext
  val mongodbRC = baseRC / RCFolders.mongodb
  val xs = XStreamHelpers.newXStream

  lazy val MongoDBPropFile = {
    val filename = SysProp(PropertyNames.MongoDBConfFile).value.getOr(PropFiles.aquarium_message_store)
    logger.debug("Using mongodb configuration from %s".format(filename))
    filename
  }

//  private def _getTestConf: String = {
//    val address1 = ServerAddressConfigurationModel("aquarium.dev.grnet.gr", 27017)
//    val model = new MongoDBConnectionModel(List(address1), true, )
//    val xml = xs.toXML(model)
//    xml
//  }

  @Test
  def testConfigurationExists: Unit = {
    assertTrue(mongodbRC.getLocalResource(MongoDBPropFile).isJust)
  }

  @Test
  def testConnection: Unit = {
    assumeTrue(LogicTestsAssumptions.EnableMongoDBTests)

    assertTrue(mongodbRC.getLocalResource(MongoDBPropFile).isJust)
    for {
      confResource <- mongodbRC.getLocalResource(MongoDBPropFile)
    } {
      val maybeModel = XStreamHelpers.parseType[MongoDBConnectionModel](confResource, xs)
      maybeModel match {
        case Failed(e, m) => throw e
        case _ =>
      }
      assertTrue(maybeModel.isJust)

      for(model <- maybeModel) {
        logger.debug("Reading mongodb configuration from %s".format(confResource.url))
        logger.debug("mongodb configuration is:\n%s".format(confResource.stringContent.getOr("")))
        val obj = new BasicDBObject("1", 1)
        logger.debug("Inserting %s into mongodb".format(obj))
        val mongo = new MongoDBConnection(model)
        val store: Option[MessageStore] = mongo.findCollection("events")

        store match {
          case Some(store) =>
            store.storeString("{a: 1}")
          case None =>
            logger.warn("No store found")
        }
      }
    }
  }
}