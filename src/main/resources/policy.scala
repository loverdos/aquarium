import gr.grnet.aquarium.charging.{OnceChargingBehavior, ContinuousChargingBehavior, OnOffChargingBehavior, DiscreteChargingBehavior}
import gr.grnet.aquarium.policy.{EffectiveUnitPrice, EffectivePriceTable, FullPriceTable, ResourceType, StdPolicy}
import gr.grnet.aquarium.Timespan

// Definition of our standard policy in plain Scala

StdPolicy(
  id = "750E6309-AB60-41B4-8D4B-9FFEA6EF843C",
  parentID = None,

  validityTimespan = Timespan(0),

  resourceTypes = Set(
    ResourceType("bandwidth", "MB/Hr", classOf[DiscreteChargingBehavior].getName),
    ResourceType("vmtime", "Hr", classOf[OnOffChargingBehavior].getName),
    ResourceType("diskspace", "MB/Hr", classOf[ContinuousChargingBehavior].getName)
  ),

  chargingBehaviors = Set(
    classOf[DiscreteChargingBehavior].getName,
    classOf[OnOffChargingBehavior].getName,
    classOf[ContinuousChargingBehavior].getName,
    classOf[OnceChargingBehavior].getName
  ),

  roleMapping = Map(
    "default" -> FullPriceTable(Map(
      "bandwidth" -> EffectivePriceTable(EffectiveUnitPrice(0.01, None) :: Nil),
      "vmtime" -> EffectivePriceTable(EffectiveUnitPrice(0.01, None) :: Nil),
      "diskspace" -> EffectivePriceTable(EffectiveUnitPrice(0.01, None) :: Nil)
    ))
  )
)