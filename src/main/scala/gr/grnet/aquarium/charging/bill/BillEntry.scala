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
import gr.grnet.aquarium.converter.StdConverters
import gr.grnet.aquarium.logic.accounting.dsl.Timeslot
import gr.grnet.aquarium.message.avro.{AvroHelpers, MessageHelpers}
import gr.grnet.aquarium.message.avro.gen._
import gr.grnet.aquarium.util.json.JsonSupport
import gr.grnet.aquarium.{Real, Aquarium, ResourceLocator, AquariumBuilder}
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import scala.collection.immutable.TreeMap
import scala.collection.mutable.ListBuffer
import scala.Some
import gr.grnet.aquarium.policy.ResourceType
import java.util


/*
* @author Prodromos Gerakios <pgerakios@grnet.gr>
*/
/*
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
                         val resourceType : String,
                         val unitName : String,
                         val totalCredits : String,
                         val totalElapsedTime : String,
                         val totalUnits : String,
                         val details : List[EventEntry])
extends JsonSupport {
   //var unitName = "EMPTY_UNIT_NAME"
   //var resourceType = "EMPTY_RESOURCE_TYPE"
}

case class ServiceEntry(val serviceName: String,
                        val totalCredits : String,
                        val totalElapsedTime : String,
                        val totalUnits:String,
                        val unitName:String,
                        val details: List[ResourceEntry]
                       )
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
 extends JsonSupport {}
*/

object BillEntryMsg {

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
  //private[this] val emptyJavaList = new java.util.ArrayList[String]()
  private[this] def newChargeEntry(id:String,
                                    unitPrice:String,
                                    startTime:String,
                                    endTime:String,
                                    elapsedTime:String,
                                    units:String,
                                    credits:String) : ChargeEntryMsg = {
    val msg = new ChargeEntryMsg
    msg.setId(id)
    msg.setUnitPrice(unitPrice)
    msg.setStartTime(startTime)
    msg.setEndTime(endTime)
    msg.setTotalElapsedTime(elapsedTime)
    msg.setTotalUnits(units)
    msg.setTotalCredits(credits)
    //msg.setDetails(emptyJavaList)
    msg
  }
  
  private[this] def newEventEntry(eventType : String,
                                  details   : java.util.List[ChargeEntryMsg]) : EventEntryMsg = {

    val msg = new EventEntryMsg
    msg.setEventType(eventType)
    msg.setDetails(details)
    msg
  }
  
  private [this] def newResourceEntry( resourceName : String,
                            resourceType : String,
                            unitName : String,
                            totalCredits : String,
                            totalElapsedTime : String,
                            totalUnits : String,
                            details : java.util.List[EventEntryMsg]) : ResourceEntryMsg = {
    val msg = new ResourceEntryMsg
    msg.setResourceName(resourceName)
    msg.setResourceType(resourceType)
    msg.setUnitName(unitName)
    msg.setTotalCredits(totalCredits)
    msg.setTotalElapsedTime(totalElapsedTime)
    msg.setTotalUnits(totalUnits)
    msg.setDetails(details)
    msg
  }

  private[this] def newServiceEntry( serviceName: String,
                           totalCredits : String,
                           totalElapsedTime : String,
                           totalUnits:String,
                           unitName:String,
                           details: java.util.List[ResourceEntryMsg]
                           ) : ServiceEntryMsg =  {
    val msg = new ServiceEntryMsg
    msg.setServiceName(serviceName)
    msg.setTotalCredits(totalCredits)
    msg.setTotalElapsedTime(totalElapsedTime)
    msg.setTotalUnits(totalUnits)
    msg.setUnitName(unitName)
    msg.setDetails(details)
    msg
 }


  private[this] def newBillEntry( id:String,
                   userID : String,
                   status : String,
                   remainingCredits:String,
                   deductedCredits:String,
                   startTime:String,
                   endTime:String,
                   bill: java.util.List[ServiceEntryMsg]
                   ) : BillEntryMsg =  {
    val msg = new BillEntryMsg
    msg.setId(id)
    msg.setUserID(userID)
    msg.setStatus(status)
    msg.setRemainingCredits(remainingCredits)
    msg.setDeductedCredits(deductedCredits)
    msg.setStartTime(startTime)
    msg.setEndTime(endTime)
    msg.setDetails(bill)
    msg
  }

  private[this] def javaList[A](l:A*) : java.util.List[A] = {
    val al = new java.util.ArrayList[A]()
    l.foreach(al.add(_))
    al
  }


