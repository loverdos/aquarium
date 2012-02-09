package gr.grnet.aquarium.user

import org.junit.Test
import gr.grnet.aquarium.Configurator
import gr.grnet.aquarium.store.memory.MemStore
import gr.grnet.aquarium.util.date.DateCalculator
import gr.grnet.aquarium.logic.accounting.dsl._
import java.util.Date
import gr.grnet.aquarium.logic.accounting.Accounting
import com.ckkloverdos.maybe.NoVal
import simulation.{ConcurrentVMLocalUIDGenerator, ClientServiceSim, UserSim}


/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
class UserStateComputationsTest {
  @Test
  def testOne: Unit = {
    val StartOfBillingYearDateCalc = new DateCalculator(2012, 1, 1)
//    println("StartOfBillingYearDateCalc = %s".format(StartOfBillingYearDateCalc))
    val UserCreationDateCalc = StartOfBillingYearDateCalc.copy.goMinusMonths(2)
//    println("UserCreationDateCalc = %s".format(UserCreationDateCalc))

    // TODO: integrate this with the rest of the simulation stuff
    // TODO: since, right now, the resource strings have to be given twice
    val VMTIME_RESOURCE    = DSLComplexResource("vmtime",    "Hr",    OnOffCostPolicy,      "")
    val DISKSPACE_RESOURCE = DSLComplexResource("diskspace", "MB/Hr", ContinuousCostPolicy, "")

    val DEFAULT_RESOURCES_MAP = new DSLResourcesMap(VMTIME_RESOURCE :: DISKSPACE_RESOURCE :: Nil)

    val FOR_EVER = DSLTimeFrame(new Date(0), None, Nil)
    val THE_PRICES_MAP: Map[DSLResource, Double] = Map(VMTIME_RESOURCE -> 1.0, DISKSPACE_RESOURCE -> 1.0)

    val THE_PRICE_LIST  = DSLPriceList("default", None, THE_PRICES_MAP, FOR_EVER)
    val THE_CREDIT_PLAN = DSLCreditPlan("default", None, 100, Nil, "", FOR_EVER)
    val THE_ALGORITHM   = DSLAlgorithm("default", None, Map(), FOR_EVER)
    val THE_AGREEMENT   = DSLAgreement("default", None, THE_ALGORITHM, THE_PRICE_LIST, THE_CREDIT_PLAN)

    val THE_RESOURCES = DEFAULT_RESOURCES_MAP.toResourcesList

    val DEFAULT_POLICY = DSLPolicy(
      Nil,
      THE_PRICE_LIST :: Nil,
      THE_RESOURCES,
      THE_CREDIT_PLAN :: Nil,
      THE_AGREEMENT :: Nil
    )

    val computer = new UserStateComputations

    val mc = Configurator.MasterConfigurator.withStoreProviderClass(classOf[MemStore])
    val storeProvider = mc.storeProvider
    val userStateStore = storeProvider.userStateStore
    val resourceEventStore = storeProvider.resourceEventStore
    val policyStore = storeProvider.policyStore

    // A new user is created on 2012-01-15 00:00:00.000
    val christos  = UserSim("Christos", UserCreationDateCalc.toDate, storeProvider.resourceEventStore)

    // There are two client services, synnefo and pithos.
    val uidGenerator = new ConcurrentVMLocalUIDGenerator
    val synnefo = ClientServiceSim("synnefo")(uidGenerator)
    val pithos  = ClientServiceSim("pithos")(uidGenerator)

    // By convention
    // - synnefo is for VMTime and
    // - pithos is for Diskspace
    val vm   = synnefo.newVMTime(christos, "VM.1")
    val disk = pithos.newDiskspace(christos, "DISK.1")

    // Let's create our dates of interest
    val vmStartDateCalc = StartOfBillingYearDateCalc.copy.goPlusDays(1).goPlusHours(1)
//    println("vmStartDateCalc = %s".format(vmStartDateCalc))
    // 2012-01-16 01:00:00.000
    val vmStartDate = vmStartDateCalc.toDate

    // Within January, create one VM ON-OFF ...
    val onOff1_M = vm.newONOFF(vmStartDate, 9)

    val diskConsumptionDateCalc = StartOfBillingYearDateCalc.copy.goPlusHours(3)
    // 2012-01-16 04:00:00.000
    val diskConsumptionDate1 = diskConsumptionDateCalc.toDate
    // 2012-01-17 05:00:00.000
    val diskConsumptionDate2 = diskConsumptionDateCalc.copy.goPlusDays(1).goPlusHours(1).toDate

    // ... and two diskspace changes
    val consume1_M = disk.consumeMB(diskConsumptionDate1, 99)
    val consume2_M = disk.consumeMB(diskConsumptionDate2, 23)

    // ... and one "future" event
    // 2012-02-07 07:07:07.007
    disk.consumeMB(
      StartOfBillingYearDateCalc.copy.
        goNextMonth.goPlusDays(6).
        goPlusHours(7).
        goPlusMinutes(7).
        goPlusSeconds(7).
        goPlusMillis(7).toDate,
      777)

    println("=============================")
    for {
      event <- christos.myResourceEventsByReceivedDate
    } {
      val when = new DateCalculator(event.receivedMillis)
      val resource  = event.resource
      val instanceId = event.instanceId
      val value = event.beautifyValue(DEFAULT_RESOURCES_MAP)
      println("%s [%s, %s] %s".format(when, resource, instanceId, value))
    }
    println("=============================")

    val userStateM = computer.doFullMonthlyBilling(
      christos.userId,
      StartOfBillingYearDateCalc.getYear,
      StartOfBillingYearDateCalc.getMonthOfYear,
      userStateStore,
      resourceEventStore,
      policyStore,
      christos.userCreationDate.getTime,
      computer.createFirstUserState(christos.userId),
      computer.createFirstUserState(christos.userId),
      DEFAULT_POLICY,
      DEFAULT_RESOURCES_MAP,
      new Accounting{}
    )
    
    println("!! userStateM = %s".format(userStateM))
    userStateM.forFailed { failed ⇒
      failed.exception.printStackTrace()
      NoVal
    }
  }
}