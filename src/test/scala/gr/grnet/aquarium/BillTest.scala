package gr.grnet.aquarium

import com.ckkloverdos.resource.FileStreamResource
import converter.StdConverters
import event.model.ExternalEventModel
import event.model.im.StdIMEvent
import event.model.resource.StdResourceEvent
import java.io.{InputStreamReader, BufferedReader, File}
import com.ckkloverdos.props.Props
import logic.accounting.dsl.Timeslot
import store.memory.MemStoreProvider
import java.util.concurrent.atomic.AtomicLong
import java.text.SimpleDateFormat
import java.net.{URLConnection, URL}
import util.Loggable
import java.util.{GregorianCalendar, Date,Calendar}
import gr.grnet.aquarium.policy.CronSpec
import scala.Tuple2
import scala.Tuple2

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


object UID {
  private[this] val counter = new AtomicLong(0L)
  def next() = counter.getAndIncrement
  def random(min:Int=Int.MinValue,max:Int=Int.MaxValue) =
      min + (scala.math.random.toInt % (max+1)) % (max+1)

  def random[A](l:List[A]) : A = {
    val sz = l.size
    if(sz==0) throw new Exception("random")
     l(random(0,sz-1))
  }
}

object Process {
  private[this] def exec(cmd : String,func : String=>Unit) : Unit = {
    val commands = cmd.split(" ")
    val proc = new ProcessBuilder(commands: _*).redirectErrorStream(true).start();
    val ins = new java.io.BufferedReader(new java.io.InputStreamReader(proc.getInputStream))
    val sb = new StringBuilder

    //spin off a thread to read process output.
    val outputReaderThread = new Thread(new Runnable(){
      def run : Unit = {
        var ln : String = null
        while({ln = ins.readLine; ln != null})
          func(ln)
      }
    })
    outputReaderThread.start()

    //suspense this main thread until sub process is done.
    proc.waitFor

    //wait until output is fully read/completed.
    outputReaderThread.join()

    ins.close()
  }
  def exec(cmd:String) : Unit = exec(cmd,Console.err.println(_))
}

object Mongo {
  def clear = Process.exec("mongo aquarium --eval db.resevents.remove();db.imevents.remove();db.policies.remove();db.userstates.remove()")
}

object AquariumInstance {
  val propsfile = new FileStreamResource(new File("aquarium.properties"))
  var props: Props = Props(propsfile)(StdConverters.AllConverters).getOr(Props()(StdConverters.AllConverters))
  val aquarium = {
    Mongo.clear
    new AquariumBuilder(props, ResourceLocator.DefaultPolicyModel).
      //update(Aquarium.EnvKeys.storeProvider, new MemStoreProvider).
      update(Aquarium.EnvKeys.eventsStoreFolder,Some(new File(".."))).
      build()
  }
  def run(f : => String) : String = {
    var _ret = ""
    aquarium.start
    Thread.sleep(4)
    try{
      _ret = f
    } finally {
      Console.err.println("Stopping aquarium")
      Thread.sleep(15)
      Console.err.println("Stopping aquarium --- DONE")
      aquarium.stop
    }
    _ret
  }
}


abstract class Message {
  val dbg = true
  val cal =   new GregorianCalendar
  var _range : Timeslot = null
  var _cronSpec : CronSpec = null
  var _messagesSent = 0
  var _done = false
  var _map = Map[String,String]()

  def updateMap(args:Tuple2[String,String]*) : Message  =
    updateMap(args.foldLeft(Map[String,String]())({(map,arg)=> map + arg}))

