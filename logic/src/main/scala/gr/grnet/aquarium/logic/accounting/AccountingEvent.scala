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

import gr.grnet.aquarium.model.{Entity, DB}
import java.util.Date


/** An accounting event represents any event that the accounting system
 *  must process.
 */
class AccountingEvent(et: AccountingEventType.Value, start: Date,
                      end: Date, who: Long, amount: Float, rel: List[Long]) {

  def dateStart() = start
  def dateEnd() = end

  def entity() = DB.find(classOf[Entity], who) match {
    case Some(x) => x
    case None => throw new Exception("No user with id: " + who)
  }

  def value() = amount

  def relatedEvents() = rel

  def kind() = et

  def process() = {
    val evts =  policy().map{p => p.process(this)}
    val total = evts.map(x => x.amount).foldLeft(0F)((a, sum) => a + sum)
    new AccountingEntry(rel, new Date(), total, evts.head.entryType)
  }

  def policy(): List[Policy] = {
    /*val agr = AgreementRegistry.getAgreement(entity().agreement) match {
      case Some(x) => x
      case None => throw new Exception("No agreement with id:" +
        entity().agreement)
    }*/

    /*agr.policy(et, when) match {
      case Some(x) => x
      case None => throw new Exception("No charging policy for event type:" +
        et + " in period starting from:" + when.getTime)
    }*/
    List()
  }

  def getRate(): Float = {
    /*val agreement = AgreementRegistry.getAgreement(entity.agreement)

    agreement match {
      case Some(x) => x.pricelist.get(et).getOrElse(0)
      case None => 0
    }*/
    0F
  }
}
