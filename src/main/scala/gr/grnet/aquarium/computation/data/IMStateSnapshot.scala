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

package gr.grnet.aquarium.computation.data

import gr.grnet.aquarium.event.model.im.IMEventModel
import gr.grnet.aquarium.util.shortClassNameOf
import gr.grnet.aquarium.util.date.MutableDateCalc

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

case class IMStateSnapshot(
                           /**
                            * This is the latest processed IMEvent
                            */
                           latestIMEvent: IMEventModel,

                           /**
                            * The earliest activation time, if it exists.
                            */
                           userEarliestActivationMillis: Option[Long],

                           /**
                            * The user creation time, if it exists
                            */
                           userCreationMillis: Option[Long],

                           /**
                            * This is the recorded role history
                            */
                           roleHistory: RoleHistory) {

  /**
   * True iff the user has ever been activated even once.
   */
  def hasBeenActivated: Boolean = {
    userEarliestActivationMillis.isDefined
  }

  def hasBeenCreated: Boolean = {
    userCreationMillis.isDefined
  }

  /**
   * Given the newly arrived event, we compute the updated user earliest activation time, if any.
   * We always update activation time if it is earlier than the currently known activation time.
   */
  private[this] def updatedEarliestActivationTime(imEvent: IMEventModel): Option[Long] = {
    this.userEarliestActivationMillis match {
      case Some(activationMillis) if imEvent.isStateActive && activationMillis < imEvent.occurredMillis ⇒
        Some(imEvent.occurredMillis)

      case None if imEvent.isStateActive ⇒
        Some(imEvent.occurredMillis)

      case other ⇒
        other
    }
  }

  /**
   * Given the newly arrived event, we compute the updated user creation time, if any.
   * Only the first `create` event triggers an actual update.
   */
  private[this] def updatedCreationTime(imEvent: IMEventModel): Option[Long] = {
    // Allow only the first `create` event
    if(this.userCreationMillis.isDefined) {
      this.userCreationMillis
    } else if(imEvent.isCreateUser) {
      Some(imEvent.occurredMillis)
    } else {
      None
    }
  }

  /**
   * Given the newly arrived event, we compute the updated role history.
   */
  private[this] def updatedRoleHistory(imEvent: IMEventModel): RoleHistory = {
    this.roleHistory.updatedWithRole(imEvent.role, imEvent.occurredMillis)
  }

  /**
   * Computes an updated state and returns a tuple made of four elements:
   * a) the updated state, b) a `Boolean` indicating whether the user creation
   * time has changed, c) a `Boolean` indicating whether the user activation
   * time has changed and d) a `Boolean` indicating whether the user
   * role history has changed.
   *
   * The role history is updated only if the `roleCheck` is not `None` and
   * the role it represents is different than the role of the `imEvent`.
   * The motivation for `roleCheck` is to use this method in a loop (as in replaying
   * events from the [[gr.grnet.aquarium.store.IMEventStore]]).
   */
  def updatedWithEvent(imEvent: IMEventModel,
                       roleCheck: Option[String]): (IMStateSnapshot, Boolean, Boolean, Boolean) = {
    // Things of interest that may change by the imEvent:
    // - user creation time
    // - user activation time
    // - user role

    val newCreationTime = updatedCreationTime(imEvent)
    val creationTimeChanged = this.userCreationMillis != newCreationTime

    val newActivationTime = updatedEarliestActivationTime(imEvent)
    val activationTimeChanged = this.userEarliestActivationMillis != newActivationTime

    val (roleChanged, newRoleHistory) = roleCheck match {
      case Some(role) if role != imEvent.role ⇒
        (true, updatedRoleHistory(imEvent))

      case _ ⇒
        (false, this.roleHistory)
    }

    val newState = this.copy(
      latestIMEvent      = imEvent,
      userCreationMillis = newCreationTime,
      userEarliestActivationMillis = newActivationTime,
      roleHistory = newRoleHistory
    )

    (newState, creationTimeChanged, activationTimeChanged, roleChanged)
  }

  override def toString = {
    "%s(\n!! %s\n!! %s\n!! %s\n!! %s)".format(
      shortClassNameOf(this),
      latestIMEvent,
      userCreationMillis.map(new MutableDateCalc(_)),
      userEarliestActivationMillis.map(new MutableDateCalc(_)),
      roleHistory
    )
  }
}

object IMStateSnapshot {
  def initial(imEvent: IMEventModel): IMStateSnapshot = {
    IMStateSnapshot(
      imEvent,
      if(imEvent.isStateActive) Some(imEvent.occurredMillis) else None,
      if(imEvent.isCreateUser) Some(imEvent.occurredMillis) else None,
      RoleHistory.initial(imEvent.role, imEvent.occurredMillis))
  }
}
