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

package gr.grnet.aquarium

/**
 * Utility definitions.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
package object util {
  def tryOption[A](f: => A): Option[A] = {
    try Some(f)
    catch {
      case _: Exception => None
    }
  }

  /**
   * Compute the class name excluding any leading packages.
   *
   * This is basically the name after the last dot.
   */
  def shortNameOfClass(theClass: Class[_]): String = {
    val cname = theClass.getName
    cname.substring(cname.lastIndexOf(".") + 1)
  }

  /**
   * For the class of the provided object, compute the class name excluding any leading packages.
   *
   * This is basically the name after the last dot.
   *
   * The `null` value is mapped to string `"null"`.
   */
  def shortClassNameOf(anyRef: AnyRef): String = {
    anyRef match {
      case null =>
        "<null>"
      case clz: Class[_] =>
        shortNameOfClass(clz)
      case obj =>
        shortNameOfClass(obj.getClass)
    }
  }

  def safeToStringOrNull(obj: AnyRef): String = obj match {
    case null => null
    case _ => obj.toString
  }

  def displayableObjectInfo(obj: AnyRef): String = {
    "[%s] %s".format(obj.getClass, obj)
  }
}