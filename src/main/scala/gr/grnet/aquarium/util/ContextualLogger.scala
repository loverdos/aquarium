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

import org.slf4j.Logger
import com.ckkloverdos.maybe.{Failed, Just, Maybe}

/**
 * A logger that keeps track of working context and indentation level.
 *
 * This is mostly useful in single-threaded debugging sessions.
  *
  * A sample output follows:
  *
  * {{{
 [DEBUG] 2012-02-13 12:54:53,653 main -                 doFullMonthlyBilling(2012-01) BEGIN
 [DEBUG] 2012-02-13 12:54:53,653 main -     findUserStateAtEndOfBillingMonth(2011-12)   BEGIN
 [DEBUG] 2012-02-13 12:54:53,661 main -     findUserStateAtEndOfBillingMonth(2011-12)     Found 0 out of sync events, will have to (re)compute user state
 [DEBUG] 2012-02-13 12:54:53,661 main -     findUserStateAtEndOfBillingMonth(2011-12)     Computing full month billing
 [DEBUG] 2012-02-13 12:54:53,662 main -                 doFullMonthlyBilling(2011-12)     BEGIN
 [DEBUG] 2012-02-13 12:54:53,662 main -     findUserStateAtEndOfBillingMonth(2011-11)       BEGIN
 [DEBUG] 2012-02-13 12:54:53,663 main -     findUserStateAtEndOfBillingMonth(2011-11)         Found 0 out of sync events, will have to (re)compute user state
 [DEBUG] 2012-02-13 12:54:53,663 main -     findUserStateAtEndOfBillingMonth(2011-11)         Computing full month billing
 [DEBUG] 2012-02-13 12:54:53,664 main -                 doFullMonthlyBilling(2011-11)         BEGIN
 [DEBUG] 2012-02-13 12:54:53,664 main -     findUserStateAtEndOfBillingMonth(2011-10)           BEGIN
 [DEBUG] 2012-02-13 12:54:53,667 main -     findUserStateAtEndOfBillingMonth(2011-10)             User did not exist before 2011-11-01 00:00:00.000. Returning UserState(Christos,0,0,false,null,ImplicitlyIssuedResourceEventsSnapshot(Map(),0),List(),List(),LatestResourceEventsSnapshot(Map(),0),0,ActiveStateSnapshot(false,0),CreditSnapshot(0.0,0),AgreementSnapshot(List(Agreement(default,0,-1)),0),RolesSnapshot(List(),0),OwnedResourcesSnapshot(List(),0))
 [DEBUG] 2012-02-13 12:54:53,668 main -     findUserStateAtEndOfBillingMonth(2011-10)           END
 [DEBUG] 2012-02-13 12:54:53,672 main -                 doFullMonthlyBilling(2011-11)           previousResourceEvents = LatestResourceEventsWorker(Map())
 [DEBUG] 2012-02-13 12:54:53,673 main -                 doFullMonthlyBilling(2011-11)           theImplicitOFFs = ImplicitlyIssuedResourceEventsWorker(Map())
 [DEBUG] 2012-02-13 12:54:53,680 main -                 doFullMonthlyBilling(2011-11)           resourceEventStore = MemStore(Map(UserState -> 0, WalletEntry -> 0, ResourceEvent -> 5, PolicyEntry -> 0, UserEvent -> 0))
 [DEBUG] 2012-02-13 12:54:53,681 main -                 doFullMonthlyBilling(2011-11)           Found 0 resource events, starting processing...
 [DEBUG] 2012-02-13 12:54:53,683 main -                 doFullMonthlyBilling(2011-11)         END
 [DEBUG] 2012-02-13 12:54:53,683 main -     findUserStateAtEndOfBillingMonth(2011-11)       END
 [DEBUG] 2012-02-13 12:54:53,684 main -                 doFullMonthlyBilling(2011-12)       previousResourceEvents = LatestResourceEventsWorker(Map())
 [DEBUG] 2012-02-13 12:54:53,684 main -                 doFullMonthlyBilling(2011-12)       theImplicitOFFs = ImplicitlyIssuedResourceEventsWorker(Map())
 [DEBUG] 2012-02-13 12:54:53,685 main -                 doFullMonthlyBilling(2011-12)       resourceEventStore = MemStore(Map(UserState -> 0, WalletEntry -> 0, ResourceEvent -> 5, PolicyEntry -> 0, UserEvent -> 0))
 [DEBUG] 2012-02-13 12:54:53,686 main -                 doFullMonthlyBilling(2011-12)       Found 0 resource events, starting processing...
 [DEBUG] 2012-02-13 12:54:53,686 main -                 doFullMonthlyBilling(2011-12)     END
 [DEBUG] 2012-02-13 12:54:53,687 main -     findUserStateAtEndOfBillingMonth(2011-12)   END
 [DEBUG] 2012-02-13 12:54:53,687 main -                 doFullMonthlyBilling(2012-01)   previousResourceEvents = LatestResourceEventsWorker(Map())
 [DEBUG] 2012-02-13 12:54:53,688 main -                 doFullMonthlyBilling(2012-01)   theImplicitOFFs = ImplicitlyIssuedResourceEventsWorker(Map())
 [DEBUG] 2012-02-13 12:54:53,688 main -                 doFullMonthlyBilling(2012-01)   resourceEventStore = MemStore(Map(UserState -> 0, WalletEntry -> 0, ResourceEvent -> 5, PolicyEntry -> 0, UserEvent -> 0))
 [DEBUG] 2012-02-13 12:54:53,689 main -                 doFullMonthlyBilling(2012-01)   Found 4 resource events, starting processing...
 [DEBUG] 2012-02-13 12:54:53,690 main -                 doFullMonthlyBilling(2012-01)     Processing EVENT(2, [2012-01-01 03:00:00.000], 99.0 [MB/Hr], diskspace::pithos/diskspace/DISK.1, Map(), Christos, pithos)
 [DEBUG] 2012-02-13 12:54:53,691 main -                 doFullMonthlyBilling(2012-01)       0 previousResourceEvents
 [DEBUG] 2012-02-13 12:54:53,691 main -                 doFullMonthlyBilling(2012-01)       0 theImplicitOFFs
 [DEBUG] 2012-02-13 12:54:53,694 main -                 doFullMonthlyBilling(2012-01)       Processing: ResourceEvent(2,1325379600000,1325379600000,Christos,pithos,diskspace,pithos/diskspace/DISK.1,1.0,99.0,Map())
 [DEBUG] 2012-02-13 12:54:53,704 main -                 doFullMonthlyBilling(2012-01)     Processing EVENT(0, [2012-01-02 01:00:00.000], ON, vmtime::synnefo/vmtime/VM.1, Map(), Christos, synnefo)
 [DEBUG] 2012-02-13 12:54:53,705 main -                 doFullMonthlyBilling(2012-01)       1 previousResourceEvents
 [DEBUG] 2012-02-13 12:54:53,708 main -                 doFullMonthlyBilling(2012-01)         EVENT(2, [2012-01-01 03:00:00.000], 99.0 [MB/Hr], diskspace::pithos/diskspace/DISK.1, Map(), Christos, pithos)
 [DEBUG] 2012-02-13 12:54:53,709 main -                 doFullMonthlyBilling(2012-01)       0 theImplicitOFFs
 [DEBUG] 2012-02-13 12:54:53,709 main -                 doFullMonthlyBilling(2012-01)       Ignoring not billable EVENT(0, [2012-01-02 01:00:00.000], ON, vmtime::synnefo/vmtime/VM.1, Map(), Christos, synnefo)
 [DEBUG] 2012-02-13 12:54:53,711 main -                 doFullMonthlyBilling(2012-01)     Processing EVENT(3, [2012-01-02 04:00:00.000], 23.0 [MB/Hr], diskspace::pithos/diskspace/DISK.1, Map(), Christos, pithos)
 [DEBUG] 2012-02-13 12:54:53,712 main -                 doFullMonthlyBilling(2012-01)       2 previousResourceEvents
 [DEBUG] 2012-02-13 12:54:53,713 main -                 doFullMonthlyBilling(2012-01)         EVENT(0, [2012-01-02 01:00:00.000], ON, vmtime::synnefo/vmtime/VM.1, Map(), Christos, synnefo)
 [DEBUG] 2012-02-13 12:54:53,714 main -                 doFullMonthlyBilling(2012-01)         EVENT(2, [2012-01-01 03:00:00.000], 99.0 [MB/Hr], diskspace::pithos/diskspace/DISK.1, Map(), Christos, pithos)
 [DEBUG] 2012-02-13 12:54:53,714 main -                 doFullMonthlyBilling(2012-01)       0 theImplicitOFFs
 [DEBUG] 2012-02-13 12:54:53,715 main -                 doFullMonthlyBilling(2012-01)       Processing: ResourceEvent(3,1325469600000,1325469600000,Christos,pithos,diskspace,pithos/diskspace/DISK.1,1.0,23.0,Map())
 [DEBUG] 2012-02-13 12:54:53,716 main -                 doFullMonthlyBilling(2012-01)       Previous  : ResourceEvent(2,1325379600000,1325379600000,Christos,pithos,diskspace,pithos/diskspace/DISK.1,1.0,99.0,Map())
 [DEBUG] 2012-02-13 12:54:53,718 main -                 doFullMonthlyBilling(2012-01)     Processing EVENT(1, [2012-01-02 10:00:00.000], OFF, vmtime::synnefo/vmtime/VM.1, Map(), Christos, synnefo)
 [DEBUG] 2012-02-13 12:54:53,719 main -                 doFullMonthlyBilling(2012-01)       2 previousResourceEvents
 [DEBUG] 2012-02-13 12:54:53,719 main -                 doFullMonthlyBilling(2012-01)         EVENT(0, [2012-01-02 01:00:00.000], ON, vmtime::synnefo/vmtime/VM.1, Map(), Christos, synnefo)
 [DEBUG] 2012-02-13 12:54:53,721 main -                 doFullMonthlyBilling(2012-01)         EVENT(3, [2012-01-02 04:00:00.000], 23.0 [MB/Hr], diskspace::pithos/diskspace/DISK.1, Map(), Christos, pithos)
 [DEBUG] 2012-02-13 12:54:53,721 main -                 doFullMonthlyBilling(2012-01)       0 theImplicitOFFs
 [DEBUG] 2012-02-13 12:54:53,722 main -                 doFullMonthlyBilling(2012-01)       Processing: ResourceEvent(1,1325491200000,1325491200000,Christos,synnefo,vmtime,synnefo/vmtime/VM.1,1.0,0.0,Map())
 [DEBUG] 2012-02-13 12:54:53,735 main -                 doFullMonthlyBilling(2012-01)       Previous  : ResourceEvent(0,1325458800000,1325458800000,Christos,synnefo,vmtime,synnefo/vmtime/VM.1,1.0,1.0,Map())
 [DEBUG] 2012-02-13 12:54:53,736 main -                 doFullMonthlyBilling(2012-01) END
 * }}}
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
        while(_i < (n * 2)) {
          buffer.append(' ')
          _i = _i + 1
        }
        buffer.append(msg)
        buffer.toString
    }
  }

  def isDebugEnabled = logger.isDebugEnabled

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
  
  def withIndent[A](f: => A): Unit = {
    import com.ckkloverdos.maybe.effect
    this.indent()
    effect(f){}{unindent()}
  }

  def debug(fmt: String, args: Any*): Unit = {
    if(logger.isDebugEnabled) {
      val msg = ctx + " " + nestMsg(fmt, args:_*)
      logger.debug(msg)
    }
  }

  def info(fmt: String, args: Any*): Unit = {
    if(logger.isInfoEnabled) {
      val msg = ctx + " " + nestMsg(fmt, args:_*)
      logger.info(msg)
    }
  }

  def warn(fmt: String, args: Any*): Unit = {
    if(logger.isWarnEnabled) {
      val msg = ctx + " " + nestMsg(fmt, args:_*)
      logger.warn(msg)
    }
  }

  def error(fmt: String, args: Any*): Unit = {
    if(logger.isErrorEnabled) {
      val msg = ctx + " " + nestMsg(fmt, args:_*)
      logger.error(msg)
    }
  }

  def error(t: Throwable, fmt: String, args: Any*): Unit = {
    if(logger.isErrorEnabled) {
      val msg = ctx + " " + nestMsg(fmt, args:_*)
      logger.error(msg, t)
    }
  }
  
  def error(failed: Failed): Unit = {
    this.error(failed.exception, "")
  }

  def begin(message: String = ""): Unit = {
    if(message == "") debug("BEGIN") else debug("+++ [%s] +++", message)
    indent()
  }

  def end(message: String = ""): Unit = {
    unindent()
    if(message == "") debug("END") else debug("--- [%s] ---", message)
  }

  def debugMap[K, V](name: String, map: scala.collection.Map[K, V], oneLineLimit: Int = 3): Unit = {
    if(this.isDebugEnabled) {
      val mapSize = map.size
      if(mapSize <= oneLineLimit) {
        this.debug("%s [#=%s] = %s", name, mapSize, map)
      } else {
        this.debug("%s [#=%s]:", name, mapSize)
        val maxKeySize = maxStringSize(map.keySet)
        this.withIndent {
          for((k, v) <- map) {
            this.debug("%s -> %s", rpad(k.toString, maxKeySize), v)
          }
        }
      }
    }
  }

  def debugSeq[T](name: String, seq: scala.collection.Seq[T], oneLineLimit: Int = 3): Unit = {
    if(this.isDebugEnabled) {
      val seqSize = seq.size
      if(seqSize <= oneLineLimit) {
        this.debug("%s [#=%s] = %s", name, seqSize, seq)
      } else {
        this.debug("%s [#=%s]: ", name, seqSize)
        this.withIndent(seq.foreach(this.debug("%s", _)))
      }
    }
  }

  def debugSet[T](name: String, set: scala.collection.Set[T], oneLineLimit: Int = 3): Unit = {
    if(this.isDebugEnabled) {
      val setSize = set.size
      if(setSize <= oneLineLimit) {
        this.debug("%s [#=%s] = %s", name, setSize, set)
      } else {
        this.debug("%s [#=%s]: ", name, setSize)
        this.withIndent(set.foreach(this.debug("%s", _)))
      }
    }
  }
}

/**
 * Companion object of [[gr.grnet.aquarium.util.ContextualLogger]].
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
object ContextualLogger {
  final val MaxCtxLength = 40
  final val ELLIPSIS = "…" // U+2026
  
  def fixCtx(ctx: String): String = {
    val ctxLen = ctx.length()
    if(ctxLen == MaxCtxLength) {
      ctx
    } else if(ctxLen > MaxCtxLength) {
      ELLIPSIS + ctx.substring(ctxLen - MaxCtxLength + 1, ctxLen)
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
