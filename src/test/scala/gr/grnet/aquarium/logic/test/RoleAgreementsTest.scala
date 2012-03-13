package gr.grnet.aquarium.logic.test

import org.junit.Assert._
import org.junit.{Test}
import io.Source
import gr.grnet.aquarium.util.TestMethods
import gr.grnet.aquarium.logic.accounting.{Policy, RoleAgreements}


/**
 * Tests for the [[gr.grnet.aquarium.logic.accounting.RoleAgreements]] class
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
class RoleAgreementsTest extends TestMethods {

  @Test
  def testParseMappings {

    var a = """

    # Some useless comment
student=foobar # This should be ignored (no default policy)
prof=default
    name=default
%asd=default  # This should be ignored (non accepted char)
*=default
      """

    var src = Source.fromBytes(a.getBytes())
    var output = RoleAgreements.parseMappings(src)

    assertEquals(3, output.size)
    assertEquals("default", output.getOrElse("prof",null).name)

    // No default value
    a = """
    prof=default
    """
    src = Source.fromBytes(a.getBytes())
    assertThrows[RuntimeException](RoleAgreements.parseMappings(src))
  }

  @Test
  def testLoadMappings {
    // Uses the roles-agreements.map file in test/resources
    RoleAgreements.loadMappings

    assertEquals("default", RoleAgreements.agreementForRole("student").name)

    // Check that default policies are applied
    assertEquals("default", RoleAgreements.agreementForRole("foobar").name)
  }
}
