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

package gr.grnet.aquarium

import com.ckkloverdos.sys.SysProp
import com.ckkloverdos.maybe.Just

import gr.grnet.aquarium.converter.StdConverters.StdConverters

/**
 * These are used to enable/disable several tests based on system properties.
 *
 * Very handy when a test is based upon an external system which sometimes we may not control.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
object LogicTestsAssumptions {
  private[this] def _checkValue(value: String, emptyStringIsTrue: Boolean) = {
    value match {
      case "" ⇒
        emptyStringIsTrue
      case value ⇒
        StdConverters.convert[Boolean](value).getOr(false)
    }
  }

  private[this] def _testPropertyTrue(name: String): Boolean = {
    // A property is true if it is given without a value (-Dtest.enable.spray) or it is given
    // with a value that corresponds to true (-Dtest.enable.spray=true)
    SysProp(name).value.map(_checkValue(_, true)).getOr(false)
  }


  private[this] def isPropertyEnabled(name: String): Boolean = {
    SysProp(name).value match {
      case Just(value) ⇒
        _checkValue(value, true)
      case _ ⇒
        _testPropertyTrue(PropertyNames.TestEnableAll)
    }
  }

  def EnableRabbitMQTests = isPropertyEnabled(PropertyNames.TestEnableRabbitMQ)
  def EnableStoreTests  = isPropertyEnabled(PropertyNames.TestEnableStore)
  def EnablePerfTests = isPropertyEnabled(PropertyNames.TestEnablePerf)
  def EnableSprayTests = isPropertyEnabled(PropertyNames.TestEnableSpray)

  def propertyValue(name: String) = SysProp(name).rawValue
}