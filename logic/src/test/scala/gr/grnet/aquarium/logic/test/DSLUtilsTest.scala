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

import org.junit.Test
import org.junit.Assert._
import gr.grnet.aquarium.logic.accounting.dsl.{DSLUtils, DSLTimeSpec}
import java.util.{Calendar, Date}

class DSLUtilsTest extends DSLUtils {

  @Test
  def testExpandTimeSpec = {
    val from =  new Date(1321621969000L) //Fri Nov 18 15:12:49 +0200 2011
    val to =  new Date(1324214719000L)   //Sun Dec 18 15:25:19 +0200 2011

    var a = DSLTimeSpec(33, 12, -1, -1, 3)
    var result = expandTimeSpec(a, from, to)
    assertEquals(4, result.size)

    a = DSLTimeSpec(33, 12, -1, 10, 3)   // Timespec falling outside from-to
    result = expandTimeSpec(a, from, to)
    assertEquals(0, result.size)

    // Would only return an entry if the 1rst of Dec 2011 is Thursday
    a = DSLTimeSpec(33, 12, 1, -1, 3)
    result = expandTimeSpec(a, from, to)
    assertEquals(0, result.size)

    // The 9th of Dec 2011 is Friday
    a = DSLTimeSpec(33, 12, 9, -1, 5)
    result = expandTimeSpec(a, from, to)
    assertEquals(1, result.size)

    // Every day
    a = DSLTimeSpec(33, 12, -1, -1, -1)
    result = expandTimeSpec(a, from, to)
    assertEquals(31, result.size)
  }
}