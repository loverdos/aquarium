import gr.grnet.aquarium.charging.{OnceChargingBehavior, ContinuousChargingBehavior, OnOffChargingBehavior, DiscreteChargingBehavior}
import gr.grnet.aquarium.policy.{EffectiveUnitPrice, EffectivePriceTable, FullPriceTable, ResourceType, StdPolicy}
import gr.grnet.aquarium.Timespan

// Definition of our standard policy in plain Scala
// This will be dynamically interpreted during Aquarium startup

StdPolicy(
    id = "policy-1",
    parentID = None,

    validityTimespan = Timespan(0),

    resourceTypes = Set(
      ResourceType("bandwidth", "MB/Hr", DiscreteChargingBehavior),
      ResourceType("vmtime",    "Hr",    OnOffChargingBehavior),
      ResourceType("diskspace", "MB/Hr", ContinuousChargingBehavior)
    ),

    chargingBehaviorClasses = Set(
      DiscreteChargingBehavior.getClass.getName,
      OnOffChargingBehavior.getClass.getName,
      ContinuousChargingBehavior.getClass.getName,
      OnceChargingBehavior.getClass.getName
    ),

    roleMapping = Map(
      "default" -> FullPriceTable(Map(
        "bandwidth" -> EffectivePriceTable(EffectiveUnitPrice(0.01, None) :: Nil),
        "vmtime"    -> EffectivePriceTable(EffectiveUnitPrice(0.01, None) :: Nil),
        "diskspace" -> EffectivePriceTable(EffectiveUnitPrice(0.01, None) :: Nil)
      ))
    )
  )