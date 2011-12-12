/*
 * Copyright 2011 GRNET S.A. All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or
 *  without modification, are permitted provided that the following
 *  conditions are met:
 *
 *    1. Redistributions of source code must retain the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 *  THIS SOFTWARE IS PROVIDED BY GRNET S.A. ``AS IS'' AND ANY EXPRESS
 *  OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL GRNET S.A OR
 *  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 *  USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 *  AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *  LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *  ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 *
 *  The views and conclusions contained in the software and
 *  documentation are those of the authors and should not be
 *  interpreted as representing official policies, either expressed
 *  or implied, of GRNET S.A.
 */

package gr.grnet.aquarium.store

import gr.grnet.aquarium.util.Loggable
import gr.grnet.aquarium.MasterConf
import mongodb.MongoDBStore

/**
 * A factory class for getting access to the underlying message store.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
object Store extends Loggable {

  private lazy val provider = {
    MasterConf.MasterConf.get(MasterConf.Keys.persistence_provider)
  }

  private lazy val host = {
    MasterConf.MasterConf.get(MasterConf.Keys.persistence_host)
  }

  private lazy val uname = {
    MasterConf.MasterConf.get(MasterConf.Keys.persistence_username)
  }

  private lazy val passwd = {
    MasterConf.MasterConf.get(MasterConf.Keys.persistence_password)
  }

  private lazy val port = {
    MasterConf.MasterConf.get(MasterConf.Keys.persistence_port)
  }

  private lazy val db = {
    MasterConf.MasterConf.get(MasterConf.Keys.persistence_db)
  }

  def getEventStore(): Option[EventStore] = {
    provider match {
      case "mongodb" =>
        Some(new MongoDBStore(host, port, uname, passwd, db))
      case _ => 
        logger.error("Provider <%s> not supported".format(provider))
        None
    }
  }
}