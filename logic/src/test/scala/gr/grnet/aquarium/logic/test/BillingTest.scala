package gr.grnet.aquarium.logic.test

import org.junit._
import gr.grnet.aquarium.logic.Bills
import gr.grnet.aquarium.model.{Entity, DB}

class BillingTest
  extends FixtureLoader with Bills {

  @Before
  def before() = {
    if (!DB.getTransaction.isActive)
      DB.getTransaction.begin
    //loadFixture("data.json")
  }

  @Test
  def testCalcBill = {

  }

  @Test
  def testAccountingRules() = {

  }

  @After
  def after() = {
    DB.getTransaction.rollback()
  }
}