  def updateMap(map:Map[String,String]) : Message = {
    def mergeMap[A, B](ms: List[Map[A, B]])(f: (B, B) => B): Map[A, B] =
      (Map[A, B]() /: (for (m <- ms; kv <- m) yield kv)) { (a, kv) =>
        a + (if (a.contains(kv._1)) kv._1 -> f(a(kv._1), kv._2) else kv)
    }
    _map =  mergeMap(List(_map,map))((v1,v2) => v2)
    (_map.get("month"),_map.get("spec")) match {
      case (Some((month0:String)),Some(spec)) =>
        val month : Int = month0.toInt
        if((_cronSpec==null ||  _cronSpec.cronSpec != spec ||cal.get(Calendar.MONTH) != month -1)) {
           val d1 = getDate(1,if(month==12) 1 else month+1,year,0,0,0)
           val d0 = getDate(1,month,year,0,0,0)
           _range = Timeslot(d0,d1 - 1000)
          _cronSpec = new CronSpec(if(spec.isEmpty) "* * * * *" else spec)
        }
      case _ => ()
    }
    this
  }

  def done = _done
  def sentMessages = _messagesSent

  def nextTime : Option[Long] = {
    _cronSpec match{
      case null =>
        None
      case _ =>
        _cronSpec.nextValidDate(_range,cal.getTime) match {
          case Some(d) =>
            val millis = d.getTime
            cal.setTimeInMillis(millis)
            Some(millis)
          case None    =>
            None
        }
    }
  }

  def year : Int = {
    cal.setTimeInMillis(System.currentTimeMillis())
    cal.get(Calendar.YEAR)
  }

  def getDate(day:Int,month:Int,year:Int,hour:Int,min:Int,sec:Int) : Long = {
    cal.set(year,month-1,day,hour,min,sec)
    cal.getTimeInMillis
  }

  def getMillis : Long = cal.getTimeInMillis

  def getDate(day:Int,month:Int,year:Int,hour:Int,min:Int) : Long =
    getDate(day,month,year,hour,min,0)

  def setMillis(millis:Long) = {
    cal.setTimeInMillis(millis)
  }

  def addMillis(day:Int,hour:Int) = {
    cal.roll(Calendar.DATE,day)
    cal.roll(Calendar.DATE,hour)
  }

  def nextID = UID.next

  def makeEvent(millis:Long,map:Map[String,String]) : ExternalEventModel

  def send(args:Tuple2[String,String]*) : Boolean =
    send(args.foldLeft(Map[String,String]())({(map,arg)=> map + arg}))

  def send(map:Map[String,String]) : Boolean = {
    nextTime match {
      case Some(millis) =>
        updateMap(map)
        val event = makeEvent(millis,_map)
        val (exchangeName,routingKey) = event match {
          case rc:StdResourceEvent => rc.resource match {
            case "vmtime" =>
              ("cyclades","cyclades.resource.vmtime")
            case "diskspace" =>
              ("pithos","pithos.resource.diskspace")
            case _ =>
              throw new Exception("send cast failed")
          }
          case im:StdIMEvent =>
            ("astakos","astakos.user")
          case _ =>
            throw new Exception("send cast failed")
        }
        val json = event.toJsonString
        AquariumInstance.aquarium(Aquarium.EnvKeys.rabbitMQProducer).
        sendMessage(exchangeName,routingKey,json)
        if(dbg)Console.err.println("Sent message:\n%s\n".format(json))
        _messagesSent += 1
        true
      case None =>
        _done = true
        false
    }
  }

}

class DiskMessage extends Message {
  /*
   *  map:
   *      "action" -> "update" , "delete" , "purge"
   *      "uid"    ->
   *      "path"   ->
   *      "value"  ->
   */
  def makeEvent(millis:Long,map:Map[String,String]) : ExternalEventModel = {
      val action = map("action")
      val uid    = map("uid")
      val path   = map("path")
      val value  = map("value").toLong
      val id = "rc.%d.object.%s".format(nextID,action)
      val occurredMillis = millis
      val receivedMillis = millis
      val userID = uid //"user%s@grnet.gr".format(uid)
      val clientID = "pithos"
      val resource ="diskspace"
      val instanceID = "1"
      val eventVersion = "1.0"
      val details = Map("action" -> "object %s".format(action),
                        "total"  -> "0.0",
                        "user"   -> userID,
                        "path"   -> path)
      new StdResourceEvent(id,occurredMillis,receivedMillis,userID,
                           clientID,resource,instanceID,value,
                           eventVersion,details)
  }
}

