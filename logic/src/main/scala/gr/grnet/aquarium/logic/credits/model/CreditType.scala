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

package gr.grnet.aquarium.logic.credits.model

/**
 * The credit type representation.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
sealed trait CreditType {
  def name: String

  def value: Int

  def isOwn = false

  def isInherited = false

  def isUnKnown = false
}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
sealed abstract class CreditTypeSkeleton(_name: String, _value: Int) extends CreditType {
  def name = _name
  def value = _value
}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
case object OwnCreditType extends CreditTypeSkeleton(CreditType.Names.Own, CreditType.Values.Own) {
  override def isOwn = true
}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
case object InheritedCreditType extends CreditTypeSkeleton(CreditType.Names.Inherited, CreditType.Values.Inherited) {
  override def isInherited = true
}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
case class UnknownCreditType(reason: Option[String]) extends CreditTypeSkeleton(CreditType.Names.Unknown, CreditType.Values.Unknown) {
  override def isUnKnown = true
}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
object CreditType {

  object Values {
    val Own = 1
    val Inherited = 2
    val Unknown = 3
  }

  object Names {
    val Own = "Own"
    val Own_Lower = Own.toLowerCase
    val Inherited = "Inherited"
    val Inherited_Lower = Inherited.toLowerCase
    val Unknown = "Unknown"
    val Unknown_Lower = Unknown.toLowerCase
  }

  def fromValue(value: Int): CreditType = {
    value match {
      case Values.Own       => OwnCreditType
      case Values.Inherited => InheritedCreditType
      case Values.Unknown   => UnknownCreditType(None)
      case value            => UnknownCreditType(Some("Bad value %s".format(value)))
    }
  }

  def fromName(name: String): CreditType = {
    name match {
      case Names.Own       => OwnCreditType
      case Names.Inherited => InheritedCreditType
      case Names.Unknown   => UnknownCreditType(None)
      case name            => UnknownCreditType(Some("Bad name %s".format(name)))
    }
  }

  def fromNameIgnoreCase(name: String): CreditType = {
    name match {
      case null => UnknownCreditType(Some("null name"))
      case _    => name.toLowerCase match {
        case Names.Own_Lower       => OwnCreditType
        case Names.Inherited_Lower => InheritedCreditType
        case Names.Unknown_Lower   => UnknownCreditType(None)
        case name                  => UnknownCreditType(Some("Bad name %s".format(name)))
      }
    }
  }

}