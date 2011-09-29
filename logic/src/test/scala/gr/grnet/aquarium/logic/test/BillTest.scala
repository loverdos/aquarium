package gr.grnet.aquarium.logic.test

import org.junit._
import Assert._
import gr.grnet.aquarium.logic.Bills
import gr.grnet.aquarium.model.{Entity, DB}

class BillTest
  extends FixtureLoader with Bills {

  @Before
  def before() = {
    if (!DB.getTransaction.isActive)
      DB.getTransaction.begin
    loadFixture("data.json")
  }

  @Test
  def testCalcBill = {
    var e = DB.find[Entity](classOf[Entity], 1L)
    assert(e.get != None)
    var bill = calcBill(e.get)
    assert(bill == 0F)

    e = DB.find[Entity](classOf[Entity], 6L)
    assert(e.get != None)
    bill = calcBill(e.get)
    assert(bill != 0F)
  }

  @After
  def after() = {
    DB.getTransaction.rollback()
  }
}