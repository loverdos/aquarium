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

package gr.grnet.aquarium.user

import gr.grnet.aquarium.store.memory.MemStoreProvider
import gr.grnet.aquarium.util.{Loggable, ContextualLogger}
import gr.grnet.aquarium.simulation._
import gr.grnet.aquarium.uid.{UIDGenerator, ConcurrentVMLocalUIDGenerator}
import org.junit.{Assert, Ignore, Test}
import gr.grnet.aquarium.logic.accounting.algorithm.{ExecutableChargingBehaviorAlgorithm, CostPolicyAlgorithmCompiler}
import gr.grnet.aquarium.{Aquarium, ResourceLocator, AquariumBuilder, AquariumException}
import gr.grnet.aquarium.util.date.MutableDateCalc
import gr.grnet.aquarium.computation.BillingMonthInfo
import gr.grnet.aquarium.charging._
import gr.grnet.aquarium.policy.{PolicyDefinedFullPriceTableRef, FullPriceTable, PolicyModel}
import gr.grnet.aquarium.charging.reason.{NoSpecificChargingReason, MonthlyBillChargingReason}
import gr.grnet.aquarium.charging.state.WorkingUserState
import gr.grnet.aquarium.policy.FullPriceTable._
import gr.grnet.aquarium.Timespan
import gr.grnet.aquarium.simulation.AquariumSim
import scala.Some
import gr.grnet.aquarium.policy.EffectiveUnitPrice
import gr.grnet.aquarium.policy.ResourceType
import gr.grnet.aquarium.policy.StdUserAgreement
import gr.grnet.aquarium.charging.state.UserStateBootstrap
import gr.grnet.aquarium.policy.EffectivePriceTable
import gr.grnet.aquarium.policy.StdPolicy
import gr.grnet.aquarium.simulation.ClientSim


/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
class UserStateComputationsTest extends Loggable {
  final val DoubleDelta = 0.001

  final val BandwidthUnitPrice = 3.3 //
  final val VMTimeUnitPrice    = 1.5 //
  final val DiskspaceUnitPrice = 2.7 //

  final val OnOffUnitPrice = VMTimeUnitPrice
  final val ContinuousUnitPrice = DiskspaceUnitPrice
  final val DiscreteUnitPrice = BandwidthUnitPrice

  final val DefaultPolicy: PolicyModel = StdPolicy(
    id = "policy-1",
    parentID = None,
    validityTimespan = Timespan(0),
    resourceTypes = Set(
      ResourceType("vmtime", "Hr", classOf[VMChargingBehavior].getName),
      ResourceType("diskspace", "MB/Hr", classOf[ContinuousChargingBehavior].getName)
    ),
    chargingBehaviors = Set(
      classOf[VMChargingBehavior].getName,
      classOf[ContinuousChargingBehavior].getName,
      classOf[OnceChargingBehavior].getName
    ),
    roleMapping = Map(
      "default" -> FullPriceTable(Map(
        "diskspace" -> Map(DefaultSelectorKey -> EffectivePriceTable(EffectiveUnitPrice(0.01) :: Nil)),
        "vmtime"    -> Map(DefaultSelectorKey -> EffectivePriceTable(EffectiveUnitPrice(0.01) :: Nil))
      ))
    )
  )

  val aquarium = new AquariumBuilder(ResourceLocator.AquariumProperties, ResourceLocator.DefaultPolicyModel).
    update(Aquarium.EnvKeys.storeProvider, new MemStoreProvider).
    build()

  val ResourceEventStore = aquarium.resourceEventStore

  val ChargingService = aquarium.chargingService

