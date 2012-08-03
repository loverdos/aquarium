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

class EventEntry(id:String,
                 eventType:String,
                 unitPrice:String,
                 startTime:String,
                 endTime:String,
                 ellapsedTime:String,
                 credits:String) extends JsonSupport {

}

class ResourceEntry(val resourceName : String,
                    val resourceType : String,
                    val unitName : String,
                    val totalCredits : String,
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
  def fromWorkingUserState(t:Timeslot,w:Option[WorkingUserState]) : BillEntry = {
    //TODO: get entries at timeslot "t"
    val eventEntry = new EventEntry("1234","onOff","0.1","323232323","3232223456","10000","5.00")
    val resourceEntry = new ResourceEntry("VM_1","vmtime","0.01","5.0",List(eventEntry))
    new BillEntry("323232","loverdos@grnet.gr","ok","100.00","5.00","23023020302","23232323",
                  List(resourceEntry))
  }

  //
  def main(args: Array[String]) = {
    //Console.err.println("JSON: " +  (new BillEntry).toJsonString)
    val propsfile = new FileStreamResource(new File("a1.properties"))
    var _props: Props = Props(propsfile)(StdConverters.AllConverters).getOr(Props()(StdConverters.AllConverters))
    val aquarium = new AquariumBuilder(_props, ResourceLocator.DefaultPolicyModel).
      update(Aquarium.EnvKeys.storeProvider, new MemStoreProvider).
      update(Aquarium.EnvKeys.eventsStoreFolder,Some(new File(".."))).
      build()
    aquarium.start()
    ()
  }
}