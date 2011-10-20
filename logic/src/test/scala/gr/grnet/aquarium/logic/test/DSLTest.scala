package gr.grnet.aquarium.logic.test

import org.junit.Test
import gr.grnet.aquarium.logic.accounting.dsl.DSL

class DSLTest {

  @Test
  def testDSLLoad = {
    DSL.parse(getClass.getClassLoader.getResourceAsStream("policy.yaml"))
  }
}