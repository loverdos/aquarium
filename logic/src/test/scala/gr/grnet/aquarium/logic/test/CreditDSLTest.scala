package gr.grnet.aquarium.logic.test
package test

import org.junit.Test
import org.junit.Assert._
import gr.grnet.aquarium.logic.credits.dsl.CreditsDSL

class CreditDSLTest {

  @Test
  def testDSLLoad = {
    val structure = CreditsDSL.parseStream(getClass.getClassLoader.getResourceAsStream("credit-structure-greek-uni.yaml"))
    assertNotNull(structure)
  }
}