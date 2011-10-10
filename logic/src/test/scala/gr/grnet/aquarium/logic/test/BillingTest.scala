package gr.grnet.aquarium.logic.test

import org.junit._
import org.junit.Assert._
import gr.grnet.aquarium.logic.Bills
import gr.grnet.aquarium.model.{User, DB}
import gr.grnet.aquarium.logic.accounting.policies.DefaultRatePolicy
import java.util.Date
import gr.grnet.aquarium.logic.accounting.{Agreement, AccountingEvent, AccountingEntryType, AccountingEventType}
import gr.grnet.aquarium.logic.accounting.agreements.AgreementRegistry

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

    val evt = new AccountingEvent(AccountingEventType.VMTime,
      new Date(4), u.id, 15, List())
    val entry = evt.process()

    assertEquals(entry.amount, 15 * 0.001F, 0.00001)
  }

  @After
  def after() = {
    DB.getTransaction.rollback
  }
}