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

package gr.grnet.aquarium.event

//trait CreditsModel {
//  /**
//   * The type of credits
//   */
//  type Type
//
//  def fromString(s: CharSequence): Type
//
//  def toString(credits: Type): CharSequence
//
//  def fromDouble(d: Double): Type
//
//  def toDouble(credits: Type): Double
//}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
object CreditsModel {
  type Type = Double // A nice value type would be great here.
  type TypeInMessage = Double

  @inline final def from(s: String): Type = s.toString.toDouble

  @inline final def from(d: TypeInMessage): Type = d

  @inline final def from(l: Long): Type = l.toDouble

  @inline final def toString(t: Type): String = t.toString

  @inline final def toTypeInMessage(t: Type): TypeInMessage = t

  @inline final def mul(a: Type, b: Type): Type = a * b

  @inline final def *(a: Type, b: Type): Type = a * b

  @inline final def div(a: Type, b: Type): Type = a / b

  @inline final def /(a: Type, b: Type): Type = a / b

  @inline final def add(a: Type, b: Type): Type = a + b

  @inline final def +(a: Type, b: Type): Type = a + b

  @inline final def sub(a: Type, b: Type): Type = a - b

  @inline final def -(a: Type, b: Type): Type = a - b

  @inline final def neg(a: Type): Type = -a

  @inline final def inv(a: Type): Type = 1 / a
}
