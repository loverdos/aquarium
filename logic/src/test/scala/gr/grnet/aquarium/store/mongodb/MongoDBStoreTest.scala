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

import confmodel.{ServerAddressConfigurationModel, MongoDBConfigurationModel}
import org.junit.Test
import org.junit.Assert._
import org.junit.Assume.assumeTrue

import com.ckkloverdos.resource.DefaultResourceContext
import MongoDBConnection.{RCFolders, PropFiles, DBNames, CollectionNames}
import gr.grnet.aquarium.util.xstream.XStreamHelpers
import com.mongodb.casbah.commons.MongoDBObject

/**
 * 
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
class MongoDBStoreTest {
  val baseRC = DefaultResourceContext
  val mongodbRC = baseRC / RCFolders.mongodb
  val xs = XStreamHelpers.newXStream

  private def _getTestConf: String = {
    val address1 = ServerAddressConfigurationModel("aquarium.dev.grnet.gr", 27017)
    val model = new MongoDBConfigurationModel(List(address1), true, "SAFE")
    val xml = xs.toXML(model)
    xml
  }

  @Test
  def testConfigurationExists: Unit = {
    assertTrue(mongodbRC.getLocalResource(PropFiles.local_message_store).isJust)
  }

  @Test
  def testConnection: Unit = {
    assumeTrue(LogicTestsAssumptions.EnableMongoDBTests)

    for {
      confResource <- mongodbRC.getLocalResource(PropFiles.local_message_store)
    } {
      val xs = XStreamHelpers.newXStream
      val maybeModel = XStreamHelpers.parseType[MongoDBConfigurationModel](confResource, xs)
      assertTrue(maybeModel.isJust)
      for(model <- maybeModel) {
        val mongo = new MongoDBConnection(model)
        println(mongo._mongoConnection)
        println(mongo._mongoConnection.getAllAddress())
        println(mongo._mongoConnection.getConnectPoint())
        val db = mongo._mongoConnection(DBNames.test)
        val collection = db.apply(CollectionNames.test)
        val obj = MongoDBObject("1" -> "one", "2" -> "two")
        collection.insert(obj)
      }
    }
  }
}