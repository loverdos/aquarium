package gr.grnet.aquarium.logic.test

import org.junit._
import org.junit.Assert._
import gr.grnet.aquarium.logic.Bills
import gr.grnet.aquarium.model.{User, DB}
import gr.grnet.aquarium.logic.accounting.policies.DefaultRatePolicy
import gr.grnet.aquarium.logic.accounting.{Agreement, AccountingEvent, AccountingEntryType, AccountingEventType}
import gr.grnet.aquarium.logic.accounting.agreements.AgreementRegistry
import java.util.Date

class BillingTest
  extends FixtureLoader with Bills {

  @Before
  def before() = {
    if (!DB.getTransaction.isActive)
      DB.getTransaction.begin
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

    DB.persistAndFlush(u)

    // Try with a basic event
    var evt = new AccountingEvent(AccountingEventType.VMTime,
      new Date(4), new Date(10), u.id, 15, List())
    var entry = evt.process()
    assertEquals(entry.amount, 15 * 0.001F, 0.00001)

    // Try with another event type
    evt = new AccountingEvent(AccountingEventType.DiskSpace,
      new Date(4), new Date(4), u.id, 12.3F, List())
    entry = evt.process()
    assertEquals(entry.amount, 12.3F * 0.00002F, 0.00001)
  }

  @After
  def after() = {
    DB.getTransaction.rollback
  }
}