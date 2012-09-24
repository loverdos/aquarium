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

package gr.grnet.aquarium.message.avro

import gr.grnet.aquarium.message.avro.gen.{UserStateMsg, IMEventMsg, ResourceEventMsg, PolicyMsg}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
object OrderingHelpers {
  final val DefaultPolicyMsgOrdering = new Ordering[PolicyMsg] {
    def compare(x: PolicyMsg, y: PolicyMsg): Int = {
      val x_getValidFromMillis = x.getValidFromMillis
      val y_getValidFromMillis = y.getValidFromMillis

      if(x_getValidFromMillis < y_getValidFromMillis) {
        -1
      }
      else if (x_getValidFromMillis == y_getValidFromMillis) {
        0
      } else {
        1
      }
    }
  }

  final val DefaultResourceEventMsgOrdering = new Ordering[ResourceEventMsg] {
    def compare(x: ResourceEventMsg, y: ResourceEventMsg): Int = {
      val x_getOccurredMillis = x.getOccurredMillis
      val y_getOccurredMillis = y.getOccurredMillis

      if(x_getOccurredMillis < y_getOccurredMillis) {
        -1
      }
      else if (x_getOccurredMillis == y_getOccurredMillis) {
        0
      } else {
        1
      }
    }
  }

  final val DefaultIMEventMsgOrdering = new Ordering[IMEventMsg] {
    def compare(x: IMEventMsg, y: IMEventMsg): Int = {
      val x_getOccurredMillis = x.getOccurredMillis
      val y_getOccurredMillis = y.getOccurredMillis

      if(x_getOccurredMillis < y_getOccurredMillis) {
        -1
      }
      else if(x_getOccurredMillis == y_getOccurredMillis) {
        0
      } else {
        1
      }
    }
  }

  final val DefaultUserStateMsgOrdering = new Ordering[UserStateMsg] {
    def compare(x: UserStateMsg, y: UserStateMsg): Int = {
      if(x.getOccurredMillis < y.getOccurredMillis) {
        -1
      }
      else if(x.getOccurredMillis == y.getOccurredMillis) {
        0
      } else {
        1
      }
    }
  }
}
