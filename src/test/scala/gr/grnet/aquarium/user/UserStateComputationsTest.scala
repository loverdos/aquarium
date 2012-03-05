package gr.grnet.aquarium.user

import org.junit.Test
import gr.grnet.aquarium.Configurator
import gr.grnet.aquarium.store.memory.MemStore
import gr.grnet.aquarium.util.date.MutableDateCalc
import gr.grnet.aquarium.logic.accounting.dsl._
import java.util.Date
import simulation.{ConcurrentVMLocalUIDGenerator, ClientServiceSim, UserSim}
import gr.grnet.aquarium.logic.accounting.{Policy, Accounting}
import gr.grnet.aquarium.util.{Loggable, ContextualLogger}
import com.ckkloverdos.maybe.{Just, NoVal}


/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
class UserStateComputationsTest extends Loggable {
  val PolicyYAML = """
aquariumpolicy:
  resources:
    - resource:
      name: bandwidth
      unit: MB/hr
      complex: false
      costpolicy: discrete
    - resource:
      name: vmtime
      unit: Hour
      complex: true
      costpolicy: onoff
      descriminatorfield: vmid
    - resource:
      name: diskspace
      unit: MB/hr
      complex: false
      costpolicy: continuous

  implicitvars:
    - price
    - volume

  algorithms:
    - algorithm:
      name: default
      bandwidth: $NotNow
      vmtime: $NotNow
      diskspace: $NotNow
      effective:
        from: 0

  pricelists:
    - pricelist:
      name: default
      bandwidth: 1.0
      vmtime: 1.0
      diskspace: 1.0
      effective:
        from: 0

  creditplans:
    - creditplan:
      name: default
      credits: 100
      at: "00 00 1 * *"
      effective:
        from: 0

  agreements:
    - agreement:
      name: default
      algorithm: default
      pricelist: default
      creditplan: default
  """

  val DefaultPolicy = new DSL{}.parse(PolicyYAML)

  // TODO: integrate this with the rest of the simulation stuff
  // TODO: since, right now, the resource strings have to be given twice
  val VMTimeResource    = DSLComplexResource("vmtime",    "Hr",    OnOffCostPolicy,      "")
  val DiskspaceResource = DSLComplexResource("diskspace", "MB/Hr", ContinuousCostPolicy, "")
  val BandwidthResource = DSLComplexResource("bandwidth", "MB/Hr", DiscreteCostPolicy,   "")
  val DefaultResourcesMap = new DSLResourcesMap(VMTimeResource :: DiskspaceResource :: BandwidthResource :: Nil)

  // There are two client services, synnefo and pithos.
  val TheUIDGenerator = new ConcurrentVMLocalUIDGenerator
  val Synnefo = ClientServiceSim("synnefo")(TheUIDGenerator)
  val Pithos  = ClientServiceSim("pithos")(TheUIDGenerator)

  @Test
  def testOne: Unit = {
    val clog = ContextualLogger.fromOther(NoVal, logger, "testOne()")
    val StartOfBillingYearDateCalc = new MutableDateCalc(2012, 1, 1)
//    println("StartOfBillingYearDateCalc = %s".format(StartOfBillingYearDateCalc))
    val UserCreationDateCalc = StartOfBillingYearDateCalc.copy.goMinusMonths(2)
//    println("UserCreationDateCalc = %s".format(UserCreationDateCalc))

    val computer = new UserStateComputations

    val mc = Configurator.MasterConfigurator.withStoreProviderClass(classOf[MemStore])
    Policy.withConfigurator(mc)

    val storeProvider = mc.storeProvider
    val userStateStore = storeProvider.userStateStore
    val resourceEventStore = storeProvider.resourceEventStore
    val policyStore = storeProvider.policyStore

    val policyOccurredMillis  = StartOfBillingYearDateCalc.toMillis
    val policyValidFromMillis = StartOfBillingYearDateCalc.copy.goPreviousYear.toMillis
    val policyValidToMillis   = StartOfBillingYearDateCalc.copy.goNextYear.toMillis
    policyStore.storePolicyEntry(DefaultPolicy.toPolicyEntry(policyOccurredMillis, policyValidFromMillis, policyValidToMillis))

    // A new user is created on 2012-01-15 00:00:00.000
    val UserCKKL  = UserSim("CKKL", UserCreationDateCalc.toDate, storeProvider.resourceEventStore)

    // By convention
    // - synnefo is for VMTime and Bandwidth
    // - pithos is for Diskspace
    val VMTimeInstance    = Synnefo.newVMTime   (UserCKKL, "VM.1")
    val BandwidthInstance = Synnefo.newBandwidth(UserCKKL, "3G.1")
    val DiskInstance      = Pithos .newDiskspace(UserCKKL, "DISK.1")

    // Let's create our dates of interest
    val vmStartDateCalc = StartOfBillingYearDateCalc.copy.goPlusDays(1).goPlusHours(1)
//    println("vmStartDateCalc = %s".format(vmStartDateCalc))
    // 2012-01-16 01:00:00.000
    val vmStartDate = vmStartDateCalc.toDate

    // Within January, create one VM ON-OFF ...
    val onOff1_M = VMTimeInstance.newONOFF(vmStartDate, 9)

    val diskConsumptionDateCalc = StartOfBillingYearDateCalc.copy.goPlusHours(3)
    // 2012-01-16 04:00:00.000
    val diskConsumptionDate1 = diskConsumptionDateCalc.toDate
    // 2012-01-17 05:00:00.000
    val diskConsumptionDateCalc2 = diskConsumptionDateCalc.copy.goPlusDays(1).goPlusHours(1)
    val diskConsumptionDate2 = diskConsumptionDateCalc2.toDate

    // ... and two diskspace changes
    val consume1_M = DiskInstance.consumeMB(diskConsumptionDate1, 99)
    val consume2_M = DiskInstance.consumeMB(diskConsumptionDate2, 23)

    // 100MB 3G bandwidth
    val bwDateCalc = diskConsumptionDateCalc2.copy.goPlusDays(1)
    BandwidthInstance.useBandwidth(bwDateCalc.toDate, 100.0)

    // ... and one "future" event
    // 2012-02-07 07:07:07.007
    DiskInstance.consumeMB(
      StartOfBillingYearDateCalc.copy.
        goNextMonth.goPlusDays(6).
        goPlusHours(7).
        goPlusMinutes(7).
        goPlusSeconds(7).
        goPlusMillis(7).toDate,
      777)

    clog.debug("")
    clog.debug("=== Events by OccurredMillis ===")
    clog.withIndent {
      for(event <- UserCKKL.myResourceEventsByOccurredDate) {
        clog.debug(event.toDebugString(DefaultResourcesMap))
      }
    }
    clog.debug("=== Events by OccurredMillis ===")
    clog.debug("")

    val billingMonthInfo = BillingMonthInfo.fromDateCalc(StartOfBillingYearDateCalc)

    val initialUserState = computer.createFirstUserState(
      userId = UserCKKL.userId,
      millis = StartOfBillingYearDateCalc.copy.goPreviousYear.toMillis
    )

    val userStateM = computer.doFullMonthlyBilling(
      UserCKKL.userId,
      billingMonthInfo,
      userStateStore,
      resourceEventStore,
      policyStore,
      UserCKKL.userCreationDate.getTime,
      initialUserState,
      initialUserState,
      DefaultPolicy,
      DefaultResourcesMap,
      new Accounting{},
      Just(clog)
    )
    
    clog.debug("userStateM = %s".format(userStateM))
    userStateM.forFailed { failed ⇒
      clog.error(failed)
      failed.exception.printStackTrace()
      NoVal
    }
  }
}