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

package gr.grnet.aquarium.logic.test

import org.junit._
import org.junit.Assert._
import gr.grnet.aquarium.logic.Bills
import gr.grnet.aquarium.model.{User}
import gr.grnet.aquarium.logic.accounting.policies.DefaultRatePolicy
import gr.grnet.aquarium.logic.accounting.{Agreement, AccountingEvent, AccountingEntryType, AccountingEventType}
import gr.grnet.aquarium.logic.accounting.agreements.AgreementRegistry
import java.util.Date
import gr.grnet.aquarium.util.FixtureLoader

class BillingTest
  extends FixtureLoader with Bills {

  def getDB = TestDB

  @Before
  def before() = {
    if (!TestDB.getTransaction.isActive)
      TestDB.getTransaction.begin
    //loadFixture("data.json")
  }

  object TestAgreement extends Agreement {
    override val id = 0xdeadbabeL

    override val pricelist = Map (
      AccountingEventType.DiskSpace -> 0.00002F,
      AccountingEventType.NetDataDown -> 0.0001F,
      AccountingEventType.NetDataUp -> 0.0001F,
      AccountingEventType.VMTime -> 0.001F
    )
  }

  @Test
  def testOverallAccountingRules() = {
    TestAgreement.addPolicy(AccountingEventType.VMTime,
      new DefaultRatePolicy(AccountingEntryType.VMTIME_CHARGE), new Date(2))
    TestAgreement.addPolicy(AccountingEventType.DiskSpace,
      new DefaultRatePolicy(AccountingEntryType.STORAGE_CHARGE), new Date(2))
    TestAgreement.addPolicy(AccountingEventType.NetDataDown,
      new DefaultRatePolicy(AccountingEntryType.NET_CHARGE), new Date(2))
    TestAgreement.addPolicy(AccountingEventType.NetDataUp,
      new DefaultRatePolicy(AccountingEntryType.NET_CHARGE), new Date(2))

    AgreementRegistry.addAgreement(TestAgreement)

    val u = new User
    u.agreement = TestAgreement.id
    u.credits = 100

    TestDB.persistAndFlush(u)

    // Try with a basic event
    /*var evt = new AccountingEvent(AccountingEventType.VMTime,
      new Date(4), new Date(10), u.id, 15, List())
    var entry = evt.process()
    assertEquals(entry.amount, 15 * 0.001F, 0.00001)

    // Try with another event type
    evt = new AccountingEvent(AccountingEventType.DiskSpace,
      new Date(4), new Date(4), u.id, 12.3F, List())
    entry = evt.process()
    assertEquals(entry.amount, 12.3F * 0.00002F, 0.00001)*/
  }

  @After
  def after() = {
    TestDB.getTransaction.rollback
  }
}