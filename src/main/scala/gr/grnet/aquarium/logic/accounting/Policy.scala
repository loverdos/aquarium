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

package gr.grnet.aquarium.logic.accounting

import dsl.{Timeslot, DSLPolicy, DSL}
import gr.grnet.aquarium.Aquarium._
import java.io.{InputStream, FileInputStream, File}
import java.util.Date
import gr.grnet.aquarium.util.date.TimeHelpers
import gr.grnet.aquarium.util.Loggable
import java.util.concurrent.atomic.AtomicReference
import gr.grnet.aquarium.Aquarium
import gr.grnet.aquarium.Aquarium.Keys
import com.ckkloverdos.maybe.{Failed, NoVal, Just}
import collection.immutable.{TreeMap, SortedMap}

/**
 * Searches for and loads the applicable accounting policy
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
object Policy extends DSL with Loggable {

  /* Pointer to the latest policy */
  private[logic] lazy val policies = {
    new AtomicReference[SortedMap[Timeslot, DSLPolicy]](reloadPolicies)
  }

  /* Pointer to the latest policy */
  private lazy val currentPolicy = {new AtomicReference[DSLPolicy](latestPolicy)}

  /* Aquarium to use for loading information about the policy store */
  private var config: Aquarium = _

  /**
   * Get the latest defined policy.
   */
  def policy = currentPolicy.get

  /**
   * Get the policy that is valid at the specified time instance.
   */
  def policy(at: Date): DSLPolicy = {
    policies.get.find {
      a => (a._1.from.before(at) && a._1.to.after(at)) ||
           (a._1.from.before(at) && a._1.to == Long.MaxValue)
    } match {
      case Some(x) => x._2
      case None =>
        throw new AccountingException("No valid policy for date: %s".format(at))
    }
  }

  /**
   * Get the policies that are valid between the specified time instances,
   * in a map whose keys are sorted by time.
   */
  def policies(from: Date, to: Date): SortedMap[Timeslot, DSLPolicy] = {
    policies.get.filter {
      a => a._1.from.before(from) &&
           a._1.to.after(to)
    }
  }

  /**
   * Get the policies that are valid throughout the specified
   * [[gr.grnet.aquarium.logic.accounting.dsl.Timeslot]]
   */
  def policies(t: Timeslot): SortedMap[Timeslot, DSLPolicy] = policies(t.from, t.to)

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
    currentPolicy.set(latestPolicy)
    policies.set(tmpPol)
  }

  /**
   * Find the latest policy in the list of policies.
   */
  private def latestPolicy =
    policies.get.foldLeft((Timeslot(new Date(1), new Date(2)) -> DSLPolicy.emptyPolicy)) {
      (acc, p) =>
        if (acc._2 == DSLPolicy.emptyPolicy)
          p
        else if (p._1.after(acc._1))
          p
        else
          acc
    }._2

  /**
   * Set the configurator to use for loading policy stores. Should only
   * used for unit testing.
   */
  def withConfigurator(config: Aquarium): Unit =
    this.config = config

  /**
   * Check whether the policy definition file (in whichever path) is
   * newer than the latest stored policy, reload and set it as current.
   * This method has side-effects to this object's state.
   */
  private[logic] def reloadPolicies: SortedMap[Timeslot, DSLPolicy] =
    if (config == null)
      reloadPolicies(Instance)
    else
      reloadPolicies(config)

  private def reloadPolicies(config: Aquarium):
  SortedMap[Timeslot, DSLPolicy] = {
    //1. Load policies from db
    val pol = config.policyStore.loadPolicyEntriesAfter(0)

    //2. Check whether policy file has been updated
    val latestPolicyChange = if (pol.isEmpty) 0 else pol.last.validFrom
    val policyf = Instance.findConfigFile(PolicyConfName, Keys.aquarium_policy, PolicyConfName)
    var updated = false

    if (policyf.exists) {
      if (policyf.lastModified > latestPolicyChange) {
        logger.info("Policy file updated since last check, reloading")
        updated = true
      } else {
        logger.info("Policy file not changed since last check")
      }
    } else {
      logger.warn("User specified policy file %s does not exist, " +
        "using stored policy information".format(policyf.getAbsolutePath))
    }

    if (updated) {
      val ts = TimeHelpers.nowMillis()
      val parsedNew = loadPolicyFromFile(policyf)
      val newPolicy = parsedNew.toPolicyEntry.copy(occurredMillis = ts,
        receivedMillis = ts, validFrom = ts)

      config.policyStore.findPolicyEntry(newPolicy.id) match {
        case Just(x) =>
          logger.warn("Policy file contents not modified")
        case NoVal =>
          if (!pol.isEmpty) {
            val toUpdate = pol.last.copy(validTo = ts - 1)
            config.policyStore.updatePolicyEntry(toUpdate)
            config.policyStore.storePolicyEntry(newPolicy)
          } else {
            config.policyStore.storePolicyEntry(newPolicy)
          }
        case failed @ Failed(e) =>
          failed.throwMe
      }
    }

    config.policyStore.loadPolicyEntriesAfter(0).foldLeft(new TreeMap[Timeslot, DSLPolicy]()){
      (acc, p) =>
        acc ++ Map(Timeslot(p.validFrom, p.validTo) -> parse(p.policyYAML))
    }
  }
}