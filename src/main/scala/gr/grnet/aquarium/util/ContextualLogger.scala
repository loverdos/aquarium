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

package gr.grnet.aquarium.util

import org.slf4j.Logger
import com.ckkloverdos.maybe.{Just, Maybe}

/**
 * A logger that keeps track of working context and indentation level.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
final class ContextualLogger(val logger: Logger, fmt: String, args: Any*) {
  val ctx = ContextualLogger.fixCtx(fmt.format(args:_*))

  private[this] var _nesting = 0

  private def nestMsg(fmt: String, args: Any*) = {
    val msg = fmt.format(args: _*)
    _nesting match {
      case 0 ⇒
        msg
      case n ⇒
        val buffer = new java.lang.StringBuilder(n + msg.size)
        var _i = 0
        while(_i < n) {
          buffer.append(' ')
          _i = _i + 1
        }
        buffer.append(msg)
        buffer.toString
    }
  }

  def nesting = _nesting

  def indentAs(other: ContextualLogger): this.type = {
    _nesting = other.nesting
    this
  }

  def indent(): this.type   = {
    _nesting = _nesting + 1
    this
  }

  def unindent(): this.type = {
    _nesting = _nesting - 1
    this
  }

  @inline
  def debug(fmt: String, args: Any*): Unit = {
    if(logger.isDebugEnabled) {
      val msg = ctx + " " + nestMsg(fmt, args:_*)
      logger.debug(msg)
    }
  }

  @inline
  def warn(fmt: String, args: Any*): Unit = {
    if(logger.isWarnEnabled) {
      val msg = ctx + " " + nestMsg(fmt, args:_*)
      logger.debug(msg)
    }
  }

  @inline
  def begin(): Unit = {
    debug("BEGIN")
    indent()
  }

  @inline
  def end(): Unit = {
    unindent()
    debug("END")
  }

  @inline
  def endWith[A](f: A): A = {
    val retval = f
    end()
    retval
  }
}

object ContextualLogger {
  final val MaxCtxLength = 45
  
  def fixCtx(ctx: String): String = {
    val ctxLen = ctx.length()
    if(ctxLen == MaxCtxLength) {
      ctx
    } else if(ctxLen > MaxCtxLength) {
      ctx.substring(0, MaxCtxLength)
    } else {
      val buffer = new java.lang.StringBuilder(MaxCtxLength)
      val prefixLen = MaxCtxLength - ctxLen
      var _i = 0
      while(_i < prefixLen) {
        buffer.append(' ')
        _i = _i + 1
      }
      buffer.append(ctx)
      buffer.toString
    }
  }
  
  def fromOther(clogM: Maybe[ContextualLogger], logger: Logger,  fmt: String, args: Any*): ContextualLogger = {
    clogM match {
      case Just(clog) ⇒
        new ContextualLogger(clog.logger, fmt, args:_*).indentAs(clog)
      case _ ⇒
        new ContextualLogger(logger, fmt, args:_*)
    }
  }
}
