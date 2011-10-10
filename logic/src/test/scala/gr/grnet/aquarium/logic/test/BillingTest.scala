package gr.grnet.aquarium.logic.test

import org.junit._
import gr.grnet.aquarium.logic.Bills
import gr.grnet.aquarium.model.{User, DB}
import gr.grnet.aquarium.logic.accounting.agreements.DefaultAgreement
import gr.grnet.aquarium.logic.accounting.policies.DefaultRatePolicy
import gr.grnet.aquarium.logic.accounting.{AccountingEvent, AccountingEntryType, AccountingEventType}
import java.util.Date

class BillingTest
  extends FixtureLoader with Bills {

  @Before
  def before() = {
    if (!DB.getTransaction.isActive)
      DB.getTransaction.begin
    //loadFixture("data.json")
  }

  @Test
  def testAccountingRules() = {
    DefaultAgreement.addPolicy(AccountingEventType.VMTime, new DefaultRatePolicy(AccountingEntryType.VMTIME_CHARGE), new Date(2))
    DefaultAgreement.addPolicy(AccountingEventType.DiskSpace, new DefaultRatePolicy(AccountingEntryType.STORAGE_CHARGE), new Date(2))
    DefaultAgreement.addPolicy(AccountingEventType.NetDataDown, new DefaultRatePolicy(AccountingEntryType.NET_CHARGE), new Date(2))
    DefaultAgreement.addPolicy(AccountingEventType.NetDataUp, new DefaultRatePolicy(AccountingEntryType.NET_CHARGE), new Date(2))

    val u = new User
    u.agreement = DefaultAgreement.id
    u.credits = 100

    DB.persistAndFlush(u)

    val evt = new AccountingEvent(AccountingEventType.VMTime, new Date(4), u.id, 15, List())
    evt.process()
  }

  @After
  def after() = {
    DB.getTransaction.rollback
  }
}