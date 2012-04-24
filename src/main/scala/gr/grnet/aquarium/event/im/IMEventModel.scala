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

package gr.grnet.aquarium.event.im

import gr.grnet.aquarium.event.AquariumEventModel

/**
 * The model of any event sent from the `Identity Management` (IM) external system.
 *
 * By definition, this is the model agreed upon between Aquarium and IM.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

trait IMEventModel extends AquariumEventModel {
  def clientID: String

  def isActive: Boolean

  def role: String

  def eventType: String

  def isStateActive = isActive

  def isStateSuspended = !isActive

  def isCreateUser = eventType.equalsIgnoreCase(IMEventModel.EventTypeNames.create)

  def isModifyUser = eventType.equalsIgnoreCase(IMEventModel.EventTypeNames.modify)
}

object IMEventModel {
  trait NamesT extends AquariumEventModel.NamesT {
    final val clientID = "clientID"
    final val isActive = "isActive"
    final val role = "role"
    final val eventType = "eventType"
  }

  object Names extends NamesT

  trait EventTypeNamesT {
    final val create = "create"
    final val modify = "modify"
  }

  object EventTypeNames extends EventTypeNamesT
}
