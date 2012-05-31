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

import org.slf4j.LoggerFactory
import gr.grnet.aquarium.util.date.TimeHelpers

/**
 * Mix this trait in your class to automatically get a Logger instance.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
trait Loggable {
  @transient
  protected val logger = LoggerFactory.getLogger(getClass)

  protected def logStartingF(fmt: String, args: Any*)(f: ⇒ Unit)(onException: ⇒ Unit = {}): Unit = {
    LogHelpers.logStarting(this.logger, fmt, args: _*)
    val ms0 = TimeHelpers.nowMillis()
    try {
      locally(f)

      val ms1 = TimeHelpers.nowMillis()
      LogHelpers.logStarted(this.logger, ms0, ms1, fmt, args: _*)
    }
    catch (StartStopErrorHandler(logger, "While starting [%s]".format(fmt.format(args)), onException))
  }

  protected def logStoppingF(fmt: String, args: Any*)(f: ⇒ Unit)(onException: ⇒ Unit = {}): Unit = {
    LogHelpers.logStopping(this.logger, fmt, args: _*)
    val ms0 = TimeHelpers.nowMillis()
    try {
      locally(f)

      val ms1 = TimeHelpers.nowMillis()
      LogHelpers.logStopped(this.logger, ms0, ms1, fmt, args: _*)
    }
    catch (StartStopErrorHandler(logger, "While stopping [%s]".format(fmt.format(args)), onException))
  }

  protected def logStarting(): Unit = {
    LogHelpers.logStarting(this.logger)
  }

  protected def logStarting(fmt: String, args: Any*): Unit = {
    LogHelpers.logStarting(this.logger, fmt, args: _*)
  }

  protected def logStarted(ms0: Long, ms1: Long): Unit = {
    LogHelpers.logStarted(this.logger, ms0, ms1)
  }

  protected def logStarted(ms0: Long, ms1: Long, fmt: String, args: Any*): Unit = {
    LogHelpers.logStarted(this.logger, ms0, ms1, fmt, args: _*)
  }

  protected def logStopping(): Unit = {
    LogHelpers.logStopping(this.logger)
  }

  protected def logStopped(ms0: Long, ms1: Long): Unit = {
    LogHelpers.logStopped(this.logger, ms0, ms1)
  }

  protected def logStopped(ms0: Long, ms1: Long, fmt: String, args: Any*): Unit = {
    LogHelpers.logStopped(this.logger, ms0, ms1, fmt, args: _*)
  }

  protected def logChainOfCauses(t: Throwable): Unit = {
    logger.error("Oops!\n{}", chainOfCausesForLogging(t))
  }

  protected def logSeparator(): Unit = {
    // With this, we should be 120 characters wide (full log line)
    logger.debug("================================================")
  }
}