  val DefaultAlgorithm = new ExecutableChargingBehaviorAlgorithm {
    def creditsForContinuous(timeDelta: Double, oldTotalAmount: Double) =
      hrs(timeDelta) * oldTotalAmount * ContinuousUnitPrice

    final val creditsForDiskspace = creditsForContinuous(_, _)
    
    def creditsForDiscrete(currentValue: Double) =
      currentValue * DiscreteUnitPrice

    final val creditsForBandwidth = creditsForDiscrete(_)

    def creditsForOnOff(timeDelta: Double) =
      hrs(timeDelta) * OnOffUnitPrice

    final val creditsForVMTime = creditsForOnOff(_)

    @inline private[this]
    def hrs(millis: Double) = millis / 1000 / 60 / 60

    def apply(vars: Map[ChargingInput, Any]): Double = {
      vars.apply(ChargingBehaviorNameInput) match {
        case ChargingBehaviorAliases.continuous ⇒
          val unitPrice = vars(UnitPriceInput).asInstanceOf[Double]
          val oldTotalAmount = vars(OldTotalAmountInput).asInstanceOf[Double]
          val timeDelta = vars(TimeDeltaInput).asInstanceOf[Double]

          Assert.assertEquals(ContinuousUnitPrice, unitPrice, DoubleDelta)

          creditsForContinuous(timeDelta, oldTotalAmount)

        case ChargingBehaviorAliases.discrete ⇒
          val unitPrice = vars(UnitPriceInput).asInstanceOf[Double]
          val currentValue = vars(CurrentValueInput).asInstanceOf[Double]

          Assert.assertEquals(DiscreteUnitPrice, unitPrice, DoubleDelta)

          creditsForDiscrete(currentValue)

        case ChargingBehaviorAliases.vmtime ⇒
          val unitPrice = vars(UnitPriceInput).asInstanceOf[Double]
          val timeDelta = vars(TimeDeltaInput).asInstanceOf[Double]

          Assert.assertEquals(OnOffUnitPrice, unitPrice, DoubleDelta)

          creditsForOnOff(timeDelta)

        case ChargingBehaviorAliases.once ⇒
          val currentValue = vars(CurrentValueInput).asInstanceOf[Double]
          currentValue

        case name ⇒
          throw new AquariumException("Unknown cost policy %s".format(name))
      }
    }

    override def toString = "DefaultAlgorithm(%s)".format(
      Map(
        ChargingBehaviorAliases.continuous -> "hrs(timeDelta) * oldTotalAmount * %s".format(ContinuousUnitPrice),
        ChargingBehaviorAliases.discrete   -> "currentValue * %s".format(DiscreteUnitPrice),
        ChargingBehaviorAliases.vmtime      -> "hrs(timeDelta) * %s".format(OnOffUnitPrice),
        ChargingBehaviorAliases.once       -> "currentValue"))
  }

  val DefaultCompiler  = new CostPolicyAlgorithmCompiler {
    def compile(definition: String): ExecutableChargingBehaviorAlgorithm = {
      DefaultAlgorithm
    }
  }
  //val DefaultAlgorithm = justForSure(DefaultCompiler.compile("")).get // hardcoded since we know exactly what this is

  val VMTimeDSLResource = DefaultPolicy.resourceTypesMap("vmtime")

  // For this to work, the definitions must match those in the YAML above.
  // Those StdXXXResourceSim are just for debugging convenience anyway, so they must match by design.
  val VMTimeResourceSim    = StdVMTimeResourceSim.fromPolicy(DefaultPolicy)
  val DiskspaceResourceSim = StdDiskspaceResourceSim.fromPolicy(DefaultPolicy)

  // There are two client services, synnefo and pithos.
  val TheUIDGenerator: UIDGenerator[_] = new ConcurrentVMLocalUIDGenerator
  val Synnefo = ClientSim("synnefo")(TheUIDGenerator)
  val Pithos  = ClientSim("pithos" )(TheUIDGenerator)

  val StartOfBillingYearDateCalc = new MutableDateCalc(2012,  1, 1)
  val UserCreationDate           = new MutableDateCalc(2011, 11, 1).toDate

  val BillingMonthInfoJan = {
    val MutableDateCalcJan = new MutableDateCalc(2012, 1, 1)
    BillingMonthInfo.fromDateCalc(MutableDateCalcJan)
  }
  val BillingMonthInfoFeb = BillingMonthInfo.fromDateCalc(new MutableDateCalc(2012,  2, 1))
  val BillingMonthInfoMar = BillingMonthInfo.fromDateCalc(new MutableDateCalc(2012,  3, 1))

  // Store the default policy
  val policyDateCalc        = StartOfBillingYearDateCalc.copy
  val policyOccurredMillis  = policyDateCalc.toMillis
  val policyValidFromMillis = policyDateCalc.copy.goPreviousYear.toMillis
  val policyValidToMillis   = policyDateCalc.copy.goNextYear.toMillis

  aquarium.policyStore.insertPolicy(DefaultPolicy)

  val AquariumSim_ = AquariumSim(List(VMTimeResourceSim, DiskspaceResourceSim), aquarium.resourceEventStore)
  val DefaultResourcesMap = DefaultPolicy.resourceTypesMap//AquariumSim_.resourcesMap

