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

import scala.collection.mutable.ListBuffer
import com.ckkloverdos.maybe.{Failed, Just, Maybe}
import java.nio.charset.Charset
import java.io.{PrintWriter, StringWriter}
import annotation.tailrec


/**
 * Utility functions.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
package object util {
  final val UTF_8_Charset = Charset.forName("UTF-8")

  def tryOption[A](f: => A): Option[A] = {
    try Some(f)
    catch {
      case _: Exception => None
    }
  }

  def withCloseable[C <: { def close(): Unit}, A](c: C)(f: C => A): A = {
    try {
      f(c)
    } finally {
      c.close()
    }
  }

  def afterLastIndexOf(separator: String, input: String) = {
    input.substring(input.lastIndexOf(separator) + 1)
  }
  /**
   * Compute the class name excluding any leading packages.
   *
   * This is basically the name after the last dot.
   */
  def shortNameOfClass(theClass: Class[_]): String = {
    afterLastIndexOf(".", theClass.getName)
  }

  /**
   * Compute the class name excluding any leading packages and any `$` prefixes.
   *
   * This is basically the name after the last dot and after any dollar sign.
   */
  def simpleNameOfClass(theClass: Class[_]): String = {
    afterLastIndexOf("$", shortNameOfClass(theClass))
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

  /**
   * Compute the class name excluding any leading packages and any `$` prefixes.
   *
   * This is basically the name after the last dot and after any dollar sign.
   */
  def simpleClassNameOf(anyRef: AnyRef) = {
    afterLastIndexOf("$", shortClassNameOf(anyRef))
  }

  def safeToStringOrNull(obj: AnyRef): String = obj match {
    case null => null
    case _ => obj.toString
  }

  def displayableObjectInfo(obj: AnyRef): String = {
    "[%s] %s".format(obj.getClass, obj)
  }

  /**
   * This basically turns an [[scala.Option]] into a [[com.ckkloverdos.maybe.Maybe]] when asking a
   * [[scala.collection.Map]] for a key.
   *
   * @param map The input map.
   * @param key The key we are interested in.
   * @tparam A The type of keys.
   * @tparam B The type of values.
   *
   * @return A [[com.ckkloverdos.maybe.Just]] if a value was found, a
   *           [[com.ckkloverdos.maybe.NoVal]] if nothing was found and a
   *           [[com.ckkloverdos.maybe.Failed]] if some error happened.
   */
  @inline
  def findFromMapAsMaybe[A, B <: AnyRef](map: scala.collection.Map[A, B], key: A): Maybe[B] = Maybe {
    map.get(key) match {
      case Some(value) ⇒
        value
      case None ⇒
        null.asInstanceOf[B]
    }
  }

  @inline
  def findAndRemoveFromMap[A, B <: AnyRef](map: scala.collection.mutable.Map[A, B], key: A): Maybe[B] = Maybe {
    map.get(key) match {
      case Some(value) ⇒
        map -= key
        value
      case None ⇒
        null.asInstanceOf[B]
    }
  }

  // Dear scalac. Optimize this.
  def nspaces(n: Int): String = {
    ("" /: (1 to n)) ((string, _) => string + " ")
  }

  def rpad(s: String, size: Int) = {
    s + nspaces((size - s.length()) max 0)
  }
  
  def maxStringSize[A](trav: Traversable[A]): Int = {
    (0 /: trav)(_ max _.toString.length)
  }

  /**
   * Given a [[com.ckkloverdos.maybe.Maybe]] that is actually a [[com.ckkloverdos.maybe.Failed]], return the latter.
   *
   * Use this only when you are sure what the `maybe` contains, since the methods can break type safety.
   *
   * @param maybe
   * @tparam A
   * @return
   */
  def failedForSure[A](maybe: Maybe[A]): Failed = {
    maybe.asInstanceOf[Failed]
  }

  /**
   * Given a [[com.ckkloverdos.maybe.Maybe]] that is actually a [[com.ckkloverdos.maybe.Just]], return the latter.
   *
   * Use this only when you are sure what the `maybe` contains, since the methods can break type safety.
   *
   * @param maybe
   * @tparam A
   * @return
   */
  def justForSure[A](maybe: Maybe[A]): Just[A] = {
    maybe.asInstanceOf[Just[A]]
  }

  /**
   * Transform an array of bytes to a string, assuming UTF-8 encoding.
   */
  def makeString(bytes: Array[Byte]): String = {
    new String(bytes, UTF_8_Charset)
  }

  /**
   * Transform a string to an array of bytes, following a UTF-8 decoding scheme.
   */
  def makeBytes(string: String): Array[Byte] = {
    string.getBytes(UTF_8_Charset)
  }

  /**
   * Return the stack trace of the given [[java.lang.Throwable]] in the form of [[java.lang.String]].
   *
   * @param t
   * @return
   */
  def stringOfStackTrace(t: Throwable): String = {
    val sw = new StringWriter()
    val pw = new PrintWriter(sw)

    t.printStackTrace(pw)
    pw.flush()
    pw.close()

    sw.toString
  }

  private[this]
  def chainOfCausesBuffer(t: Throwable): ListBuffer[String] = {
    @tailrec
    def loop(t: Throwable, buffer: ListBuffer[String]): ListBuffer[String] = {
      t match {
        case null ⇒
          buffer

        case _ ⇒
          buffer.append("%s: %s".format(shortClassNameOf(t), t.getMessage))
          loop(t.getCause, buffer)
      }
    }

    loop(t, new ListBuffer[String])
  }

  def chainOfCauses(t: Throwable): List[String] = {
    chainOfCausesBuffer(t).toList
  }

  def sourceOfTraceElement(st: StackTraceElement): Option[String] = {
    tryOption {
      val path = Class.forName(st.getClassName).getProtectionDomain.getCodeSource.getLocation.getPath
      val file = new java.io.File(path)
      file.getName
    }
  }

  def formatTraceElement(st: StackTraceElement): String = {
    val source = sourceOfTraceElement(st)
    "%s.%s(%s:%s)%s".format(
      st.getClassName,
      st.getMethodName,
      st.getFileName,
      st.getLineNumber,
      if(source.isDefined) " [%s]".format(source.get) else ""
    )
  }

  def chainOfCausesForLogging(t: Throwable, caughtTraceIndex: Int = 3) = {
    val buf = chainOfCausesBuffer(t)
    val happenedTrace = t.getStackTrace()(0)
    val caughtTrace = new Exception().getStackTrace()(caughtTraceIndex)

    buf.prepend("[Happened @] %s".format(formatTraceElement(happenedTrace)))
    buf.prepend("[Caught   @] %s".format(formatTraceElement(caughtTrace)))

    val noNL = buf.map { line ⇒
      "!! " + line.replaceAll("""[\n\r]+""", ", ")
    }

    noNL mkString "\n"
  }
}