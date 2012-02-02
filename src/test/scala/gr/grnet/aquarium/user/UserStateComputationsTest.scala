package gr.grnet.aquarium.user

import org.junit.Test
import gr.grnet.aquarium.Configurator
import gr.grnet.aquarium.store.memory.MemStore
import gr.grnet.aquarium.util.date.DateCalculator
import simulation.{ClientServiceSim, UserSim}


/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
class UserStateComputationsTest {
  @Test
  def testOne: Unit = {
    val START_YEAR = 2012
    val START_MONTH = 1
    val START_DAY = 15

    val mc = Configurator.MasterConfigurator.withStoreProviderClass(classOf[MemStore])
    val storeProvider = mc.storeProvider
    println("!! storeProvider = %s".format(storeProvider))

    // A new user is created on January 15th, 2012
    val USER_START_DATECALC = new DateCalculator(START_YEAR, START_MONTH, START_DAY)
    val christos  = UserSim("Christos", USER_START_DATECALC.toDate, storeProvider.resourceEventStore)

    // There are two client services, synnefo and pithos.
    val synnefo = ClientServiceSim("synnefo")
    val pithos  = ClientServiceSim("pithos")

    // By convention
    // - synnefo is for VMTime and
    // - pithos is for Diskspace
    val vm   = synnefo.newVMTime(christos, "VM.1")
    val disk = pithos.newDiskspace(christos, "DISK.1")

    // Let's create our dates of interest
    val vmStartDateCalc = USER_START_DATECALC.plusDays(1).plusHours(1)
    val vmStartDate = vmStartDateCalc.toDate

    // Within January, create one VM ON-OFF
    val onOff1_M = vm.newONOFF(vmStartDate, 9)

  }
}