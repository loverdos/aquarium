package gr.grnet.aquarium.logic.test

import gr.grnet.aquarium.model.DB
import org.junit.{After, Before, Test}

class BillTest extends FixtureLoader {

  @Before
  def before() = {
    if (!DB.getTransaction.isActive)
      DB.getTransaction.begin
    loadFixture("data.json")
  }

  @Test
  def testCalcBill = {

  }

  @After
  def after() = {
    DB.getTransaction.rollback()
  }
}