  private[this] def toChargeEntry(c:ChargeslotMsg) : (ChargeEntryMsg,Long,Real) = {
    val unitPrice = c.getUnitPrice.toString
    val startTime = c.getStartMillis.toString
    val endTime   = c.getStopMillis.toString
    val difTime   = (c.getStopMillis - c.getStartMillis).toLong
    val unitsD     = (Real(c.getCreditsToSubtract)/Real(c.getUnitPrice))
    val credits   = c.getCreditsToSubtract.toString
    (newChargeEntry(counter.getAndIncrement.toString,unitPrice,
                    startTime,endTime,difTime.toString,unitsD.toString,credits),difTime,unitsD)
  }



  private[this] def toEventEntry(eventType:String,c:ChargeslotMsg) : (EventEntryMsg,Long,Real) = {
    val (c1,l1,d1) = toChargeEntry(c)
    (newEventEntry(eventType,javaList(c1)),l1,d1)
  }


  private[this] def toResourceEntry(w:WalletEntryMsg) : ResourceEntryMsg = {
    assert(w.getSumOfCreditsToSubtract==0.0 || MessageHelpers.chargeslotCountOf(w) > 0)
    val rcType =  w.getResourceType.getName
    val rcName = rcType match {
            case "diskspace" =>
              String.valueOf(MessageHelpers.currentResourceEventOf(w).getDetails.get("path").getAnyValue)
            case _ =>
              MessageHelpers.currentResourceEventOf(w).getInstanceID
        }
    val rcUnitName = w.getResourceType.getUnit
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
    var totalUnits = Real.Zero
    val eventEntry = new java.util.ArrayList[EventEntryMsg]() //new ListBuffer[EventEntry]
      for { c <- w.getChargeslots.asScala }{
        if(c.getCreditsToSubtract != 0.0) {
          //Console.err.println("c.creditsToSubtract : " + c.creditsToSubtract)
          val (e,l,u) = toEventEntry(eventType.toString,c)
          eventEntry.add(e) //eventEntry +=  e
          totalElapsedTime += l
          totalUnits += u
        }
      }
    //Console.err.println("TOTAL resource event credits: " + credits)
    /*val re =*/ newResourceEntry(rcName,rcType,rcUnitName,credits.toString,totalElapsedTime.toString,
                       totalUnits.toString,eventEntry)
    //re.unitName = rcUnitName
    //re.resourceType = rcType
    //re
  }

  private[this] def resourceEntriesAt(t:Timeslot,w:UserStateMsg) : (java.util.List[ResourceEntryMsg],Double) = {
    var sum = 0.0
    //Console.err.println("Wallet entries: " + w.walletEntries)
    import scala.collection.JavaConverters.asScalaBufferConverter
    val walletEntries = w.getWalletEntries.asScala
    /*Console.err.println("Wallet entries ")
    for { i <- walletEntries }
      Console.err.println("WALLET ENTRY\n%s\nEND WALLET ENTRY".format(i.toJsonString))
    Console.err.println("End wallet entries")*/
    val ret = new java.util.ArrayList[ResourceEntryMsg]()//new ListBuffer[ResourceEntry]
    for { i <- walletEntries} {
      val referenceTimeslot = MessageHelpers.referenceTimeslotOf(i)
      if(t.contains(referenceTimeslot) && i.getSumOfCreditsToSubtract.toDouble != 0.0){
        /*Console.err.println("i.sumOfCreditsToSubtract : " + i.sumOfCreditsToSubtract)*/
        if(i.getSumOfCreditsToSubtract.toDouble > 0.0D)
          sum += i.getSumOfCreditsToSubtract.toDouble
          ret.add(toResourceEntry(i)) //ret += toResourceEntry(i)
      } else {
        val ijson = AvroHelpers.jsonStringOfSpecificRecord(i)
        val itimeslot = MessageHelpers.referenceTimeslotOf(i)
       // Console.err.println("IGNORING WALLET ENTRY : " + ijson + "\n" +
       //              t + "  does not contain " +  itimeslot + "  !!!!")
      }
    }
    (ret,sum)
  }

