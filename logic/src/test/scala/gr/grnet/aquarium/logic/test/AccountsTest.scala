package gr.grnet.aquarium.logic.test

import gr.grnet.aquarium.model.DB
import gr.grnet.aquarium.logic.test._
import org.junit.{After, Before, Test}


class AccountsTest extends FixtureLoader {

  @Before
  def before() = {
    if (!DB.getTransaction.isActive)
      DB.getTransaction.begin
    //loadFixture("data.json")
  }

  @Test
  def AccountsTest() = {
  }

  @After
  def after() = {
    DB.getTransaction.rollback()
  }
}