  val UserCKKL  = AquariumSim_.newUser("CKKL", UserCreationDate)

//  val InitialUserState = UserState.createInitialUserState(
//    userID = UserCKKL.userID,
//    userCreationMillis = UserCreationDate.getTime,
//    totalCredits = 0.0,
//    initialRole = "default",
//    initialAgreement = DSLAgreement.DefaultAgreementName
//  )

  val UserStateBootstrapper = UserStateBootstrap(
    userID = UserCKKL.userID,
    userCreationMillis = UserCreationDate.getTime(),
    initialAgreement = StdUserAgreement("", None, 0, Long.MaxValue, "default", PolicyDefinedFullPriceTableRef),
    initialCredits = 0.0
  )

  // By convention
  // - synnefo is for VMTime and Bandwidth
  // - pithos is for Diskspace
  val VMTimeInstanceSim    = VMTimeResourceSim.newInstance   ("VM.1",   UserCKKL, Synnefo)
  val DiskInstanceSim      = DiskspaceResourceSim.newInstance("DISK.1", UserCKKL, Pithos)

  private[this]
  def showUserState(clog: ContextualLogger, workingUserState: WorkingUserState) {
//    val id = workingUserState.id
//    val parentId = workingUserState.parentUserStateIDInStore
//    val credits = workingUserState.totalCredits
//    val newWalletEntries = workingUserState.newWalletEntries.map(_.toDebugString)
//    val changeReason = workingUserState.lastChangeReason
//    val implicitlyIssued = workingUserState.implicitlyIssuedSnapshot.implicitlyIssuedEvents.map(_.toDebugString)
//    val latestResourceEvents = workingUserState.latestResourceEventsSnapshot.resourceEvents.map(_.toDebugString)
//
//    clog.debug("_id = %s", id)
//    clog.debug("parentId = %s", parentId)
//    clog.debug("credits = %s", credits)
//    clog.debug("changeReason = %s", changeReason)
//    clog.debugSeq("implicitlyIssued", implicitlyIssued, 0)
//    clog.debugSeq("latestResourceEvents", latestResourceEvents, 0)
//    clog.debugSeq("newWalletEntries", newWalletEntries, 0)
  }

  private[this]
  def showResourceEvents(clog: ContextualLogger): Unit = {
    clog.debug("")
    clog.begin("Events by OccurredMillis")
    clog.withIndent {
      for(event <- UserCKKL.myResourceEventsByOccurredDate) {
        clog.debug(event.toDebugString)
      }
    }
    clog.end("Events by OccurredMillis")
    clog.debug("")
  }

  private[this]
  def doFullMonthlyBilling(
      clog: ContextualLogger,
      billingMonthInfo: BillingMonthInfo,
      billingTimeMillis: Long) = {

    ChargingService.replayMonthChargingUpTo(
      billingMonthInfo,
      billingTimeMillis,
      UserStateBootstrapper,
      DefaultResourcesMap,
      MonthlyBillChargingReason(NoSpecificChargingReason(), billingMonthInfo),
      aquarium.userStateStore.insertUserState,
      Some(clog)
    )
  }

  private[this]
  def expectCredits(
      clog: ContextualLogger,
      creditsConsumed: Double,
      workingUserState: WorkingUserState,
      accuracy: Double = 0.001
  ): Unit = {

    val computed = workingUserState.totalCredits
    Assert.assertEquals(-creditsConsumed, computed, accuracy)

    clog.info("Consumed %.3f credits [accuracy = %f]", creditsConsumed, accuracy)
  }

  private[this]
  def millis2hrs(millis: Long) = millis.toDouble / 1000 / 60 / 60

  private[this]
  def hrs2millis(hrs: Double) = (hrs * 60 * 60 * 1000).toLong

