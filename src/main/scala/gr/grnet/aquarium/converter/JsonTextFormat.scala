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

package gr.grnet.aquarium.converter

import gr.grnet.aquarium.util.shortClassNameOf

/**
 * Represents a string in JSON format.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

sealed class JsonTextFormat(val value: String) {
  def toCompact: CompactJsonTextFormat = {
    StdConverters.StdConverters.convertEx[CompactJsonTextFormat](value)
  }

  def toPretty: PrettyJsonTextFormat = {
    StdConverters.StdConverters.convertEx[PrettyJsonTextFormat](value)
  }

  override def toString = "%s(%s)".format(shortClassNameOf(this), value)
}

object JsonTextFormat {
  def apply(value: String) = new JsonTextFormat(value)
}

final class PrettyJsonTextFormat(override val value: String) extends JsonTextFormat(value) {
  override def toPretty = this
}

object PrettyJsonTextFormat {
  def apply(value: String) = new PrettyJsonTextFormat(value)
}

final class CompactJsonTextFormat(override val value: String) extends JsonTextFormat(value) {
  override def toCompact = this
}

object CompactJsonTextFormat {
  def apply(value: String) = new CompactJsonTextFormat(value)
}