class VMMessage extends Message {
  /*
   *   map:
   *      uid        -> unique id for user
   *      instanceID -> "cyclades.vm.kJSOLek"
   *      vmName     -> "My Lab VM"
   *      status     ->  "on", "off" , "destroy"
   */
  var _status = "off"
  def nextStatus = {
    if(_status=="off") _status = "on" else _status = "off"
    _status
  }
  def makeEvent(millis:Long,map:Map[String,String]) : ExternalEventModel = {
    val uid    = map("uid")
    val value  = nextStatus /* map("status") match {
       case "on" => 1.0
       case "off" => 0.0
       case "destroy" => 2.0
       case x => throw new Exception("VMMessage bad status: %s".format(x))
      }*/
    val id = "rc.%d.vmtime".format(nextID)
    val occurredMillis = millis
    val receivedMillis = millis
    val userID = uid // "user%s@grnet.gr".format(uid)
    val clientID = "cyclades"
    val resource ="vmtime"
    val instanceID = map("instanceID")
    val eventVersion = "1.0"
    val details = Map("VM Name" -> map("vmName"))
    new StdResourceEvent(id,occurredMillis,receivedMillis,userID,clientID,
                         resource,instanceID,value.toDouble,eventVersion,details)
  }
 }

class CreationMessage extends Message {
  /*
   *  map contains:
   *   uid -> user id
   */
  def makeEvent(millis:Long,map:Map[String,String]) : ExternalEventModel = {
    val uid    = map("uid")     //
    val id = "im.%d.create.user".format(nextID)
    val occurredMillis = millis
    val receivedMillis = millis
    val userID =  uid //"user%d@grnet.gr".format(mid)
    val clientID = "astakos"
    val isActive = false
    val role = "default"
    val eventVersion = "1.0"
    val eventType = "create"
    new StdIMEvent(id,occurredMillis,receivedMillis,userID,
                  clientID,isActive,role,eventVersion,eventType,
                  Map())
  }
}

class AddCreditsMessage extends Message {
  /*
   *  map contains:
   *    amount -> "2000"
   *    uid    -> loverdos1
   */
  def makeEvent(millis:Long,map:Map[String,String]) : ExternalEventModel = {
    val uid    = map("uid")     //
    val amount = map("amount")
    val id = "im.%d.add.credits".format(nextID)
    val occurredMillis = millis
    val receivedMillis = millis
    val userID = uid //"user%d@grnet.gr".format(uid)
    val clientID = "astakos"
    val isActive = false
    val role = "default"
    val eventVersion = "1.0"
    val eventType = "addcredits"
    new StdIMEvent(id,occurredMillis,receivedMillis,userID,
                   clientID,isActive,role,eventVersion,eventType,
                   Map("credits" -> amount.toString))
  }
}

object Message {
  def apply(typ:String,args:Tuple2[String,String]*) : Message =
    apply(typ,args.foldLeft(Map[String,String]())({(map,arg)=> map + arg}))

  def apply(typ:String,map:Map[String,String]) : Message = {
    val msg =  typ match {
      case "vm" => new VMMessage
      case "disk" =>   new DiskMessage
      case "create" => new CreationMessage
      case "credits" => new AddCreditsMessage
      case _ => throw new Exception("unknown type")
    }
    msg.updateMap(map)
    msg
  }
}


class User(serverAndPort:String,month:Int) {
  val uid = "user%d@grnet.gr".format(UID.next)
  val _creationMessage  : Message = Message("create","uid"->uid,"month"->month.toString,"spec"->"")
  var _resources : List[Message] = Nil



  def add(no:Int,typ:String,args:Tuple2[String,String]*) : User =
    add(no,typ,args.foldLeft(Map[String,String]())({(map,arg)=> map + arg}))

  def add(no:Int,typ:String,map:Map[String,String]) : User  =
    add(no,typ,{_ => map})

