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

package gr.grnet.aquarium.charging

import gr.grnet.aquarium.AquariumException

/**
 * Encapsulates the possible states that a resource with an
 * [[gr.grnet.aquarium.charging.OnOffChargingBehavior]]
 * can be.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
sealed abstract class OnOffPolicyResourceState(val state: String) {
  def isOn: Boolean = !isOff
  def isOff: Boolean = !isOn
}

object OnResourceState extends OnOffPolicyResourceState(OnOffPolicyResourceStateNames.on) {
  override def isOn = true
}

object OffResourceState extends OnOffPolicyResourceState(OnOffPolicyResourceStateNames.off) {
  override def isOff = true
}

object OnOffPolicyResourceState {
  def apply(name: Any): OnOffPolicyResourceState = {
    name match {
      case x: String if (x.equalsIgnoreCase(OnOffPolicyResourceStateNames.on))  ⇒ OnResourceState
      case y: String if (y.equalsIgnoreCase(OnOffPolicyResourceStateNames.off)) ⇒ OffResourceState
      case a: Double if (a == 0) => OffResourceState
      case b: Double if (b == 1) => OnResourceState
      case i: Int if (i == 0) => OffResourceState
      case j: Int if (j == 1) => OnResourceState
      case _ => throw new AquariumException("Invalid OnOffPolicyResourceState %s".format(name))
    }
  }
}

