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

/**
 * Like [[gr.grnet.aquarium.util.Loggable]] but the underlying logger is lazily evaluated.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

trait LazyLoggable {
  @transient
  protected lazy val logger = LoggerFactory.getLogger(getClass)

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
}