  def add(no:Int,typ:String,map:Int=>Map[String,String]) : User  = {
    for {i <- 1 to no} {
      val map0 : Map[String,String] = map(i) + ("uid"->uid) + ("month"->month.toString)
      _resources = Message(typ,map0) :: _resources
    }
    this
  }

  def addVMs(no:Int,status:String,cronSpec:String) : User =
    add(no,"vm",{i =>
         Map("instanceID"->"cyclades.vm.%d".format(i),
         "vmName"  -> "Virtual Machine #%d".format(i),
         "status"  -> status,
         "spec"    -> cronSpec)})

  def addFiles(no:Int,action:String,value:Int,minVal:Int,maxVal:Int,spec:String) : User =
    add(no,"disk",{i =>
       Map("action" -> action,
           "path"->"/Papers/file_%d.PDF".format(i),
           "value"->UID.random(minVal,maxVal).toString,
           "spec" -> spec
          )
    })

  def addCredits(amount:Int,spec:String) : User = {
    add(1,"credits","month"->month.toString,"uid"->uid,"spec"->spec,"amount"->amount.toString)
  }

  def run(minFile:Int,maxFile:Int,minAmount:Int,maxAmount:Int,maxJSONRetry:Int=10) : String =  {
    _creationMessage.send("month"->month.toString,"uid"->uid,"spec"->"0 0 * %d ?".format(month)) // send once!
    var iter = _resources.toList
    var done = false
    while(!iter.isEmpty){
      iter = _resources.filterNot(_.done)
      for{i<-iter}
        i.send("value"->UID.random(minFile,maxFile).toString,
               "amount"->UID.random(minAmount,maxAmount).toString //,
               //"status" -> UID.random(List("off","on"))
               )
    }
    getJSON(maxJSONRetry)
  }

  def getJSON(max:Int=10) : String = {
    def get () : String = {
      val fromMillis = _creationMessage._range.from.getTime
      val toMillis   = _creationMessage._range.to.getTime
      val url = " http://%s/user/%s/bill/%d/%d".format(serverAndPort,uid,fromMillis,toMillis)
      try{
        val in = new BufferedReader(
          new InputStreamReader(
            new URL(url).openConnection().
              getInputStream()))
        var inputLine = ""
        var ret = ""
        while ({inputLine = in.readLine();inputLine} != null)
          ret += (if(ret.isEmpty) "" else "\n")+ inputLine
        in.close()
        ret
      } catch {
        case e:Exception =>
          ""
      }
    }
    var resp = ""
    var count = 0
    while(resp.isEmpty && count < max){
      if(count > 0) Console.err.println("Retrying for bill request.")
      resp = get()
      if(resp.isEmpty) Thread.sleep(1000)
      //sleep(1000L)
      count += 1
    }
    resp
  }
}

object UserTest extends Loggable {

 val aquarium  = AquariumInstance.aquarium

 def main(args: Array[String]) = {
    val user = new User("localhost:8888",9)
    val (minFileCredits,maxFileCredits) = (2000,5000)
    val (minUserCredits,maxUserCredits) = (10000,10000)
    //Cron spec  minutes hours day-of-month Month Day-of-Week (we do not specify seconds)

   val json =AquariumInstance.run {
          user.
                    addCredits(10000,"00 00 ? 9 Sat").
                    addFiles(1,"update",2000,1000,3000,"00 18 ? 9 Tue").
                    //addVMs(1,"on","00 18 ? 9 Mon").
                    //addVMs(5,"on","00 18 ? 9 Tue")
                    run(minFileCredits,maxFileCredits,minUserCredits,maxUserCredits)
   }
   Thread.sleep(2000)
   Console.err.println("Response:\n" + json)
 }

}



object BillTest extends Loggable {

  type JSON = String
  type UID  = Long
  type DATE = String

  private[this] val counter = new AtomicLong(0L)
  private[this] def nextID() = counter.getAndIncrement

  private [this] val format = new SimpleDateFormat("HH/mm/s/dd/MM/yyyy");

  val propsfile = new FileStreamResource(new File("aquarium.properties"))

