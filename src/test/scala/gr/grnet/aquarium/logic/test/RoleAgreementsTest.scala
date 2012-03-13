package gr.grnet.aquarium.logic.test

import org.junit.Assert._
import org.junit.{Test}
import io.Source
import gr.grnet.aquarium.logic.accounting.RoleAgreements


/**
 * Tests for the [[gr.grnet.aquarium.logic.accounting.RoleAgreements]] class
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
class RoleAgreementsTest {

  @Test
  def testParseMappings {

    var a = """

    # Some useless comment
student=foobar # This should be ignored (no default policy)
prof=default
%asd=default  # This should be ignored (non accepted char)
*=default

      """

    var src = Source.fromBytes(a.getBytes())
    var output = RoleAgreements.parseMappings(src)

    assertEquals(2, output.size)
    assertEquals("default", output.getOrElse("prof",null).name)
  }
}
