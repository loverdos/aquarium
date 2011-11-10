/*
 * Copyright 2011 GRNET S.A. All rights reserved.
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
package test

import org.junit.Test
import org.junit.Assert._
import java.io.{Reader, InputStreamReader, InputStream, StringReader}
import gr.grnet.aquarium.util.Loggable
import gr.grnet.aquarium.util.yaml.{YAMLNode, YAMLHelpers}
import gr.grnet.aquarium.logic.credits.model.GroupCreditHolder

class CreditDSLTest extends Loggable {

  object Keys {
    val credit_group = "credit_group"
    val name = "name"
    val label = "label"
    val owner = "owner"
    val members = "members"
    val credit_distribution = "credit_distribution"
  }

  def parseResource(name: String): GroupCreditHolder = {
    parseStream(getClass.getClassLoader.getResourceAsStream(name))
  }

  def parseString(s: CharSequence): GroupCreditHolder = {
    doParse(new StringReader(s.toString))
  }

  def parseStream(in: InputStream, encoding: String = "UTF-8", closeIn: Boolean = true): GroupCreditHolder = {
    doParse(new InputStreamReader(in, encoding), closeIn)
  }

  def parseChild[T](node: YAMLNode, name: String): YAMLNode = {
    val y = node / name
    val yname = y.name
    assert(name == yname, "Parsed name [%s] equals requested name [%s]".format(yname, name))
    logger.debug("Parsed [%s] %s".format(yname, y))
    y
  }

  // FIXME: implement
  private def doParse(r: Reader, closeReader: Boolean = true): GroupCreditHolder = {
    val document = YAMLHelpers.loadYAML(r, closeReader)

    val ygroup = parseChild(document, Keys.credit_group)
    val yname = parseChild(ygroup, Keys.name)
    val yabel = parseChild(ygroup, Keys.label)
    val yowner = parseChild(ygroup, Keys.owner)
    val ymembers = parseChild(ygroup, Keys.members)
    val ydistrib = parseChild(ygroup, Keys.credit_distribution)

    GroupCreditHolder("", "", Nil, Nil)
  }
  @Test
  def testDSLLoad = {
    val structure = parseResource("credit-group-lab.yaml")
    assertNotNull(structure)
  }
}