  /**
   * Test a sequence of ON, OFF vmtime events.
   */
  @Ignore
  @Test
  def testFullOnOff: Unit = {
    val clog = ContextualLogger.fromOther(None, logger, "testFullOnOff()")
    clog.begin()

    ResourceEventStore.clearResourceEvents()
    val OnOffDurationHrs = 10
    val OnOffDurationMillis = hrs2millis(OnOffDurationHrs.toDouble)

    VMTimeInstanceSim.newONOFF(
      new MutableDateCalc(2012, 01, 10).goPlusHours(13).goPlusMinutes(30).toDate, // 2012-01-10 13:30:00.000
      OnOffDurationHrs
    )

    val credits = DefaultAlgorithm.creditsForVMTime(OnOffDurationMillis)

    showResourceEvents(clog)

    val workingUserState = doFullMonthlyBilling(clog, BillingMonthInfoJan, BillingMonthInfoJan.monthStopMillis)

    showUserState(clog, workingUserState)

    expectCredits(clog, credits, workingUserState)

    clog.end()
  }

  @Ignore
  @Test
  def testLonelyON: Unit = {
    val clog = ContextualLogger.fromOther(None, logger, "testLonelyON()")
    clog.begin()

    ResourceEventStore.clearResourceEvents()
    
    val JanStart = new MutableDateCalc(2012, 01, 01)
    val JanEnd = JanStart.copy.goEndOfThisMonth
    val JanStartDate = JanStart.toDate
    val OnOffImplicitDurationMillis = JanEnd.toMillis - JanStart.toMillis
    val OnOffImplicitDurationHrs = millis2hrs(OnOffImplicitDurationMillis)

    VMTimeInstanceSim.newON(JanStartDate)

    val credits = DefaultAlgorithm.creditsForVMTime(OnOffImplicitDurationMillis)

    showResourceEvents(clog)

    val userState = doFullMonthlyBilling(clog, BillingMonthInfoJan, BillingMonthInfoJan.monthStopMillis)

    showUserState(clog, userState)

    expectCredits(clog, credits, userState)

    clog.end()
  }

//  @Ignore
  @Test
  def testOrphanOFF: Unit = {
    val clog = ContextualLogger.fromOther(None, logger, "testOrphanOFF()")
    clog.begin()

    ResourceEventStore.clearResourceEvents()

    val JanStart = new MutableDateCalc(2012, 01, 01)
    val JanEnd = JanStart.copy.goEndOfThisMonth
    val JanStartDate = JanStart.toDate
    val OnOffImplicitDurationMillis = JanEnd.toMillis - JanStart.toMillis
    val OnOffImplicitDurationHrs = millis2hrs(OnOffImplicitDurationMillis)

    VMTimeInstanceSim.newOFF(JanStartDate)

    // This is an orphan event, so no credits will be charged
    val credits = 0

    showResourceEvents(clog)

    val userState = doFullMonthlyBilling(clog, BillingMonthInfoJan, BillingMonthInfoJan.monthStopMillis)

    showUserState(clog, userState)

    expectCredits(clog, credits, userState)

    clog.end()
  }

  @Ignore
  @Test
  def testOne: Unit = {
    val clog = ContextualLogger.fromOther(None, logger, "testOne()")
    clog.begin()

    // Let's create our dates of interest
    val VMStartDateCalc = StartOfBillingYearDateCalc.copy.goPlusDays(1).goPlusHours(1)
    val VMStartDate = VMStartDateCalc.toDate

    // Within January, create one VM ON-OFF ...
    VMTimeInstanceSim.newONOFF(VMStartDate, 9)

    val diskConsumptionDateCalc = StartOfBillingYearDateCalc.copy.goPlusHours(3)
    val diskConsumptionDate1 = diskConsumptionDateCalc.toDate
    val diskConsumptionDateCalc2 = diskConsumptionDateCalc.copy.goPlusDays(1).goPlusHours(1)
    val diskConsumptionDate2 = diskConsumptionDateCalc2.toDate

    // ... and two diskspace changes
    DiskInstanceSim.consumeMB(diskConsumptionDate1, 99)
    DiskInstanceSim.consumeMB(diskConsumptionDate2, 23)

    // ... and one "future" event
    DiskInstanceSim.consumeMB(
      StartOfBillingYearDateCalc.copy.
        goNextMonth.goPlusDays(6).
        goPlusHours(7).
        goPlusMinutes(7).
        goPlusSeconds(7).
        goPlusMillis(7).toDate,
      777)

    showResourceEvents(clog)

    // Policy: from 2012-01-01 to Infinity

    clog.debugMap("DefaultResourcesMap", DefaultResourcesMap, 1)

    val userState = doFullMonthlyBilling(clog, BillingMonthInfoJan, BillingMonthInfoJan.monthStopMillis)

    showUserState(clog, userState)

    clog.end()
  }
}