package gr.grnet.aquarium.user

import org.junit.Test
import gr.grnet.aquarium.Configurator
import gr.grnet.aquarium.store.memory.MemStore
import gr.grnet.aquarium.util.date.MutableDateCalc
import gr.grnet.aquarium.logic.accounting.dsl._
import gr.grnet.aquarium.logic.accounting.{Policy, Accounting}
import gr.grnet.aquarium.util.{Loggable, ContextualLogger}
import com.ckkloverdos.maybe.{Just, NoVal}
import gr.grnet.aquarium.simulation._
import gr.grnet.aquarium.simulation.uid.{UIDGenerator, ConcurrentVMLocalUIDGenerator}


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
  val VMTimeResource    = StdVMTimeResourceSim
  val DiskspaceResource = StdDiskspaceResourceSim
  val BandwidthResource = StdBandwidthResourceSim

  // There are two client services, synnefo and pithos.
  val TheUIDGenerator: UIDGenerator = new ConcurrentVMLocalUIDGenerator
  val Synnefo = ClientSim("synnefo")(TheUIDGenerator)
  val Pithos  = ClientSim("pithos" )(TheUIDGenerator)

  @Test
  def testOne: Unit = {
    val clog = ContextualLogger.fromOther(NoVal, logger, "testOne()")
    val StartOfBillingYearDateCalc = new MutableDateCalc(2012, 1, 1)
    val UserCreationDateCalc = StartOfBillingYearDateCalc.copy.goMinusMonths(2)

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

    val Aquarium = AquariumSim(List(VMTimeResource, DiskspaceResource, BandwidthResource), storeProvider.resourceEventStore)
    val DefaultResourcesMap = Aquarium.resourcesMap

    // A new user is created on 2012-01-15 00:00:00.000
    val UserCKKL  = Aquarium.newUser("CKKL", UserCreationDateCalc.toDate)

    // By convention
    // - synnefo is for VMTime and Bandwidth
    // - pithos is for Diskspace
    val VMTimeInstance    = StdVMTimeInstanceSim   ("VM.1",   UserCKKL, Synnefo)
    val BandwidthInstance = StdBandwidthInstanceSim("3G.1",   UserCKKL, Synnefo)
    val DiskInstance      = StdDiskspaceInstanceSim("DISK.1", UserCKKL, Pithos)

    // Let's create our dates of interest
    val vmStartDateCalc = StartOfBillingYearDateCalc.copy.goPlusDays(1).goPlusHours(1)
    // 2012-01-16 01:00:00.000
    val vmStartDate = vmStartDateCalc.toDate

    // Within January, create one VM ON-OFF ...
    VMTimeInstance.newONOFF(vmStartDate, 9)

    val diskConsumptionDateCalc = StartOfBillingYearDateCalc.copy.goPlusHours(3)
    // 2012-01-16 04:00:00.000
    val diskConsumptionDate1 = diskConsumptionDateCalc.toDate
    // 2012-01-17 05:00:00.000
    val diskConsumptionDateCalc2 = diskConsumptionDateCalc.copy.goPlusDays(1).goPlusHours(1)
    val diskConsumptionDate2 = diskConsumptionDateCalc2.toDate

    // ... and two diskspace changes
    DiskInstance.consumeMB(diskConsumptionDate1, 99)
    DiskInstance.consumeMB(diskConsumptionDate2, 23)

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