package gr.grnet.aquarium

import com.ckkloverdos.resource.FileStreamResource
import converter.StdConverters
import event.model.im.StdIMEvent
import event.model.resource.StdResourceEvent
import java.io.{InputStreamReader, BufferedReader, File}
import com.ckkloverdos.props.Props
import store.memory.MemStoreProvider
import java.util.concurrent.atomic.AtomicLong
import java.text.SimpleDateFormat
import java.net.{URLConnection, URL}
import util.Loggable

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
