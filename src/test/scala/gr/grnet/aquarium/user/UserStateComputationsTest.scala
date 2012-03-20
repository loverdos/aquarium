package gr.grnet.aquarium.user

import gr.grnet.aquarium.Configurator
import gr.grnet.aquarium.store.memory.MemStore
import gr.grnet.aquarium.util.date.MutableDateCalc
import gr.grnet.aquarium.logic.accounting.dsl._
import gr.grnet.aquarium.logic.accounting.{Policy, Accounting}
import gr.grnet.aquarium.util.{Loggable, ContextualLogger, justForSure}
import gr.grnet.aquarium.simulation._
import gr.grnet.aquarium.simulation.uid.{UIDGenerator, ConcurrentVMLocalUIDGenerator}
import com.ckkloverdos.maybe.{Maybe, Failed, Just, NoVal}
import org.junit.{Ignore, Test}
import gr.grnet.aquarium.logic.accounting.algorithm.{SimpleExecutableCostPolicyAlgorithm, SimpleCostPolicyAlgorithmCompiler}


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
      unit: MB/Hr
      complex: false
      costpolicy: discrete
    - resource:
      name: vmtime
      unit: Hr
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
      bandwidth: function bandwidth() {return 1;}
      vmtime: function vmtime() {return 1;}
      diskspace: function diskspace() {return 1;}
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

  val Computations = new UserStateComputations

  val DefaultPolicy = new DSL{} parse PolicyYAML
  val DefaultAccounting = new Accounting{}
  val DefaultCompiler  = SimpleCostPolicyAlgorithmCompiler
  val DefaultAlgorithm = justForSure(DefaultCompiler.compile("")).get // hardcoded since we know exactly what this is

  // For this to work, the definitions must match those in the YAML above.
  // Those StdXXXResourceSim are just for debugging convenience anyway, so they must match by design.
  val VMTimeResource    = StdVMTimeResourceSim.fromPolicy(DefaultPolicy)
  val DiskspaceResource = StdDiskspaceResourceSim.fromPolicy(DefaultPolicy)
  val BandwidthResource = StdBandwidthResourceSim.fromPolicy(DefaultPolicy)

  // There are two client services, synnefo and pithos.
  val TheUIDGenerator: UIDGenerator = new ConcurrentVMLocalUIDGenerator
  val Synnefo = ClientSim("synnefo")(TheUIDGenerator)
  val Pithos  = ClientSim("pithos" )(TheUIDGenerator)

  val mc = Configurator.MasterConfigurator.withStoreProviderClass(classOf[MemStore])
  Policy.withConfigurator(mc)
  val StoreProvider = mc.storeProvider
  val ResourceEventStore = StoreProvider.resourceEventStore

  val StartOfBillingYearDateCalc = new MutableDateCalc(2012,  1, 1)
  val UserCreationDate           = new MutableDateCalc(2011, 11, 1).toDate

  val BillingMonthInfoJan = BillingMonthInfo.fromDateCalc(new MutableDateCalc(2012,  1, 1))
  val BillingMonthInfoFeb = BillingMonthInfo.fromDateCalc(new MutableDateCalc(2012,  2, 1))
  val BillingMonthInfoMar = BillingMonthInfo.fromDateCalc(new MutableDateCalc(2012,  3, 1))

  // Store the default policy
  val policyDateCalc        = StartOfBillingYearDateCalc.copy
  val policyOccurredMillis  = policyDateCalc.toMillis
  val policyValidFromMillis = policyDateCalc.copy.goPreviousYear.toMillis
  val policyValidToMillis   = policyDateCalc.copy.goNextYear.toMillis
  StoreProvider.policyStore.storePolicyEntry(DefaultPolicy.toPolicyEntry(policyOccurredMillis, policyValidFromMillis, policyValidToMillis))

  val Aquarium = AquariumSim(List(VMTimeResource, DiskspaceResource, BandwidthResource), StoreProvider.resourceEventStore)
  val DefaultResourcesMap = Aquarium.resourcesMap

  val UserCKKL  = Aquarium.newUser("CKKL", UserCreationDate)

  val InitialUserState = Computations.createInitialUserState(
    userId = UserCKKL.userId,
    userCreationMillis = UserCreationDate.getTime,
    isActive = true,
    credits = 0.0,
    roleNames = List("user"),
    agreementName = DSLAgreement.DefaultAgreementName
  )

  // By convention
  // - synnefo is for VMTime and Bandwidth
  // - pithos is for Diskspace
  val VMTimeInstance    = VMTimeResource.newInstance   ("VM.1",   UserCKKL, Synnefo)
  val BandwidthInstance = BandwidthResource.newInstance("3G.1",   UserCKKL, Synnefo)
  val DiskInstance      = DiskspaceResource.newInstance("DISK.1", UserCKKL, Pithos)

  private[this]
  def showUserState(clog: ContextualLogger, userStateM: Maybe[UserState]) {
    userStateM match {
      case Just(userState) ⇒
        val _id = userState._id
        val parentId = userState.parentUserStateId
        val credits = userState.creditsSnapshot.creditAmount
        val newWalletEntries = userState.newWalletEntries
        val changeReasonCode = userState.lastChangeReasonCode
        val changeReason = userState.lastChangeReason
        userState.implicitlyIssuedSnapshot

        clog.indent()
        clog.debug("_id = %s", _id)
        clog.debug("parentId = %s", parentId)
        clog.debug("credits = %s", credits)
        clog.debug("changeReasonCode = %s", changeReasonCode)
        clog.debug("changeReason = %s", changeReason)
        clog.debugSeq("newWalletEntries", newWalletEntries.map(_.toDebugString), 0)
        clog.unindent()

      case NoVal ⇒

      case failed@Failed(_, _) ⇒
        clog.error(failed)
        failed.exception.printStackTrace()
    }
  }

  private[this]
  def showResourceEvents(clog: ContextualLogger): Unit = {
    clog.debug("")
    clog.begin("Events by OccurredMillis")
    clog.withIndent {
      for(event <- UserCKKL.myResourceEventsByOccurredDate) {
        clog.debug(event.toDebugString())
      }
    }
    clog.end("Events by OccurredMillis")
    clog.debug("")
  }

  private[this]
  def doFullMonthlyBilling(clog: ContextualLogger, billingMonthInfo: BillingMonthInfo) = {
    Computations.doFullMonthlyBilling(
      UserCKKL.userId,
      billingMonthInfo,
      StoreProvider,
      InitialUserState,
      DefaultResourcesMap,
      DefaultAccounting,
      DefaultCompiler,
      MonthlyBillingCalculation(billingMonthInfo),
      Just(clog)
    )
  }

  @Test
  def testFullOnOff: Unit = {
    val clog = ContextualLogger.fromOther(NoVal, logger, "testFullOnOff()")

    ResourceEventStore.clearResourceEvents()
    VMTimeInstance.newONOFF(
      new MutableDateCalc(2012, 01, 10).goPlusHours(13).goPlusMinutes(30).toDate, // 2012-01-10 13:30:00.000
      10
    )

    showResourceEvents(clog)
    val userStateM = doFullMonthlyBilling(clog, BillingMonthInfoJan)
    showUserState(clog, userStateM)
  }

  @Ignore
  @Test
  def testOne: Unit = {
    val clog = ContextualLogger.fromOther(NoVal, logger, "testOne()")

    // Let's create our dates of interest
    val VMStartDateCalc = StartOfBillingYearDateCalc.copy.goPlusDays(1).goPlusHours(1)
    val VMStartDate = VMStartDateCalc.toDate

    // Within January, create one VM ON-OFF ...
    VMTimeInstance.newONOFF(VMStartDate, 9)

    val diskConsumptionDateCalc = StartOfBillingYearDateCalc.copy.goPlusHours(3)
    val diskConsumptionDate1 = diskConsumptionDateCalc.toDate
    val diskConsumptionDateCalc2 = diskConsumptionDateCalc.copy.goPlusDays(1).goPlusHours(1)
    val diskConsumptionDate2 = diskConsumptionDateCalc2.toDate

    // ... and two diskspace changes
    DiskInstance.consumeMB(diskConsumptionDate1, 99)
    DiskInstance.consumeMB(diskConsumptionDate2, 23)

    // 100MB 3G bandwidth
    val bwDateCalc = diskConsumptionDateCalc2.copy.goPlusDays(1)
    BandwidthInstance.useBandwidth(bwDateCalc.toDate, 100.0)

    // ... and one "future" event
    DiskInstance.consumeMB(
      StartOfBillingYearDateCalc.copy.
        goNextMonth.goPlusDays(6).
        goPlusHours(7).
        goPlusMinutes(7).
        goPlusSeconds(7).
        goPlusMillis(7).toDate,
      777)

    showResourceEvents(clog)

    // Policy: from 2012-01-01 to Infinity

    clog.debugMap("DefaultResourcesMap", DefaultResourcesMap.map, 1)

    val userStateM = doFullMonthlyBilling(clog, BillingMonthInfoJan)

    showUserState(clog, userStateM)
  }
}