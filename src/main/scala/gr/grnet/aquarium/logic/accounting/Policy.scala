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

package gr.grnet.aquarium.logic.accounting

import dsl.{Timeslot, DSLPolicy, DSL}
import gr.grnet.aquarium.Configurator._
import gr.grnet.aquarium.util.Loggable
import java.io.{InputStream, FileInputStream, File}
import java.util.Date
import com.ckkloverdos.maybe.{NoVal, Maybe, Just}

/**
 * Searches for and loads the applicable accounting policy
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
object Policy extends DSL with Loggable {
  
  private var policies = {

    Map[Timeslot, DSLPolicy]()

  }
  
  lazy val policy = {

    // Look for user configured policy first
    val userConf = MasterConfigurator.props.get(Keys.aquarium_policy) match {
      case Just(x) => x
      case _ => logger.info("Cannot find a user configured policy")
        "policy.yaml"
    }

    val pol = new File(userConf)
    val stream = pol.exists() match {
      case true =>
        logger.info("Using policy file %s".format(userConf))
        new FileInputStream(pol)
      case false =>
        logger.warn(("Cannot find policy file %s, " +
          "looking for default policy").format(userConf))
        getClass.getClassLoader.getResourceAsStream("policy.yaml") match {
          case x: InputStream =>
            logger.warn("Using default policy, this is problably bad")
            x
          case null =>
            logger.error("No valid policy file found, Aquarium will fail")
            null
        }
    }

    parse(stream)
  }

  def policy(at: Date): DSLPolicy = {
    policies.find {
      a => a._1.from.before(at) &&
           a._1.to.after(at)
    } match {
      case Some(x) => x._2
      case None =>
        throw new AccountingException("No valid policy for date: %s".format(at))
    }
  }

  /**
   * Return the active policy for the provided userId at the provided timestamp
   */
  def policy(userId: String, timestamp: Date): Maybe[DSLPolicy] = {
    NoVal
  }
  
  def policies(from: Date, to: Date): List[DSLPolicy] = {
    policies.filter {
      a => a._1.from.before(from) &&
           a._1.to.after(to)
    }.values.toList
  }
  
  def policies(t: Timeslot): List[DSLPolicy] = policies(t.from, t.to)

  def reloadPolicyFile(): DSLPolicy = synchronized {
    // Look for user configured policy first
    val userConf = MasterConfigurator.props.get(Keys.aquarium_policy) match {
      case Just(x) => x
      case _ => logger.info("Cannot find a user configured policy")
      "policy.yaml"
    }

    val pol = new File(userConf)
    val stream = pol.exists() match {
      case true =>
        logger.info("Using policy file %s".format(userConf))
        new FileInputStream(pol)
      case false =>
        logger.warn(("Cannot find policy file %s, " +
          "looking for default policy").format(userConf))
        getClass.getClassLoader.getResourceAsStream("policy.yaml") match {
          case x: InputStream =>
            logger.warn("Using default policy, this is problably bad")
            x
          case null =>
            logger.error("No valid policy file found, Aquarium will fail")
            null
        }
    }
    parse(stream)
  }

  private def loadPolicies(): Map[Timeslot, DSLPolicy] = {
    //1. Load policies from db
    val store = MasterConfigurator.policyEventStore



    //2. Check whether policy file has been updated

    //3. Reload policy

    Map[Timeslot, DSLPolicy]()
  }
}