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
import com.ckkloverdos.maybe.Failed

/**
 * A logger that keeps track of working context and indentation level.
 *
 * This is mostly useful in single-threaded debugging sessions.
  *
  * A sample output follows:
  *
  * {{{
DEBUG 17:50:19 g.g.a.user.UserStateComputationsTest -                          testOrphanOFF() BEGIN
DEBUG 17:50:19 g.g.a.user.UserStateComputationsTest -                          testOrphanOFF()
DEBUG 17:50:19 g.g.a.user.UserStateComputationsTest -                          testOrphanOFF()   +++ [Events by OccurredMillis] +++
DEBUG 17:50:19 g.g.a.user.UserStateComputationsTest -                          testOrphanOFF()       EVENT(0, [2012-01-01 00:00:00], 0.0, vmtime::VM.1, Map(), CKKL, synnefo)
DEBUG 17:50:19 g.g.a.user.UserStateComputationsTest -                          testOrphanOFF()   --- [Events by OccurredMillis] ---
DEBUG 17:50:19 g.g.a.user.UserStateComputationsTest -                          testOrphanOFF()
DEBUG 17:50:19 g.g.a.user.UserStateComputationsTest -            doFullMonthlyBilling(2012-01)   BEGIN
DEBUG 17:50:19 g.g.a.user.UserStateComputationsTest - …ndUserStateAtEndOfBillingMonth(2011-12)     BEGIN
DEBUG 17:50:19 g.g.a.user.UserStateComputationsTest - …ndUserStateAtEndOfBillingMonth(2011-12)       No user state found from cache, will have to (re)compute
DEBUG 17:50:19 g.g.a.user.UserStateComputationsTest -            doFullMonthlyBilling(2011-12)       BEGIN
DEBUG 17:50:19 g.g.a.user.UserStateComputationsTest - …ndUserStateAtEndOfBillingMonth(2011-11)         BEGIN
DEBUG 17:50:19 g.g.a.user.UserStateComputationsTest - …ndUserStateAtEndOfBillingMonth(2011-11)           No user state found from cache, will have to (re)compute
DEBUG 17:50:19 g.g.a.user.UserStateComputationsTest -            doFullMonthlyBilling(2011-11)           BEGIN
DEBUG 17:50:19 g.g.a.user.UserStateComputationsTest - …ndUserStateAtEndOfBillingMonth(2011-10)             BEGIN
DEBUG 17:50:19 g.g.a.user.UserStateComputationsTest - …ndUserStateAtEndOfBillingMonth(2011-10)               User did not exist before 2011-11-01 00:00:00.000
DEBUG 17:50:19 g.g.a.user.UserStateComputationsTest - …ndUserStateAtEndOfBillingMonth(2011-10)               Returning INITIAL state [_id=4fa7e12ba0eee3db73fbe8d0] UserState(true,CKKL,1320098400000,0,false,null,ImplicitlyIssuedResourceEventsSnapshot(List()),List(),List(),LatestResourceEventsSnapshot(List()),0,0,IMStateSnapshot(StdIMEvent(,1320098400000,1320098400000,CKKL,,true,user,1.0,create,Map())),CreditSnapshot(0.0),AgreementsSnapshot(List(AgreementSnapshot(default, 2011-11-01 00:00:00.000, 292278994-08-17 07:12:55.807))),OwnedResourcesSnapshot(List()),List(),1320098400000,InitialUserStateSetup,0,None,4fa7e12ba0eee3db73fbe8d0)
DEBUG 17:50:19 g.g.a.user.UserStateComputationsTest - …ndUserStateAtEndOfBillingMonth(2011-10)             END
DEBUG 17:50:19 g.g.a.user.UserStateComputationsTest -            doFullMonthlyBilling(2011-11)             calculationReason = MonthlyBillingCalculation(2011-11)
DEBUG 17:50:19 g.g.a.user.UserStateComputationsTest -            doFullMonthlyBilling(2011-11)             Saved [_id=4fa7e12ba0eee3db73fbe8d0] UserState(true,CKKL,1320098400000,1,false,null,ImplicitlyIssuedResourceEventsSnapshot(List()),List(),List(),LatestResourceEventsSnapshot(List()),0,0,IMStateSnapshot(StdIMEvent(,1320098400000,1320098400000,CKKL,,true,user,1.0,create,Map())),CreditSnapshot(0.0),AgreementsSnapshot(List(AgreementSnapshot(default, 2011-11-01 00:00:00.000, 292278994-08-17 07:12:55.807))),OwnedResourcesSnapshot(List()),List(),1320098400000,MonthlyBillingCalculation(2011-11),0,Some(4fa7e12ba0eee3db73fbe8d0),4fa7e12ba0eee3db73fbe8d0)
DEBUG 17:50:19 g.g.a.user.UserStateComputationsTest -            doFullMonthlyBilling(2011-11)             RETURN UserState(true,CKKL,1320098400000,1,false,null,ImplicitlyIssuedResourceEventsSnapshot(List()),List(),List(),LatestResourceEventsSnapshot(List()),0,0,IMStateSnapshot(StdIMEvent(,1320098400000,1320098400000,CKKL,,true,user,1.0,create,Map())),CreditSnapshot(0.0),AgreementsSnapshot(List(AgreementSnapshot(default, 2011-11-01 00:00:00.000, 292278994-08-17 07:12:55.807))),OwnedResourcesSnapshot(List()),List(),1320098400000,MonthlyBillingCalculation(2011-11),0,Some(4fa7e12ba0eee3db73fbe8d0),4fa7e12ba0eee3db73fbe8d0)
DEBUG 17:50:19 g.g.a.user.UserStateComputationsTest -            doFullMonthlyBilling(2011-11)           END
DEBUG 17:50:19 g.g.a.user.UserStateComputationsTest - …ndUserStateAtEndOfBillingMonth(2011-11)         END
DEBUG 17:50:19 g.g.a.user.UserStateComputationsTest -            doFullMonthlyBilling(2011-12)         calculationReason = MonthlyBillingCalculation(2011-12)
DEBUG 17:50:19 g.g.a.user.UserStateComputationsTest -            doFullMonthlyBilling(2011-12)         Saved [_id=4fa7e12ba0eee3db73fbe8d0] UserState(true,CKKL,1320098400000,2,false,null,ImplicitlyIssuedResourceEventsSnapshot(List()),List(),List(),LatestResourceEventsSnapshot(List()),0,0,IMStateSnapshot(StdIMEvent(,1320098400000,1320098400000,CKKL,,true,user,1.0,create,Map())),CreditSnapshot(0.0),AgreementsSnapshot(List(AgreementSnapshot(default, 2011-11-01 00:00:00.000, 292278994-08-17 07:12:55.807))),OwnedResourcesSnapshot(List()),List(),1320098400000,MonthlyBillingCalculation(2011-12),0,Some(4fa7e12ba0eee3db73fbe8d0),4fa7e12ba0eee3db73fbe8d0)
DEBUG 17:50:19 g.g.a.user.UserStateComputationsTest -            doFullMonthlyBilling(2011-12)         RETURN UserState(true,CKKL,1320098400000,2,false,null,ImplicitlyIssuedResourceEventsSnapshot(List()),List(),List(),LatestResourceEventsSnapshot(List()),0,0,IMStateSnapshot(StdIMEvent(,1320098400000,1320098400000,CKKL,,true,user,1.0,create,Map())),CreditSnapshot(0.0),AgreementsSnapshot(List(AgreementSnapshot(default, 2011-11-01 00:00:00.000, 292278994-08-17 07:12:55.807))),OwnedResourcesSnapshot(List()),List(),1320098400000,MonthlyBillingCalculation(2011-12),0,Some(4fa7e12ba0eee3db73fbe8d0),4fa7e12ba0eee3db73fbe8d0)
DEBUG 17:50:19 g.g.a.user.UserStateComputationsTest -            doFullMonthlyBilling(2011-12)       END
DEBUG 17:50:19 g.g.a.user.UserStateComputationsTest - …ndUserStateAtEndOfBillingMonth(2011-12)     END
DEBUG 17:50:19 g.g.a.user.UserStateComputationsTest -         walletEntriesForResourceEvent(0)     +++ [EVENT(0, [2012-01-01 00:00:00], 0.0, vmtime::VM.1, Map(), CKKL, synnefo)] +++
DEBUG 17:50:19 g.g.a.user.UserStateComputationsTest -         walletEntriesForResourceEvent(0)       Cost policy OnOffCostPolicy for DSLResource(vmtime,Hr,OnOffCostPolicy,true,instanceId)
DEBUG 17:50:19 g.g.a.user.UserStateComputationsTest -         walletEntriesForResourceEvent(0)       PreviousM None
DEBUG 17:50:19 g.g.a.user.UserStateComputationsTest -         walletEntriesForResourceEvent(0)       Ignoring first event of its kind EVENT(0, [2012-01-01 00:00:00], 0.0, vmtime::VM.1, Map(), CKKL, synnefo)
DEBUG 17:50:19 g.g.a.user.UserStateComputationsTest -         walletEntriesForResourceEvent(0)     --- [EVENT(0, [2012-01-01 00:00:00], 0.0, vmtime::VM.1, Map(), CKKL, synnefo)] ---
DEBUG 17:50:19 g.g.a.user.UserStateComputationsTest -            doFullMonthlyBilling(2012-01)     calculationReason = MonthlyBillingCalculation(2012-01)
DEBUG 17:50:19 g.g.a.user.UserStateComputationsTest -            doFullMonthlyBilling(2012-01)     Saved [_id=4fa7e12ba0eee3db73fbe8d0] UserState(true,CKKL,1320098400000,3,false,null,ImplicitlyIssuedResourceEventsSnapshot(List()),List(),List(),LatestResourceEventsSnapshot(List(StdResourceEvent(0,1325368800000,1325368800000,CKKL,synnefo,vmtime,VM.1,0.0,1.0,Map()))),0,0,IMStateSnapshot(StdIMEvent(,1320098400000,1320098400000,CKKL,,true,user,1.0,create,Map())),CreditSnapshot(0.0),AgreementsSnapshot(List(AgreementSnapshot(default, 2011-11-01 00:00:00.000, 292278994-08-17 07:12:55.807))),OwnedResourcesSnapshot(List()),List(),1320098400000,MonthlyBillingCalculation(2012-01),0,Some(4fa7e12ba0eee3db73fbe8d0),4fa7e12ba0eee3db73fbe8d0)
DEBUG 17:50:19 g.g.a.user.UserStateComputationsTest -            doFullMonthlyBilling(2012-01)     RETURN UserState(true,CKKL,1320098400000,3,false,null,ImplicitlyIssuedResourceEventsSnapshot(List()),List(),List(),LatestResourceEventsSnapshot(List(StdResourceEvent(0,1325368800000,1325368800000,CKKL,synnefo,vmtime,VM.1,0.0,1.0,Map()))),0,0,IMStateSnapshot(StdIMEvent(,1320098400000,1320098400000,CKKL,,true,user,1.0,create,Map())),CreditSnapshot(0.0),AgreementsSnapshot(List(AgreementSnapshot(default, 2011-11-01 00:00:00.000, 292278994-08-17 07:12:55.807))),OwnedResourcesSnapshot(List()),List(),1320098400000,MonthlyBillingCalculation(2012-01),0,Some(4fa7e12ba0eee3db73fbe8d0),4fa7e12ba0eee3db73fbe8d0)
DEBUG 17:50:19 g.g.a.user.UserStateComputationsTest -            doFullMonthlyBilling(2012-01)   END
DEBUG 17:50:19 g.g.a.user.UserStateComputationsTest -                          testOrphanOFF()   _id = 4fa7e12ba0eee3db73fbe8d0
DEBUG 17:50:19 g.g.a.user.UserStateComputationsTest -                          testOrphanOFF()   parentId = Some(4fa7e12ba0eee3db73fbe8d0)
DEBUG 17:50:19 g.g.a.user.UserStateComputationsTest -                          testOrphanOFF()   credits = 0.0
DEBUG 17:50:19 g.g.a.user.UserStateComputationsTest -                          testOrphanOFF()   changeReason = MonthlyBillingCalculation(2012-01)
DEBUG 17:50:19 g.g.a.user.UserStateComputationsTest -                          testOrphanOFF()   implicitlyIssued [#=0] = List()
DEBUG 17:50:19 g.g.a.user.UserStateComputationsTest -                          testOrphanOFF()   latestResourceEvents [#=1]:
DEBUG 17:50:19 g.g.a.user.UserStateComputationsTest -                          testOrphanOFF()     EVENT(0, [2012-01-01 00:00:00], 0.0, vmtime::VM.1, Map(), CKKL, synnefo)
DEBUG 17:50:20 g.g.a.user.UserStateComputationsTest -                          testOrphanOFF()   newWalletEntries [#=0] = List()
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
  
  def fromOther(clogOpt: Option[ContextualLogger], logger: Logger,  fmt: String, args: Any*): ContextualLogger = {
    clogOpt match {
      case Some(clog) ⇒
        new ContextualLogger(clog.logger, fmt, args:_*).indentAs(clog)

      case None ⇒
        new ContextualLogger(logger, fmt, args:_*)
    }
  }
}
