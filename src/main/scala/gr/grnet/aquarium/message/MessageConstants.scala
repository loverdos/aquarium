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

package gr.grnet.aquarium.message

/**
 * Provides constants for known values expected in message objects.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
object MessageConstants {
  final val EventVersion_1_0 = "1.0"
  final val DefaultSelectorKey = "default"

  final object DetailsKeys {
    final val path = "path"

    final val action = "action"

    final val versions = "versions"

    // This is set in the details map to indicate a synthetic resource event (ie not a real one).
    // Examples of synthetic resource events are those that are implicitly generated at the
    // end of the billing period (e.g. `OFF`s).
    final val aquarium_is_synthetic    = "__aquarium_is_synthetic__"

    final val aquarium_is_implicit_end = "__aquarium_is_implicit_end__"

    final val aquarium_is_dummy_first = "__aquarium_is_dummy_first__"

    final val aquarium_is_realtime_virtual = "__aquarium_is_realtime_virtual__"

    final val aquarium_reference_event_id = "__aquarium_reference_event_id__"

    final val aquarium_reference_event_id_in_store = "__aquarium_reference_event_id_in_store__"
  }

  final object IMEventMsg {
    final object EventTypes {
      final val create = "create"
      final val modify = "modify"
    }
  }
}
