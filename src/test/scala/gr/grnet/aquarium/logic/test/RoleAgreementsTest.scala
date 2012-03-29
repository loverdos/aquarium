/*
 * Copyright 2011-2012 GRNET S.A. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 *   1. Redistributions of source code must retain the above
 *      copyright notice, this list of conditions and the following
 *      disclaimer.
 *
 *   2. Redistributions in binary form must reproduce the above
 *      copyright notice, this list of conditions and the following
 *      disclaimer in the documentation and/or other materials
 *      provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY GRNET S.A. ``AS IS'' AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL GRNET S.A OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and
 * documentation are those of the authors and should not be
 * interpreted as representing official policies, either expressed
 * or implied, of GRNET S.A.
 */

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
