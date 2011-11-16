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

package gr.grnet.aquarium.logic.accounting.dsl

import java.util.Date
import scala.collection._

/**
 * A semantic checker for the Aquarium accounting DSL. 
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
trait DSLSemanticChecks {

  /**
   * Functions to apply by default when checking consistency for
   * [[DSLPolicy]] resources.
   */
  val policyChecks = List[DSLPolicy => List[DSLConsistencyMsg]](
  )

  /**
   * Functions to apply by default when checking consistency for
   * [[DSLPriceList]] resources.
   */
  val priceListChecks = List[DSLPriceList => List[DSLConsistencyMsg]](
  )

  /**
   * Functions to apply by default when checking consistency for
   * [[DSLAgreement]] resources.
   */
  val agreementChecks = List[DSLAgreement => List[DSLConsistencyMsg]](
  )

  /**
   * Functions to apply by default when checking consistency for
   * [[DSLTimeFrame]] resources.
   */
  val timeFrameChecks = List[DSLTimeFrame => List[DSLConsistencyMsg]](
    checkTimeFrameFromTo,
    checkRepeatHoles
  )

  /**
   * Apply a list of consistency checking functions to a DSL entity and
   * report results.
   */
  def check[A](resource: A, checks: List[A => List[DSLConsistencyMsg]]) :
    List[DSLConsistencyMsg] = {
    checks.map(f => f(resource)).flatten.toList
  }

  /**
   * Top level consistency check functions. Applies all tests on all resources.
   */
  def check(creditPolicy: DSLCreditPolicy) : List[DSLConsistencyMsg] = {
    List[DSLConsistencyMsg]() ++
      creditPolicy.pricelists.flatMap(p => check(p)) ++
      creditPolicy.policies.flatMap(p => check(p)) ++
      creditPolicy.agreements.flatMap(a => check(a))
  }

  /** Apply [[DSLPriceList]]-related checks on a pricelist */
  def check(pl: DSLPriceList): List[DSLConsistencyMsg] = check(pl, priceListChecks)

  /** Apply [[DSLPolicy]]-related checks on a policy */
  def check(pl: DSLPolicy): List[DSLConsistencyMsg] = check(pl, policyChecks)

  /** Apply [[DSLAgreement]]-related checks on a policy */
  def check(pl: DSLAgreement): List[DSLConsistencyMsg] = check(pl, agreementChecks)

  /** Apply [[DSLTimeframe]]-related checks on a timeframe */
  def check(time: DSLTimeFrame): List[DSLConsistencyMsg] = check(time, timeFrameChecks)

  /* -- Checker functions -- */
  private def checkTimeFrameFromTo(time: DSLTimeFrame) : List[DSLConsistencyMsg] = {
    if (time.from.after(time.to.getOrElse(new Date(0))))
      List(DSLConsistencyError("foo"))
    else
      List()
  }

}

sealed trait DSLConsistencyMsg
case class DSLConsistencyWarn(warn: String) extends DSLConsistencyMsg
case class DSLConsistencyError(err: String) extends DSLConsistencyMsg