  var props: Props = Props(propsfile)(StdConverters.AllConverters).getOr(Props()(StdConverters.AllConverters))

  val (astakosExchangeName,astakosRoutingKey) = ("astakos","astakos.user")

  val (pithosExchangeName,pithosRoutingKey) = ("pithos","pithos.resource.diskspace")

  val aquarium = {
      exec("mongo aquarium --eval db.resevents.remove();db.imevents.remove();db.policies.remove();db.userstates.remove()",
           Console.err.println(_))
      new AquariumBuilder(props, ResourceLocator.DefaultPolicyModel).
      //update(Aquarium.EnvKeys.storeProvider, new MemStoreProvider).
      update(Aquarium.EnvKeys.eventsStoreFolder,Some(new File(".."))).
      build()
  }


  private[this] def exec(cmd : String,func : String=>Unit) : Unit = {
    val commands = cmd.split(" ")
    val proc = new ProcessBuilder(commands: _*).redirectErrorStream(true).start();
    val ins = new java.io.BufferedReader(new java.io.InputStreamReader(proc.getInputStream))
    val sb = new StringBuilder

    //spin off a thread to read process output.
    val outputReaderThread = new Thread(new Runnable(){
      def run : Unit = {
        var ln : String = null
        while({ln = ins.readLine; ln != null})
          func(ln)
      }
    })
    outputReaderThread.start()

    //suspense this main thread until sub process is done.
    proc.waitFor

    //wait until output is fully read/completed.
    outputReaderThread.join()

    ins.close()
  }


  private [this] def createUser(date:DATE) : (JSON,UID) = {
    val mid = nextID
    val id = "im.%d.create.user".format(mid)
    val millis = format.parse(date).getTime
    val occurredMillis = millis
    val receivedMillis = millis
    val userID = "user%d@grnet.gr".format(mid)
    val clientID = "astakos"
    val isActive = false
    val role = "default"
    val eventVersion = "1.0"
    val eventType = "create"
    (new StdIMEvent(id,occurredMillis,receivedMillis,userID,
                   clientID,isActive,role,eventVersion,eventType,
                   Map()).toJsonString,mid)
  }

  private [this] def addCredits(date:DATE,uid:UID,amount:Long) : JSON = {
    val id = "im.%d.add.credits".format(nextID)
    val millis = format.parse(date).getTime
    val occurredMillis = millis
    val receivedMillis = millis
    val userID = "user%d@grnet.gr".format(uid)
    val clientID = "astakos"
    val isActive = false
    val role = "default"
    val eventVersion = "1.0"
    val eventType = "addcredits"
    new StdIMEvent(id,occurredMillis,receivedMillis,userID,
                   clientID,isActive,role,eventVersion,eventType,
                   Map("credits" -> amount.toString)).toJsonString
  }

  private [this] def makePithos(date:DATE,uid:UID,path:String,
                                value:Double,action:String) : JSON = {
    val id = "rc.%d.object.%s".format(nextID,action)
    val millis = format.parse(date).getTime
    val occurredMillis = millis
    val receivedMillis = millis
    val userID = "user%d@grnet.gr".format(uid)
    val clientID = "pithos"
    val resource ="diskspace"
    val instanceID = "1"
    val eventVersion = "1.0"
    val details = Map("action" -> "object %s".format(action),
                      "total"  -> "0.0",
                      "user"   -> userID,
                      "path"   -> path)
    new StdResourceEvent(id,occurredMillis,receivedMillis,userID,clientID,
                         resource,instanceID,value,eventVersion,details).toJsonString
  }

  private[this] def sendCreate(date:DATE) : UID = {
    val (json,uid) = createUser(date)
    aquarium(Aquarium.EnvKeys.rabbitMQProducer).
    sendMessage(astakosExchangeName,astakosRoutingKey,json)
    Console.err.println("Sent message:\n%s\n".format(json))
    uid
  }

