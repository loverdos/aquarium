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

import date.TimeHelpers
import org.slf4j.Logger

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

object LogHelpers {
  def logStarting(logger: Logger): Unit = {
    logger.debug("Starting ...")
  }

  def logStarting(logger: Logger, fmt: String, args: Any*): Unit = {
    logger.debug("Starting %s ...".format(fmt.format(args: _*)))
  }

  def logStarted(logger: Logger, ms0: Long, ms1: Long): Unit = {
    logger.info("Started in %.3f sec".format(TimeHelpers.secDiffOfMillis(ms0, ms1)))
  }

  def logStarted(logger: Logger, ms0: Long, ms1: Long, fmt: String, args: Any*): Unit = {
    logger.info("Started %s in %.3f sec".format(fmt.format(args: _*), TimeHelpers.secDiffOfMillis(ms0, ms1)))
  }

  def logStopping(logger: Logger): Unit = {
    logger.debug("Stopping ...")
  }

  def logStopping(logger: Logger, fmt: String, args: Any*): Unit = {
    logger.debug("Stopping %s ...".format(fmt.format(args: _*)))
  }

  def logStopped(logger: Logger, ms0: Long, ms1: Long): Unit = {
    logger.info("Stopped in %.3f sec".format(TimeHelpers.secDiffOfMillis(ms0, ms1)))
  }

  def logStopped(logger: Logger, ms0: Long, ms1: Long, fmt: String, args: Any*): Unit = {
    logger.info("Stopped %s in %.3f sec".format(fmt.format(args: _*), TimeHelpers.secDiffOfMillis(ms0, ms1)))
  }

  def logChainOfCauses(logger: Logger, t: Throwable, message: String = "Oops!"): Unit = {
    logger.error(message + "\n{}", chainOfCausesForLogging(t))
  }

  def logChainOfCausesAndException(logger: Logger, t: Throwable, message: String = "Oops!"): Unit = {
    logger.error(message + "\n{}", chainOfCausesForLogging(t))
    logger.error(message, t)
  }

  @inline
  final def Debug(logger: Logger, fmt: String, args: Any*): Unit = {
    if(logger.isDebugEnabled) {
      logger.debug(fmt.format(args:_*))
    }
  }

  @inline
  final def Info(logger: Logger, fmt: String, args: Any*): Unit = {
    if(logger.isInfoEnabled) {
      logger.info(fmt.format(args:_*))
    }
  }

  @inline
  final def Warn(logger: Logger, fmt: String, args: Any*): Unit = {
    if(logger.isWarnEnabled) {
      logger.warn(fmt.format(args:_*))
    }
  }

  @inline
  final def Error(logger: Logger, fmt: String, args: Any*): Unit = {
    if(logger.isErrorEnabled) {
      logger.error(fmt.format(args:_*))
    }
  }

  @inline
  final def Error(logger: Logger, t: Throwable, fmt: String, args: Any*): Unit = {
    if(logger.isErrorEnabled) {
      logger.error(fmt.format(args:_*), t)
    }
  }

  final def DebugMap[K, V](
      logger: Logger,
      name: String,
      map: scala.collection.Map[K, V],
      oneLineLimit: Int = 3
  ): Unit = {

    if(logger.isDebugEnabled) {
      val mapSize = map.size
      if(mapSize <= oneLineLimit) {
        Debug(logger, "%s [#=%s] = %s", name, mapSize, map)
      } else {
        logger.debug("%s [#=%s]:", name, mapSize)
        val maxKeySize = maxStringSize(map.keySet)
        for((k, v) <- map) {
          this.Debug(logger, "%s -> %s", rpad(k.toString, maxKeySize), v)
        }
      }
    }
  }

  final def DebugSeq[T](logger: Logger, name: String, seq: scala.collection.Seq[T], oneLineLimit: Int = 3): Unit = {
    if(logger.isDebugEnabled) {
      val seqSize = seq.size
      if(seqSize <= oneLineLimit) {
        Debug(logger, "%s [#=%s] = %s", name, seqSize, seq)
      } else {
        Debug(logger, "%s [#=%s]: ", name, seqSize)
        seq.foreach(Debug(logger, "%s", _))
      }
    }
  }

  final def DebugSet[T](logger: Logger, name: String, set: scala.collection.Set[T], oneLineLimit: Int = 3): Unit = {
    if(logger.isDebugEnabled) {
      val setSize = set.size
      if(setSize <= oneLineLimit) {
        Debug(logger, "%s [#=%s] = %s", name, setSize, set)
      } else {
        Debug(logger, "%s [#=%s]: ", name, setSize)
        set.foreach(Debug(logger, "%s", _))
      }
    }
  }
}
