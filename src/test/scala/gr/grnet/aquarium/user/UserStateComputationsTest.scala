package gr.grnet.aquarium.user

import org.junit.Test
import gr.grnet.aquarium.Configurator
import gr.grnet.aquarium.store.memory.MemStore
import gr.grnet.aquarium.util.date.DateCalculator
import simulation.{ClientServiceSim, UserSim}
import gr.grnet.aquarium.logic.accounting.dsl._
import java.util.Date
import gr.grnet.aquarium.logic.accounting.Accounting
import com.ckkloverdos.maybe.NoVal


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

    val START_OF_YEAR_DATECALC = new DateCalculator(START_YEAR, 1, 1)

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

    val userPolicyFinder = new UserPolicyFinder {
      def findUserPolicyAt(userId: String, whenMillis: Long) = DEFAULT_POLICY
    }

    val fullStateFinder = new FullStateFinder {
      def findFullState(userId: String, whenMillis: Long) = null
    }

    val userStateCache = new UserStateCache {
      def findUserStateAtEndOfPeriod(userId: String, year: Int, month: Int) = NoVal

      def findLatestUserStateForBillingMonth(userId: String, yearOfBillingMonth: Int, billingMonth: Int) = NoVal
    }

    val mc = Configurator.MasterConfigurator.withStoreProviderClass(classOf[MemStore])
    val storeProvider = mc.storeProvider
    val resourceEventStore = storeProvider.resourceEventStore
//    println("!! storeProvider = %s".format(storeProvider))

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
    val vmStartDateCalc = USER_START_DATECALC.goPlusDays(1).goPlusHours(1)
    val vmStartDate = vmStartDateCalc.toDate

    // Within January, create one VM ON-OFF ...
    val onOff1_M = vm.newONOFF(vmStartDate, 9)

    val diskConsumptionDateCalc = USER_START_DATECALC.goPlusHours(3)
    val diskConsumptionDate1 = diskConsumptionDateCalc.toDate
    val diskConsumptionDate2 = diskConsumptionDateCalc.goPlusDays(1).goPlusHours(1).toDate

    // ... and two diskspace changes
    val consume1_M = disk.consumeMB(diskConsumptionDate1, 99)
    val consume2_M = disk.consumeMB(diskConsumptionDate2, 23)

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

    val billing = computer.computeFullMonthlyBilling(
      START_YEAR,
      START_MONTH,
      christos.userId,
      userPolicyFinder,
      fullStateFinder,
      userStateCache,
      resourceEventStore,
      computer.createFirstUserState(christos.userId),
      Nil,
      DEFAULT_POLICY,
      DEFAULT_RESOURCES_MAP,
      new Accounting{}
    )
    
    println("!! billing = %s".format(billing))
  }
}