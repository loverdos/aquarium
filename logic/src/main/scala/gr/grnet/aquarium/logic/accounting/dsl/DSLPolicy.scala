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

/**
 * Root object for the Aquarium policy configuration tree.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
case class DSLPolicy(
  algorithms: List[DSLAlgorithm],
  pricelists: List[DSLPriceList],
  resources: List[DSLResource],
  creditplans: List[DSLCreditPlan],
  agreements: List[DSLAgreement]
) {

  /**Find a resource by name */
  def findResource(name: String): Option[DSLResource] = {
    resources.find(a => a.name.equals(name))
  }

  /**Find a pricelist by name */
  def findPriceList(name: String): Option[DSLPriceList] = {
    pricelists.find(a => a.name.equals(name))
  }

  /**Find an algorithm by name */
  def findAlgorithm(name: String): Option[DSLAlgorithm] = {
    algorithms.find(a => a.name.equals(name))
  }

  /**Find a crediplan by name */
  def findCreditPlan(name: String): Option[DSLCreditPlan] = {
    creditplans.find(a => a.name.equals(name))
  }

  /**Find an agreement by name */
  def findAgreement(name: String): Option[DSLAgreement] = {
    agreements.find(a => a.name.equals(name))
  }
}