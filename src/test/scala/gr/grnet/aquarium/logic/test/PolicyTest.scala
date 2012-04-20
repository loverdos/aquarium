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

import org.junit.Test
import org.junit.Assert._
import gr.grnet.aquarium.{StoreConfigurator}
import gr.grnet.aquarium.util.date.TimeHelpers
import gr.grnet.aquarium.logic.accounting.Policy
import java.io.File

/**
 * Tests for the Policy resolution algorithms
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
class PolicyTest extends DSLTestBase with StoreConfigurator {

  @Test
  def testReloadPolicies: Unit = {
    
    def copyModifyFile(from: String, to: String) = {
      val extra = "    - agreement:\n      overrides: default\n      name: foobar"
      val out = new java.io.BufferedWriter(new java.io.FileWriter(to) );
      io.Source.fromFile(from).getLines.map(x => x + "\n").foreach(s => out.write(s,0,s.length));
      out.write(extra)
      out.close()
    }

    //Initial policy file, read from class path
    Policy.withConfigurator(configurator)
    val pol = Policy.policies.get

    /*val f = Policy.policyFile
    assertTrue(f.exists)

    //Touch the file to trigger reloading with non changed state
    Thread.sleep(200)
    f.setLastModified(TimeHelpers.nowMillis)
    var polNew = Policy.reloadPolicies

    assertEquals(pol.keys.size, polNew.keys.size)
    //assertEquals(pol.keys.head, polNew.keys.head)

    //Copy the file and add a new element -> new policy
    val fileCopy = new File(f.getParent, "policy.yaml.old")
    f.renameTo(fileCopy)
    copyModifyFile(fileCopy.getAbsolutePath,
      (new File(fileCopy.getParent, "policy.yaml")).getAbsolutePath)

    polNew = Policy.reloadPolicies
    assertEquals(pol.keys.size + 1, polNew.keys.size)
    val policyEffectivities = Policy.policies.get.keySet.toList.sortWith((x,y) => if (y.from after x.from) true else false)
    testSuccessiveTimeslots(policyEffectivities)
    testNoGaps(policyEffectivities)
    */
  }

  @Test
  def testLoadStore: Unit = {
    before

    val policies = configurator.policyStore
    policies.storePolicyEntry(this.dsl.toPolicyEntry)

    val copy1 = this.dsl.copy(algorithms = List())
    policies.storePolicyEntry(copy1.toPolicyEntry)

    val copy2 = this.dsl.copy(pricelists = List())
    policies.storePolicyEntry(copy2.toPolicyEntry)

    var pol = policies.loadPolicyEntriesAfter(TimeHelpers.nowMillis())
    assert(pol.isEmpty)

    pol = policies.loadPolicyEntriesAfter(0)
    assertEquals(3, pol.size)
    assertEquals(pol.head.policyYAML, this.dsl.toYAML)
    assertEquals(pol.tail.head.policyYAML, copy1.toYAML)
    assertEquals(pol.tail.tail.head.policyYAML, copy2.toYAML)
  }
}
