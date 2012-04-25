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

package gr.grnet.aquarium
package actor

import util.shortNameOfClass
import java.lang.reflect.InvocationTargetException
import com.ckkloverdos.maybe.{Failed, Just, MaybeEither}

/**
 * An actor who dispatches to particular methods based on the type of the received message.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
trait ReflectiveAquariumActor extends AquariumActor {
  private val messageMethodMap: Map[Class[_], java.lang.reflect.Method] = {
    val classMethodPairs = for(knownMessageClass <- knownMessageTypes) yield {
      require(knownMessageClass ne null, "Null in knownMessageTypes of %s".format(this.getClass))

      val methodName = "on%s".format(shortNameOfClass(knownMessageClass))
      // For each class MethodClass we expect a method with the following signature:
      // def onMethodClass(message: MethodClass): Unit
      MaybeEither(this.getClass.getMethod(methodName, knownMessageClass)) match {
        case Just(method) =>
          method.setAccessible(true)
          (knownMessageClass, method)

        case Failed(e) =>
          val errMsg = "Reflective actor %s does not know how to process message %s".format(this.getClass, knownMessageClass)
          logger.error(errMsg, e)
          throw new AquariumException(errMsg, e)
      }
    }

    Map(classMethodPairs.toSeq: _*)
  }

  protected def onThrowable(t: Throwable): Unit = {
    throw t
  }

  def knownMessageTypes = role.knownMessageTypes

  final protected def receive: Receive = {
    case null =>
      onThrowable(new NullPointerException("Received null message"))
    case message: AnyRef if messageMethodMap.contains(message.getClass) ⇒
      try messageMethodMap(message.getClass).invoke(this, message)
      catch {
        case e: InvocationTargetException ⇒
          onThrowable(e.getTargetException)
        case e ⇒
          onThrowable(e)
      }
  }

  def onNull: Unit = {
    throw new NullPointerException(this.toString)
  }
}