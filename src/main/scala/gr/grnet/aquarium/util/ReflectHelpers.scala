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

package gr.grnet.aquarium.util

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

object ReflectHelpers {
  def setField[C <: AnyRef, A : Manifest](container: C,
                                          fieldName: String,
                                          value: A,
                                          synchronizeOnContainer: Boolean = true): Unit = {
    require(container ne null, "container is null")
    require(fieldName ne null, "fieldName is null")
    require(fieldName.length > 0, "fieldName is empty")

    val field = container.getClass.getDeclaredField(fieldName)
    field.setAccessible(true)

    def doSet(): Unit = {
      manifest[A] match {
        case Manifest.Byte ⇒ field.setBoolean(container, value.asInstanceOf[Boolean])
        case Manifest.Int  ⇒ field.setInt(container, value.asInstanceOf[Int])
        case Manifest.Long ⇒ field.setLong(container, value.asInstanceOf[Long])
        case _             ⇒ field.set(container, value)
      }
    }

    if(synchronizeOnContainer) {
      container synchronized doSet()
    } else {
      doSet()
    }
  }
}
