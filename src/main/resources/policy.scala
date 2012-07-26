import gr.grnet.aquarium.charging.{OnceChargingBehavior, ContinuousChargingBehavior, VMChargingBehavior, DiscreteChargingBehavior}
import gr.grnet.aquarium.policy.{IsSelectorMap, IsEffectivePriceTable, EffectiveUnitPrice, EffectivePriceTable, FullPriceTable, ResourceType, StdPolicy}
import gr.grnet.aquarium.Timespan

// Definition of our standard policy in plain Scala

StdPolicy(
  id = "750E6309-AB60-41B4-8D4B-9FFEA6EF843C",
  parentID = None,

  validityTimespan = Timespan(0),

  resourceTypes = Set(
    ResourceType("bandwidth", "MB/Hr", classOf[DiscreteChargingBehavior].getName),
    ResourceType("vmtime", "Hr", classOf[VMChargingBehavior].getName),
    ResourceType("diskspace", "MB/Hr", classOf[ContinuousChargingBehavior].getName)
  ),

  chargingBehaviors = Set(
    classOf[DiscreteChargingBehavior].getName,
    classOf[VMChargingBehavior].getName,
    classOf[ContinuousChargingBehavior].getName,
    classOf[OnceChargingBehavior].getName
  ),

  roleMapping = Map(
    "default" -> FullPriceTable(Map(
      "bandwidth" -> IsEffectivePriceTable(EffectivePriceTable(EffectiveUnitPrice(0.01) :: Nil)),
      "diskspace" -> IsEffectivePriceTable(EffectivePriceTable(EffectiveUnitPrice(0.01) :: Nil)),
      "vmtime"    -> IsSelector(Map(
        VMChargingBehavior.Selectors._1.powerOn ->
          IsEffectivePriceTable(EffectivePriceTable(EffectiveUnitPrice(0.02) :: Nil)),
        VMChargingBehavior.Selectors._1.powerOff ->
          IsEffectivePriceTable(EffectivePriceTable(EffectiveUnitPrice(0.01) :: Nil)) // powerOff is cheaper!
      ))
    ))
  )
)