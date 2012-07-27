import gr.grnet.aquarium.charging.VMChargingBehavior.Selectors.Power
import gr.grnet.aquarium.charging.{OnceChargingBehavior, ContinuousChargingBehavior, VMChargingBehavior}
import gr.grnet.aquarium.policy.FullPriceTable._
import gr.grnet.aquarium.policy.{EffectiveUnitPrice, EffectivePriceTable, FullPriceTable, ResourceType, StdPolicy}
import gr.grnet.aquarium.Timespan
import gr.grnet.aquarium.util.nameOfClass

// Definition of our standard policy in plain Scala

StdPolicy(
  id = "750E6309-AB60-41B4-8D4B-9FFEA6EF843C",
  parentID = None,

  validityTimespan = Timespan(0),

  resourceTypes = Set(
    ResourceType("vmtime", "Hr", nameOfClass[VMChargingBehavior]),
    ResourceType("diskspace", "MB/Hr", nameOfClass[ContinuousChargingBehavior])
  ),

  chargingBehaviors = Set(
    nameOfClass[VMChargingBehavior],
    nameOfClass[ContinuousChargingBehavior],
    nameOfClass[OnceChargingBehavior]
  ),

  roleMapping = Map(
    "default" -> FullPriceTable(Map(
      "diskspace" -> Map(
        DefaultSelectorKey -> EffectivePriceTable(EffectiveUnitPrice(0.01) :: Nil)
      ),
      "vmtime" -> Map(
        Power.powerOn  -> EffectivePriceTable(EffectiveUnitPrice(0.01) :: Nil),
        Power.powerOff -> EffectivePriceTable(EffectiveUnitPrice(0.001) :: Nil) // cheaper when the VM is OFF
      )
    ))
  )
)