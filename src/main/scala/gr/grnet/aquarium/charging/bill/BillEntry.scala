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
import gr.grnet.aquarium.policy.ResourceType


/*
* @author Prodromos Gerakios <pgerakios@grnet.gr>
*/

case class ChargeEntry(val id:String,
                       val unitPrice:String,
                       val startTime:String,
                       val endTime:String,
                       val elapsedTime:String,
                       val units:String,
                       val credits:String)
  extends JsonSupport {}


class EventEntry(val eventType : String,
                 val details   : List[ChargeEntry])
 extends JsonSupport {}


case class ResourceEntry(val resourceName : String,
                         //val resourceType : String,
                         //val unitName : String,
                         val totalCredits : String,
                         val totalElapsedTime : String,
                         val totalUnits : String,
                         val details : List[EventEntry])
extends JsonSupport {
   var unitName = "EMPTY_UNIT_NAME"
   var resourceType = "EMPTY_RESOURCE_TYPE"
}

case class ServiceEntry(val serviceName: String,
                        val totalCredits : String,
                        val totalElapsedTime : String,
                        val totalUnits:String,
                        val unitName:String,
                        val details: List[ResourceEntry]
                       )
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
                val bill:List[ServiceEntry]
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

  private[this] def toChargeEntry(c:ChargeslotMsg) : (ChargeEntry,Long,Double) = {
    val unitPrice = c.getUnitPrice.toString
    val startTime = c.getStartMillis.toString
    val endTime   = c.getStopMillis.toString
    val difTime   = (c.getStopMillis - c.getStartMillis).toLong
    val unitsD     = (c.getCreditsToSubtract/c.getUnitPrice)
    val credits   = c.getCreditsToSubtract.toString
    (new ChargeEntry(counter.getAndIncrement.toString,unitPrice,
                    startTime,endTime,difTime.toString,unitsD.toString,credits),difTime,unitsD)
  }

  private[this] def toEventEntry(eventType:String,c:ChargeslotMsg) : (EventEntry,Long,Double) = {
    val (c1,l1,d1) = toChargeEntry(c)
    (new EventEntry(eventType,List(c1)),l1,d1)
  }


  private[this] def toResourceEntry(w:WalletEntryMsg) : ResourceEntry = {
    assert(w.getSumOfCreditsToSubtract==0.0 || MessageHelpers.chargeslotCountOf(w) > 0)
    val rcType =  w.getResourceType.getName
    val rcName = rcType match {
            case "diskspace" =>
              String.valueOf(MessageHelpers.currentResourceEventOf(w).getDetails.get("path").getAnyValue)
            case _ =>
              MessageHelpers.currentResourceEventOf(w).getInstanceID
        }
    val rcUnitName = w.getResourceType.getUnit
    val eventEntry = new ListBuffer[EventEntry]
    val credits = w.getSumOfCreditsToSubtract
    val eventType = //TODO: This is hardcoded; find a better solution
        rcType match {
          case "diskspace" =>
            val action = MessageHelpers.currentResourceEventOf(w).getDetails.get("action").getAnyValue
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
    //w.
    import scala.collection.JavaConverters.asScalaBufferConverter
    ///FIXME: val elapsedTime = w.getChargeslots.asScala.foldLeft()
   //c.getStopMillis - c.getStartMillis
    var totalElapsedTime = 0L
    var totalUnits = 0.0D
      for { c <- w.getChargeslots.asScala }{
        if(c.getCreditsToSubtract != 0.0) {
          //Console.err.println("c.creditsToSubtract : " + c.creditsToSubtract)
          val (e,l,u) = toEventEntry(eventType.toString,c)
          eventEntry +=  e
          totalElapsedTime += l
          totalUnits += u
        }
      }
    //Console.err.println("TOTAL resource event credits: " + credits)
    val re = new ResourceEntry(rcName,/*rcType,rcUnitName,*/credits.toString,totalElapsedTime.toString,
                       totalUnits.toString,eventEntry.toList)
    re.unitName = rcUnitName
    re.resourceType = rcType
    re
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
        if(i.getSumOfCreditsToSubtract.toDouble > 0.0D)
          sum += i.getSumOfCreditsToSubtract.toDouble
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

  private[this] def aggregateResourceEntries(re:List[ResourceEntry]) : List[ServiceEntry] = {
    def addResourceEntries(a:ResourceEntry,b:ResourceEntry) : ResourceEntry = {
      assert(a.resourceName == b.resourceName)
      val totalCredits = (a.totalCredits.toDouble+b.totalCredits.toDouble).toString
      val totalElapsedTime =  (a.totalElapsedTime.toLong+b.totalElapsedTime.toLong).toString
      val totalUnits =  (a.totalUnits.toDouble+b.totalUnits.toDouble).toString
      val ab = a.copy(a.resourceName/*,a.resourceType,a.unitName*/,totalCredits,totalElapsedTime,totalUnits,
             a.details ::: b.details)
      ab.unitName = a.unitName
      ab.resourceType = a.resourceType
      ab
    }
    val map0 = re.foldLeft(TreeMap[String,ResourceEntry]()){ (map,r1) =>
      map.get(r1.resourceName) match {
        case None => map + ((r1.resourceName,r1))
        case Some(r0) => (map - r0.resourceName) +
                         ((r0.resourceName, addResourceEntries(r0,r1)))
      }
    }
    val map1 = map0.foldLeft(TreeMap[String,List[ResourceEntry]]()){ case (map,(_,r1)) =>
      map.get(r1.resourceType) match {
        case None =>  map + ((r1.resourceType,List(r1)))
        case Some(rl) => (map - r1.resourceType) +  ((r1.resourceName,r1::rl))
      }
    }
    map1.foldLeft(List[ServiceEntry]()){ case (ret,(serviceName,resList)) =>
      val (totalCredits,totalElapsedTime,totalUnits) =
        resList.foldLeft((0.0D,0L,0.0D)){ case ((a,b,c),r) =>
            (a+r.totalCredits.toDouble,
             b+r.totalElapsedTime.toLong,
             c+r.totalUnits.toDouble
            )}
      new ServiceEntry(serviceName,totalCredits.toString,
                       totalElapsedTime.toString,totalUnits.toString,
                       resList.head.unitName,resList) :: ret
    }
  }

  def addMissingServices(se:List[ServiceEntry],re:Map[String,ResourceType]) : List[ServiceEntry]=
    se:::(re -- se.map(_.serviceName).toSet).foldLeft(List[ServiceEntry]()) { case (ret,(name,typ:ResourceType)) =>
      new ServiceEntry(name,"0.0","0","0.0",typ.unit,List[ResourceEntry]()) :: ret
    }

  def fromWorkingUserState(t0:Timeslot,userID:String,w:Option[UserStateMsg],
                           resourceTypes:Map[String,ResourceType]) : AbstractBillEntry = {
    val t = t0.roundMilliseconds /* we do not care about milliseconds */
    //Console.err.println("Timeslot: " + t0)
    //Console.err.println("After rounding timeslot: " + t)
    val ret = w match {
      case None =>
          val allMissing = addMissingServices(Nil,resourceTypes)
          new BillEntry(counter.getAndIncrement.toString,
                        userID,"processing",
                        "0.0",
                        "0.0",
                        t.from.getTime.toString,t.to.getTime.toString,
                        allMissing)
      case Some(w) =>
        val wjson = AvroHelpers.jsonStringOfSpecificRecord(w)
        Console.err.println("Working user state: %s".format(wjson))
        val (rcEntries,rcEntriesCredits) = resourceEntriesAt(t,w)
        val resList0 = aggregateResourceEntries(rcEntries)
        val resList1 = addMissingServices(resList0,resourceTypes)
        new BillEntry(counter.getAndIncrement.toString,
                      userID,"ok",
                      w.getTotalCredits.toString,
                      rcEntriesCredits.toString,
                      t.from.getTime.toString,
                      t.to.getTime.toString,
                      resList1)
    }
    //Console.err.println("JSON: " +  ret.toJsonString)
    ret
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