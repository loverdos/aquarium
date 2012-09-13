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

package gr.grnet.aquarium.charging.bill

import com.ckkloverdos.props.Props
import com.ckkloverdos.resource.FileStreamResource
import gr.grnet.aquarium.converter.{CompactJsonTextFormat, StdConverters}
import gr.grnet.aquarium.logic.accounting.dsl.Timeslot
import gr.grnet.aquarium.message.avro.{AvroHelpers, MessageHelpers}
import gr.grnet.aquarium.message.avro.gen.{ChargeslotMsg, WalletEntryMsg, UserStateMsg}
import gr.grnet.aquarium.store.memory.MemStoreProvider
import gr.grnet.aquarium.util.json.JsonSupport
import gr.grnet.aquarium.{Aquarium, ResourceLocator, AquariumBuilder}
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import scala.collection.immutable.TreeMap
import scala.collection.mutable.ListBuffer


/*
* @author Prodromos Gerakios <pgerakios@grnet.gr>
*/

case class ChargeEntry(val id:String,
                       val unitPrice:String,
                       val startTime:String,
                       val endTime:String,
                       val ellapsedTime:String,
                       val credits:String)
  extends JsonSupport {}


class EventEntry(val eventType : String,
                 val details   : List[ChargeEntry])
 extends JsonSupport {}


case class ResourceEntry(val resourceName : String,
                         val resourceType : String,
                         val unitName : String,
                         val totalCredits : String,
                         val details : List[EventEntry])
extends JsonSupport {}


abstract class AbstractBillEntry
 extends JsonSupport {}

class BillEntry(val id:String,
                val userID : String,
                val status : String,
                val remainingCredits:String,
                val deductedCredits:String,
                val startTime:String,
                val endTime:String,
                val bill:List[ResourceEntry]
              )
 extends AbstractBillEntry {}


object AbstractBillEntry {

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

  private[this] def toChargeEntry(c:ChargeslotMsg) : ChargeEntry = {
    val unitPrice = c.getUnitPrice.toString
    val startTime = c.getStartMillis.toString
    val endTime   = c.getStopMillis.toString
    val difTime   = (c.getStopMillis - c.getStartMillis).toString
    val credits   = c.getCreditsToSubtract.toString
    new ChargeEntry(counter.getAndIncrement.toString,unitPrice,
                    startTime,endTime,difTime,credits)
  }

  private[this] def toEventEntry(eventType:String,c:ChargeslotMsg) : EventEntry =
    new EventEntry(eventType,List(toChargeEntry(c)))


  private[this] def toResourceEntry(w:WalletEntryMsg) : ResourceEntry = {
    assert(w.getSumOfCreditsToSubtract==0.0 || MessageHelpers.chargeslotCountOf(w) > 0)
    val rcType =  w.getResourceType.getName
    val rcName = rcType match {
            case "diskspace" =>
              String.valueOf(MessageHelpers.currentResourceEventOf(w).getDetails.get("path"))
            case _ =>
              MessageHelpers.currentResourceEventOf(w).getInstanceID
        }
    val rcUnitName = w.getResourceType.getUnit
    val eventEntry = new ListBuffer[EventEntry]
    val credits = w.getSumOfCreditsToSubtract
    val eventType = //TODO: This is hardcoded; find a better solution
        rcType match {
          case "diskspace" =>
            val action = MessageHelpers.currentResourceEventOf(w).getDetails.get("action")
            //val path = MessageHelpers.currentResourceEventOf(w).getDetails.get("path")
            //"%s@%s".format(action,path)
            action
          case "vmtime" =>
            MessageHelpers.currentResourceEventOf(w).getValue.toInt match {
              case 0 => // OFF
                  "offOn"
              case 1 =>  // ON
                 "onOff"
              case 2 =>
                 "destroy"
              case _ =>
                 "BUG"
            }
          case "addcredits" =>
            "once"
        }

    import scala.collection.JavaConverters.asScalaBufferConverter
    for { c <- w.getChargeslots.asScala }{
      if(c.getCreditsToSubtract != 0.0) {
        //Console.err.println("c.creditsToSubtract : " + c.creditsToSubtract)
        eventEntry += toEventEntry(eventType.toString,c)
        //credits += c.creditsToSubtract
      }
    }
    //Console.err.println("TOTAL resource event credits: " + credits)
    new ResourceEntry(rcName,rcType,rcUnitName,credits.toString,eventEntry.toList)
  }

  private[this] def resourceEntriesAt(t:Timeslot,w:UserStateMsg) : (List[ResourceEntry],Double) = {
    val ret = new ListBuffer[ResourceEntry]
    var sum = 0.0
    //Console.err.println("Wallet entries: " + w.walletEntries)
    import scala.collection.JavaConverters.asScalaBufferConverter
    val walletEntries = w.getWalletEntries.asScala
    /*Console.err.println("Wallet entries ")
    for { i <- walletEntries }
      Console.err.println("WALLET ENTRY\n%s\nEND WALLET ENTRY".format(i.toJsonString))
    Console.err.println("End wallet entries")*/
    for { i <- walletEntries} {
      val referenceTimeslot = MessageHelpers.referenceTimeslotOf(i)
      if(t.contains(referenceTimeslot) && i.getSumOfCreditsToSubtract.toDouble != 0.0){
        /*Console.err.println("i.sumOfCreditsToSubtract : " + i.sumOfCreditsToSubtract)*/
        if(i.getSumOfCreditsToSubtract.toDouble > 0.0D) sum += i.getSumOfCreditsToSubtract.toDouble
        ret += toResourceEntry(i)
      } else {
        val ijson = AvroHelpers.jsonStringOfSpecificRecord(i)
        val itimeslot = MessageHelpers.referenceTimeslotOf(i)
        Console.err.println("IGNORING WALLET ENTRY : " + ijson + "\n" +
                     t + "  does not contain " +  itimeslot + "  !!!!")
      }
    }
    (ret.toList,sum)
  }

