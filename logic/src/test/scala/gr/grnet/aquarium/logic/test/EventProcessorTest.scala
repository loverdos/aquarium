package gr.grnet.aquarium.logic.test

import java.util.Date
import gr.grnet.aquarium.logic.events._
import org.junit.Test
import org.junit.Assert._

class EventProcessorTest {

   def getEvents(from: Option[Date], to: Option[Date]): List[Event] = {
    //Tmp list of events
    List[Event](
      new VMCreated(1, new Date(123), 2, 1),
      new VMStarted(2, new Date(123), 2, 1),
      new VMCreated(3, new Date(125), 2, 2),
      new DiskSpaceChanged(4, new Date(122), 2, 1554),
      new DataUploaded(5, new Date(122), 2, 1554),
      new DiskSpaceChanged(6, new Date(122), 1, 1524),
      new DataUploaded(7, new Date(122), 1, 1524),
      new DiskSpaceChanged(8, new Date(122), 1, 1332)
    )
  }
  
  @Test
  def testProcess() = {
    var result = EventProcessor.process(None, None, getEvents)
    assert(result.size == 6)
  }
}