  private[this] def sendAddCredits(date:DATE,uid:UID,amount:Long) = {
    val json = addCredits(date,uid,amount)
    aquarium(Aquarium.EnvKeys.rabbitMQProducer).
    sendMessage(astakosExchangeName,astakosRoutingKey,
                json)
    Console.err.println("Sent message:\n%s\n".format(json))
  }

  private[this] def sendPithos(date:DATE,uid:UID,path:String,
                               value:Double,action:String) = {
    val json = makePithos(date,uid,path,value,action)
    aquarium(Aquarium.EnvKeys.rabbitMQProducer).
    sendMessage(pithosExchangeName,pithosRoutingKey,
                json)
    Console.err.println("Sent message:\n%s\n".format(json))
  }

  private[this] def jsonOf(url:String) : JSON = {
     val in = new BufferedReader(
                         new InputStreamReader(
                         new URL(url).openConnection().
                         getInputStream()))
      var inputLine = ""
      var ret = ""
      while ({inputLine = in.readLine();inputLine} != null)
        ret += (if(ret.isEmpty) "" else "\n")+ inputLine
      in.close()
      ret
  }

  private[this] def getBill(uid:Long,from:String,to:String) : JSON = {
    val fromMillis = format.parse(from).getTime
    val toMillis   = format.parse(to).getTime
    val billURL = " http://localhost:8888/user/user%d@grnet.gr/bill/%d/%d".format(uid,fromMillis,toMillis)
    try{
      jsonOf(billURL)
    } catch {
      case e:Exception =>
        ""
    }
  }

  private[this] def sleep(l:Long) = {
  try {
      Thread.sleep(l)
    } catch {
      case ex:InterruptedException =>
        Thread.currentThread().interrupt()
    }
  }


  private[this] def testCase1() : JSON  = {
    /* GET BILL FROM TO*/
    val billFromDate = "00/00/00/01/08/2012"
    val billToDate= "23/59/59/31/08/2012"
    /* USER Creation */
    val creationDate = "15/00/00/03/08/2012"
    /* ADD CREDITS */
    val addCreditsDate = "18/15/00/05/08/2012"
    val creditsToAdd = 6000
    /* Pithos STUFF */
    val pithosPath = "/Papers/GOTO_HARMFUL.PDF"

    val pithosDate1 = "20/30/00/05/08/2012"
    val pithosAction1 = "update"
    val pithosValue1 = 2000


    val pithosDate2 = "21/05/00/15/08/2012"
    val pithosAction2 = "update"
    val pithosValue2 = 4000


    val pithosDate3 = "08/05/00/20/08/2012"
    val pithosAction3 = "update"
    val pithosValue3 = 100

    val id =
      sendCreate(creationDate)
      //Thread.sleep(5000)
      sendAddCredits(addCreditsDate,id,creditsToAdd)
      //Thread.sleep(5000)
      sendPithos(pithosDate1,id,pithosPath,pithosValue1,pithosAction1)
      //Thread.sleep(5000)
      sendPithos(pithosDate2,id,pithosPath,pithosValue2,pithosAction2)
      //
      sendPithos(pithosDate3,id,pithosPath,pithosValue3,pithosAction3)


    Console.err.println("Waiting for stuff to be processed")
    Thread.sleep(5000)

    var resp = ""
    var count = 0
    while(resp.isEmpty && count < 5){
      if(count > 0) Console.err.println("Retrying for bill request.")
      resp = getBill(id,billFromDate,billToDate)
      if(resp.isEmpty) Thread.sleep(1000)
      //sleep(1000L)
      count += 1
    }
    Console.err.println("Sending URL done")
    resp
  }

  def runTestCase(f: => JSON) = {
    var json = ""
    aquarium.start
    Thread.sleep(2000)
    try{
      json = f
    }  catch{
      case e:Exception =>
        e.printStackTrace
    }
    aquarium.stop
    Thread.sleep(1000)
    Console.err.println("Response : " + json )
  }

  def main(args: Array[String]) = {
    //Console.err.println("JSON: " +  (new BillEntry).toJsonString)
    runTestCase(testCase1)
  }
}