  private[this] def aggregateResourceEntries(re:List[ResourceEntry]) : List[ResourceEntry] = {
    def addResourceEntries(a:ResourceEntry,b:ResourceEntry) : ResourceEntry = {
      assert(a.resourceName == b.resourceName)
      val totalCredits = (a.totalCredits.toDouble+b.totalCredits.toDouble).toString
      a.copy(a.resourceName,a.resourceType,a.unitName,totalCredits,a.details ::: b.details)
    }
    re.foldLeft(TreeMap[String,ResourceEntry]()){ (map,r1) =>
      map.get(r1.resourceName) match {
        case None => map + ((r1.resourceName,r1))
        case Some(r0) => (map - r0.resourceName) +
                         ((r0.resourceName, addResourceEntries(r0,r1)))
      }
    }.values.toList
  }

  def fromWorkingUserState(t0:Timeslot,userID:String,w:Option[UserStateMsg]) : AbstractBillEntry = {
    val t = t0.roundMilliseconds /* we do not care about milliseconds */
    //Console.err.println("Timeslot: " + t0)
    //Console.err.println("After rounding timeslot: " + t)
    val ret = w match {
      case None =>
          new BillEntry(counter.getAndIncrement.toString,
                        userID,"processing",
                        "0.0",
                        "0.0",
                        t.from.getTime.toString,t.to.getTime.toString,
                        Nil)
      case Some(w) =>
        val wjson = AvroHelpers.jsonStringOfSpecificRecord(w)
        Console.err.println("Working user state: %s".format(wjson))
        val (rcEntries,rcEntriesCredits) = resourceEntriesAt(t,w)
        val resMap = aggregateResourceEntries(rcEntries)
        new BillEntry(counter.getAndIncrement.toString,
                      userID,"ok",
                      w.getTotalCredits.toString,
                      rcEntriesCredits.toString,
                      t.from.getTime.toString,t.to.getTime.toString,
                      resMap)
    }
    //Console.err.println("JSON: " +  ret.toJsonString)
    ret
  }

  val jsonSample = "{\n  \"id\":\"2\",\n  \"userID\":\"loverdos@grnet.gr\",\n  \"status\":\"ok\",\n  \"remainingCredits\":\"3130.0000027777783\",\n  \"deductedCredits\":\"5739.9999944444435\",\n  \"startTime\":\"1341090000000\",\n  \"endTime\":\"1343768399999\",\n  \"bill\":[{\n    \"resourceName\":\"diskspace\",\n    \"resourceType\":\"diskspace\",\n    \"unitName\":\"MB/Hr\",\n    \"totalCredits\":\"2869.9999972222217\",\n    \"eventType\":\"object update@/Papers/GOTO_HARMFUL.PDF\",\n\t    \"details\":[\n\t     {\"totalCredits\":\"2869.9999972222217\",\n\t      \"details\":[{\n\t      \"id\":\"0\",\n\t      \"unitPrice\":\"0.01\",\n\t      \"startTime\":\"1342735200000\",\n\t      \"endTime\":\"1343768399999\",\n\t      \"ellapsedTime\":\"1033199999\",\n\t      \"credits\":\"2869.9999972222217\"\n\t    \t}]\n\t    }\n\t  ]\n  },{\n    \"resourceName\":\"diskspace\",\n    \"resourceType\":\"diskspace\",\n    \"unitName\":\"MB/Hr\",\n    \"totalCredits\":\"2869.9999972222217\",\n    \"eventType\":\"object update@/Papers/GOTO_HARMFUL.PDF\",\n    \"details\":[\t     {\"totalCredits\":\"2869.9999972222217\",\n\t      \"details\":[{\n\t      \"id\":\"0\",\n\t      \"unitPrice\":\"0.01\",\n\t      \"startTime\":\"1342735200000\",\n\t      \"endTime\":\"1343768399999\",\n\t      \"ellapsedTime\":\"1033199999\",\n\t      \"credits\":\"2869.9999972222217\"\n\t    \t}]\n\t    }\n\t]\n  }]\n}"

  def main0(args: Array[String]) = {
     val b : BillEntry = StdConverters.AllConverters.convertEx[BillEntry](CompactJsonTextFormat(jsonSample))
     val l0 = b.bill
     val l1 = aggregateResourceEntries(l0)

     Console.err.println("Initial resources: ")
     for{ i <- l0 } Console.err.println("RESOURCE: " + i.toJsonString)
    Console.err.println("Aggregate resources: ")
    for{ a <- l1 } {
      Console.err.println("RESOURCE:  %s\n  %s\nEND RESOURCE".format(a.resourceName,a.toJsonString))
    }

    val aggr = new BillEntry(b.id,b.userID,b.status,b.remainingCredits,b.deductedCredits,b.startTime,b.endTime,l1)
    Console.err.println("Aggregate:\n" + aggr.toJsonString)
  }

  //
  def main(args: Array[String]) = {
    //Console.err.println("JSON: " +  (new BillEntry).toJsonString)
    val propsfile = new FileStreamResource(new File("aquarium.properties"))
    var _props: Props = Props(propsfile)(StdConverters.AllConverters).getOr(Props()(StdConverters.AllConverters))
    val aquarium = new AquariumBuilder(_props, ResourceLocator.DefaultPolicyMsg).
      //update(Aquarium.EnvKeys.storeProvider, new MemStoreProvider).
      update(Aquarium.EnvKeys.eventsStoreFolder,Some(new File(".."))).
      build()
    aquarium.start()
    ()
  }
}