  private[this] def aggregateResourceEntries(re:java.util.List[ResourceEntryMsg]) : java.util.List[ServiceEntryMsg] = {
    def addResourceEntries(a:ResourceEntryMsg,b:ResourceEntryMsg) : ResourceEntryMsg = {
      assert(a.getResourceName == b.getResourceName)
      val totalCredits = (a.getTotalCredits.toDouble+b.getTotalCredits.toDouble).toString
      val totalElapsedTime =  (a.getTotalElapsedTime.toLong+b.getTotalElapsedTime.toLong).toString
      val totalUnits =  (a.getTotalUnits.toDouble+b.getTotalUnits.toDouble).toString
      val msg = new ResourceEntryMsg

      val details = new java.util.ArrayList[EventEntryMsg](a.getDetails)
      details.addAll(b.getDetails)
      msg.setResourceName(a.getResourceName)
      msg.setResourceType(a.getResourceType)
      msg.setUnitName(a.getUnitName)
      msg.setTotalCredits(totalCredits)
      msg.setTotalElapsedTime(totalElapsedTime)
      msg.setTotalUnits(totalUnits)
      msg.setDetails(details)
      msg
      /*/*val ab =*/ a.copy(a.getResourceName,a.getResourceType,a.getUnitName,totalCredits,totalElapsedTime,totalUnits,
                    {a.getDetails.addAll(b.details); a})
      //ab.unitName = a.unitName
      //ab.resourceType = a.resourceType
      //ab*/
    }
    import scala.collection.JavaConverters.asScalaBufferConverter
    val map0 = re.asScala.foldLeft(TreeMap[String,ResourceEntryMsg]()){ (map,r1) =>
      map.get(r1.getResourceName) match {
        case None => map + ((r1.getResourceName,r1))
        case Some(r0) => (map - r0.getResourceName) +
                         ((r0.getResourceName, addResourceEntries(r0,r1)))
      }
    }
    val map1 = map0.foldLeft(TreeMap[String,List[ResourceEntryMsg]]()){ case (map,(_,r1)) =>
      map.get(r1.getResourceType) match {
        case None =>  map + ((r1.getResourceType,List(r1)))
        case Some(rl) => (map - r1.getResourceType) +  ((r1.getResourceType,r1::rl))
      }
    }
    import scala.collection.JavaConverters.seqAsJavaListConverter
    map1.foldLeft(List[ServiceEntryMsg]()){ case (ret,(serviceName,resList)) =>
      val (totalCredits,totalElapsedTime,totalUnits) =
        resList.foldLeft((0.0D,0L,0.0D)){ case ((a,b,c),r) =>
            (a+r.getTotalCredits.toDouble,
             b+r.getTotalElapsedTime.toLong,
             c+r.getTotalUnits.toDouble
            )}
      newServiceEntry(serviceName,totalCredits.toString,
                       totalElapsedTime.toString,totalUnits.toString,
                       resList.head.getUnitName,resList.asJava) :: ret
    }.asJava
  }

  def addMissingServices(se0:java.util.List[ServiceEntryMsg],re:Map[String,ResourceType]) :
   java.util.List[ServiceEntryMsg]= {
    import scala.collection.JavaConverters.asScalaBufferConverter
    val se = se0.asScala.toList
    import scala.collection.JavaConverters.seqAsJavaListConverter
    (se :::(re -- se.map(_.getServiceName).toSet).foldLeft(List[ServiceEntryMsg]()) { case (ret,(name,typ:ResourceType)) =>
      newServiceEntry(name,"0.0","0","0.0",typ.unit,new java.util.ArrayList[ResourceEntryMsg]()) :: ret
    }).asJava
  }

  def fromWorkingUserState(t0:Timeslot,userID:String,w:Option[UserStateMsg],
                           resourceTypes:Map[String,ResourceType]) : BillEntryMsg = {
    val t = t0.roundMilliseconds /* we do not care about milliseconds */
    //Console.err.println("Timeslot: " + t0)
    //Console.err.println("After rounding timeslot: " + t)
    val ret = w match {
      case None =>
          val allMissing = addMissingServices(new util.ArrayList[ServiceEntryMsg](),resourceTypes)
          newBillEntry(counter.getAndIncrement.toString,
                        userID,"processing",
                        "0.0",
                        "0.0",
                        t.from.getTime.toString,t.to.getTime.toString,
                        allMissing)
      case Some(w) =>
        val wjson = AvroHelpers.jsonStringOfSpecificRecord(w)
        //Console.err.println("Working user state: %s".format(wjson))
        val (rcEntries,rcEntriesCredits) = resourceEntriesAt(t,w)
        val resList0 = aggregateResourceEntries(rcEntries)
        val resList1 = addMissingServices(resList0,resourceTypes)
        newBillEntry(counter.getAndIncrement.toString,
                      userID,"ok",
                      w.getTotalCredits.toString,
                      rcEntriesCredits.toString,
                      t.from.getTime.toString,
                      t.to.getTime.toString,
                      resList1)
    }
    //Console.err.println("JSON: " +  AvroHelpers.jsonStringOfSpecificRecord(ret))
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