package gr.grnet.aquarium.charging.bill

import gr.grnet.aquarium.charging.state.WorkingUserState
import gr.grnet.aquarium.util.json.JsonSupport
import com.ckkloverdos.resource.FileStreamResource
import java.io.File
import com.ckkloverdos.props.Props
import gr.grnet.aquarium.converter.{PrettyJsonTextFormat, StdConverters}
import gr.grnet.aquarium.{Aquarium, ResourceLocator, AquariumBuilder}
import gr.grnet.aquarium.store.memory.MemStoreProvider
import gr.grnet.aquarium.converter.StdConverters._
import scala.Some
import gr.grnet.aquarium.logic.accounting.dsl.Timeslot
import java.util.concurrent.atomic.AtomicLong
import java.util.{Date, Calendar, GregorianCalendar}
import gr.grnet.aquarium.charging.wallet.WalletEntry
import collection.parallel.mutable
import collection.mutable.ListBuffer
import gr.grnet.aquarium.Aquarium.EnvKeys
import gr.grnet.aquarium.charging.Chargeslot

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


/*
* @author Prodromos Gerakios <pgerakios@grnet.gr>
*/

class EventEntry(val id:String,
                 val unitPrice:String,
                 val startTime:String,
                 val endTime:String,
                 val ellapsedTime:String,
                 val credits:String) extends JsonSupport {

}

class ResourceEntry(val resourceName : String,
                    val resourceType : String,
                    val unitName : String,
                    val totalCredits : String,
                    val eventType:String,
                    val details : List[EventEntry]) extends JsonSupport {

}

class BillEntry(val id:String,
                val userID : String,
                val status : String,
                val remainingCredits:String,
                val deductedCredits:String,
                val startTime:String,
                val endTime:String,
                val bill:List[ResourceEntry]
              )  extends JsonSupport {

}

object BillEntry {

  private[this] val counter = new AtomicLong(0L)
  private[this] def nextUIDObject() = counter.getAndIncrement

  /*private[this] def walletTimeslot(i:WalletEntry) : Timeslot = {
    val cal = new GregorianCalendar
    cal.set(i.billingYear,i.billingMonth,1)
    val dstart = cal.getTime
    val lastDate = cal.getActualMaximum(Calendar.DATE)
    cal.set(Calendar.DATE, lastDate)
    val dend = cal.getTime
   Timeslot(dstart,dend)
  } */

  private[this] def toEventEntry(c:Chargeslot) : EventEntry = {
    val unitPrice = c.unitPrice.toString
    val startTime = c.startMillis.toString
    val endTime   = c.stopMillis.toString
    val difTime   = (c.stopMillis - c.startMillis).toString
    val credits   = c.creditsToSubtract.toString
    new EventEntry(counter.getAndIncrement.toString,unitPrice,
                    startTime,endTime,difTime,credits)

  }


  private[this] def toResourceEntry(w:WalletEntry) : ResourceEntry = {
    assert(w.sumOfCreditsToSubtract==0.0 || w.chargslotCount > 0)
    val rcName =  w.resource.toString
    val rcType =  w.resourceType.name
    val rcUnitName = w.resourceType.unit
    val eventEntry = new ListBuffer[EventEntry]
    val credits = w.sumOfCreditsToSubtract
    val eventType = //TODO: This is hardcoded; find a better solution
        w.currentResourceEvent.clientID match {
          case "pithos" =>
            val action = w.currentResourceEvent.details("action")
            val path = w.currentResourceEvent.details("path")
            "%s@%s".format(action,path)
          case "cyclades" =>
            w.currentResourceEvent.value.toInt match {
              case 0 => // OFF
                  "offOn"
              case 1 =>  // ON
                 "onOff"
              case 2 =>
                 "destroy"
              case _ =>
                 "BUG"
            }
          case "astakos" =>
            "once"
        }

    for { c <- w.chargeslots }{
      if(c.creditsToSubtract != 0.0) {
        //Console.err.println("c.creditsToSubtract : " + c.creditsToSubtract)
        eventEntry += toEventEntry(c)
        //credits += c.creditsToSubtract
      }
    }
    //Console.err.println("TOTAL resource event credits: " + credits)
    new ResourceEntry(rcName,rcType,rcUnitName,credits.toString,eventType.toString,eventEntry.toList)
  }

  private[this] def resourceEntriesAt(t:Timeslot,w:WorkingUserState) : (List[ResourceEntry],Double) = {
    val ret = new ListBuffer[ResourceEntry]
    var sum = 0.0
    //Console.err.println("Wallet entries: " + w.walletEntries)
    for { i <- w.walletEntries} {
      if(t.contains(i.referenceTimeslot) && i.sumOfCreditsToSubtract != 0.0){
        //Console.err.println("i.sumOfCreditsToSubtract : " + i.sumOfCreditsToSubtract)
        sum += i.sumOfCreditsToSubtract
        ret += toResourceEntry(i)
      } else {
        //Console.err.println("WALLET ENTERY : " + i + "\n" +
          //           t + "  does not contain " +  i.referenceTimeslot + "  !!!!")
      }
    }
    (ret.toList,sum)
  }

  def fromWorkingUserState(t:Timeslot,userID:String,w:Option[WorkingUserState]) : BillEntry = {
    val ret = w match {
      case None =>
          new BillEntry(counter.getAndIncrement.toString,
                        userID,"processing",
                        "0.0",
                        "0.0",
                        t.from.getTime.toString,t.to.getTime.toString,
                        Nil)
      case Some(w) =>
        val (rcEntries,rcEntriesCredits) = resourceEntriesAt(t,w)
        new BillEntry(counter.getAndIncrement.toString,
                      userID,"ok",
                      w.totalCredits.toString,
                      rcEntriesCredits.toString,
                      t.from.getTime.toString,t.to.getTime.toString,
                      rcEntries)
    }
    //Console.err.println("JSON: " +  ret.toJsonString)
    ret
  }

  //
  def main(args: Array[String]) = {
    //Console.err.println("JSON: " +  (new BillEntry).toJsonString)
    val propsfile = new FileStreamResource(new File("aquarium.properties"))
    var _props: Props = Props(propsfile)(StdConverters.AllConverters).getOr(Props()(StdConverters.AllConverters))
    val aquarium = new AquariumBuilder(_props, ResourceLocator.DefaultPolicyModel).
      update(Aquarium.EnvKeys.storeProvider, new MemStoreProvider).
      update(Aquarium.EnvKeys.eventsStoreFolder,Some(new File(".."))).
      build()
    aquarium.start()
    ()
  }
}