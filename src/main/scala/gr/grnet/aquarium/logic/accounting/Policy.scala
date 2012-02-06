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
import java.io.{InputStream, FileInputStream, File}
import java.util.Date
import gr.grnet.aquarium.util.date.TimeHelpers
import gr.grnet.aquarium.logic.events.PolicyEntry
import gr.grnet.aquarium.util.{CryptoUtils, Loggable}
import java.util.concurrent.atomic.AtomicReference
import com.ckkloverdos.maybe.{Maybe, Just}

/**
 * Searches for and loads the applicable accounting policy
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
object Policy extends DSL with Loggable {

  /* Pointer to the latest policy */
  private lazy val policies = new AtomicReference[Map[Timeslot, DSLPolicy]](reloadPolicies)

  /* Pointer to the latest policy */
  private val currentPolicy = new AtomicReference[DSLPolicy](policies.get.last._2)

  /**
   * Get the latest defined policy.
   */
  def policy = currentPolicy.get

  /**
   * Get the policy that is valid at the specified time instance.
   */
  def policy(at: Date): Maybe[DSLPolicy] = Maybe {
    policies.get.find {
      a => (a._1.from.before(at) && a._1.to.after(at)) ||
           (a._1.from.before(at) && a._1.to == -1)
    } match {
      case Some(x) => x._2
      case None =>
        throw new AccountingException("No valid policy for date: %s".format(at))
    }
  }

  /**
   * Get the policies that are valid between the specified time instances
   */
  def policies(from: Date, to: Date): List[DSLPolicy] = {
    policies.get.filter {
      a => a._1.from.before(from) &&
           a._1.to.after(to)
    }.valuesIterator.toList
  }

  /**
   * Get the policies that are valid throughout the specified
   * [[gr.grnet.aquarium.logic.accounting.dsl.Timeslot]]
   */
  def policies(t: Timeslot): List[DSLPolicy] = policies(t.from, t.to)

  /**
   * Load and parse a policy from file.
   */
  def loadPolicyFromFile(pol: File): DSLPolicy = {

    val stream = pol.exists() match {
      case true =>
        logger.info("Using policy file %s".format(pol.getAbsolutePath))
        new FileInputStream(pol)
      case false =>
        logger.warn(("Cannot find user configured policy file %s, " +
          "looking for default policy").format(pol.getAbsolutePath))
        getClass.getClassLoader.getResourceAsStream("policy.yaml") match {
          case x: InputStream =>
            logger.warn("Using default policy, this is problably not what you want")
            x
          case null =>
            logger.error("No valid policy file found, Aquarium will fail")
            null
        }
    }
    parse(stream)
  }

  /**
   * Trigger a policy update cycle.
   */
  def updatePolicies = synchronized {
    //XXX: The following update should happen as one transaction
    val tmpPol = reloadPolicies
    currentPolicy.set(tmpPol.last._2)
    policies.set(tmpPol)
  }

  /**
   * Search for and open a stream to a policy.
   */
   private def policyFile = {
    val policyConfResourceM = BasicResourceContext.getResource(PolicyConfName)
    policyConfResourceM match {
      case Just(policyResource) ⇒
        val path = policyResource.url.getPath
        new File(path) // assume it is resolved in the filesystem as it should (!)
      case _ ⇒
        new File("/etc/aquarium/policy.yaml") // FIXME remove this since it should have been picked up by the context
    }
  }

  /**
   * Check whether the policy definition file (in whichever path) is
   * newer than the latest stored policy, reload and set it as current.
   * This method has side-effects to this object's state.
   */
  private def reloadPolicies: Map[Timeslot, DSLPolicy] = {
    //1. Load policies from db
    val policies = MasterConfigurator.policyStore.loadPolicies(0)

    //2. Check whether policy file has been updated
    val latestPolicyChange = if (policies.isEmpty) 0 else policies.last.validFrom
    val policyf = policyFile
    var updated = false

    if (policyf.exists) {
      if (policyf.lastModified > latestPolicyChange) {
        logger.info("Policy changed since last check, reloading")
        updated = true
      } else {
        logger.info("Policy not changed since last check")
      }
    } else {
      logger.warn("User specified policy file %s does not exist, " +
        "using stored policy information".format(policyf.getAbsolutePath))
    }

    val toAdd = updated match {
      case true =>
        val ts = TimeHelpers.nowMillis
        val parsedNew = loadPolicyFromFile(policyf)
        val newPolicy = parsedNew.toPolicyEntry.copy(occurredMillis = ts,
          receivedMillis = ts, validFrom = ts)

        if(!policies.isEmpty) {
          val toUpdate = policies.last.copy(validTo = ts)
          MasterConfigurator.policyStore.updatePolicy(toUpdate)
          MasterConfigurator.policyStore.storePolicy(newPolicy)
          List(toUpdate, newPolicy)
        } else {
          MasterConfigurator.policyStore.storePolicy(newPolicy)
          List(newPolicy)
        }

      case false => List()
    }

    // FIXME: policies may be empty
    policies.init.++(toAdd).foldLeft(Map[Timeslot, DSLPolicy]()){
      (acc, p) =>
        acc ++ Map(Timeslot(new Date(p.validFrom), new Date(p.validTo)) -> parse(p.policyYAML))
    }
  }
}