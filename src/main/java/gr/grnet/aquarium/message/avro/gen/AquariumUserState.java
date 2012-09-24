/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package gr.grnet.aquarium.message.avro.gen;

@SuppressWarnings("all")
public interface AquariumUserState {
  public static final org.apache.avro.Protocol PROTOCOL = org.apache.avro.Protocol.parse("{\"protocol\":\"AquariumUserState\",\"namespace\":\"gr.grnet.aquarium.message.avro.gen\",\"types\":[{\"type\":\"record\",\"name\":\"ResourceTypeMsg\",\"fields\":[{\"name\":\"name\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"unit\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"chargingBehaviorClass\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}}]},{\"type\":\"record\",\"name\":\"CronSpecTupleMsg\",\"fields\":[{\"name\":\"a\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"b\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}}]},{\"type\":\"record\",\"name\":\"EffectiveUnitPriceMsg\",\"fields\":[{\"name\":\"unitPrice\",\"type\":\"double\"},{\"name\":\"when\",\"type\":[\"CronSpecTupleMsg\",\"null\"]}]},{\"type\":\"record\",\"name\":\"EffectivePriceTableMsg\",\"fields\":[{\"name\":\"priceOverrides\",\"type\":{\"type\":\"array\",\"items\":\"EffectiveUnitPriceMsg\"}}]},{\"type\":\"record\",\"name\":\"SelectorValueMsg\",\"fields\":[{\"name\":\"selectorValue\",\"type\":[\"EffectivePriceTableMsg\",{\"type\":\"map\",\"values\":\"SelectorValueMsg\",\"avro.java.string\":\"String\"}]}]},{\"type\":\"record\",\"name\":\"FullPriceTableMsg\",\"fields\":[{\"name\":\"perResource\",\"type\":{\"type\":\"map\",\"values\":{\"type\":\"map\",\"values\":\"SelectorValueMsg\",\"avro.java.string\":\"String\"},\"avro.java.string\":\"String\"}}]},{\"type\":\"record\",\"name\":\"PolicyMsg\",\"fields\":[{\"name\":\"originalID\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"inStoreID\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"parentID\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"validFromMillis\",\"type\":\"long\"},{\"name\":\"validToMillis\",\"type\":\"long\"},{\"name\":\"resourceMapping\",\"type\":{\"type\":\"map\",\"values\":\"ResourceTypeMsg\",\"avro.java.string\":\"String\"}},{\"name\":\"chargingBehaviors\",\"type\":{\"type\":\"array\",\"items\":{\"type\":\"string\",\"avro.java.string\":\"String\"}}},{\"name\":\"roleMapping\",\"type\":{\"type\":\"map\",\"values\":\"FullPriceTableMsg\",\"avro.java.string\":\"String\"}}]},{\"type\":\"record\",\"name\":\"AnyValueMsg\",\"fields\":[{\"name\":\"anyValue\",\"type\":[\"null\",\"int\",\"long\",\"boolean\",\"double\",\"bytes\",{\"type\":\"string\",\"avro.java.string\":\"String\"},{\"type\":\"array\",\"items\":\"AnyValueMsg\"},{\"type\":\"map\",\"values\":\"AnyValueMsg\",\"avro.java.string\":\"String\"}]}]},{\"type\":\"record\",\"name\":\"DetailsMsg\",\"fields\":[{\"name\":\"details\",\"type\":{\"type\":\"map\",\"values\":\"AnyValueMsg\",\"avro.java.string\":\"String\"}}]},{\"type\":\"record\",\"name\":\"ResourceEventMsg\",\"fields\":[{\"name\":\"originalID\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"inStoreID\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"occurredMillis\",\"type\":\"long\"},{\"name\":\"receivedMillis\",\"type\":\"long\",\"default\":0},{\"name\":\"userID\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"clientID\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"eventVersion\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"},\"default\":\"1.0\"},{\"name\":\"resource\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"instanceID\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"value\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"isSynthetic\",\"type\":\"boolean\",\"default\":false},{\"name\":\"details\",\"type\":{\"type\":\"map\",\"values\":\"AnyValueMsg\",\"avro.java.string\":\"String\"}}]},{\"type\":\"record\",\"name\":\"IMEventMsg\",\"fields\":[{\"name\":\"originalID\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"inStoreID\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"occurredMillis\",\"type\":\"long\"},{\"name\":\"receivedMillis\",\"type\":\"long\",\"default\":0},{\"name\":\"userID\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"clientID\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"eventVersion\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"},\"default\":\"1.0\"},{\"name\":\"eventType\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"isActive\",\"type\":\"boolean\"},{\"name\":\"role\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"isSynthetic\",\"type\":\"boolean\",\"default\":false},{\"name\":\"details\",\"type\":{\"type\":\"map\",\"values\":\"AnyValueMsg\",\"avro.java.string\":\"String\"}}]},{\"type\":\"record\",\"name\":\"UserAgreementMsg\",\"fields\":[{\"name\":\"id\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"relatedIMEventOriginalID\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"userID\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"occurredMillis\",\"type\":\"long\",\"default\":0},{\"name\":\"validFromMillis\",\"type\":\"long\"},{\"name\":\"validToMillis\",\"type\":\"long\"},{\"name\":\"role\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"fullPriceTableRef\",\"type\":[\"FullPriceTableMsg\",\"null\"]},{\"name\":\"relatedIMEventMsg\",\"type\":[\"IMEventMsg\",\"null\"]}]},{\"type\":\"record\",\"name\":\"ResourceInstanceChargingStateMsg\",\"fields\":[{\"name\":\"clientID\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"resource\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"instanceID\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"details\",\"type\":{\"type\":\"map\",\"values\":\"AnyValueMsg\",\"avro.java.string\":\"String\"}},{\"name\":\"previousEvents\",\"type\":{\"type\":\"array\",\"items\":\"ResourceEventMsg\"}},{\"name\":\"implicitlyIssuedStartEvents\",\"type\":{\"type\":\"array\",\"items\":\"ResourceEventMsg\"}},{\"name\":\"accumulatingAmount\",\"type\":\"double\"},{\"name\":\"oldAccumulatingAmount\",\"type\":\"double\"},{\"name\":\"previousValue\",\"type\":\"double\"},{\"name\":\"currentValue\",\"type\":\"double\"}]},{\"type\":\"record\",\"name\":\"ResourcesChargingStateMsg\",\"fields\":[{\"name\":\"resource\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"details\",\"type\":{\"type\":\"map\",\"values\":\"AnyValueMsg\",\"avro.java.string\":\"String\"}},{\"name\":\"stateOfResourceInstance\",\"type\":{\"type\":\"map\",\"values\":\"ResourceInstanceChargingStateMsg\",\"avro.java.string\":\"String\"}}]},{\"type\":\"record\",\"name\":\"UserAgreementHistoryMsg\",\"fields\":[{\"name\":\"originalID\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"inStoreID\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"userID\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"latestOccurredMillis\",\"type\":\"long\"},{\"name\":\"latestValidFromMillis\",\"type\":\"long\"},{\"name\":\"userCreationTimeMillis\",\"type\":\"long\",\"default\":0},{\"name\":\"agreements\",\"type\":{\"type\":\"array\",\"items\":\"UserAgreementMsg\"}}]},{\"type\":\"record\",\"name\":\"ChargeslotMsg\",\"fields\":[{\"name\":\"startMillis\",\"type\":\"long\"},{\"name\":\"stopMillis\",\"type\":\"long\"},{\"name\":\"unitPrice\",\"type\":\"double\"},{\"name\":\"explanation\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"},\"default\":\"\"},{\"name\":\"creditsToSubtract\",\"type\":\"double\",\"default\":0.0}]},{\"type\":\"record\",\"name\":\"WalletEntryMsg\",\"fields\":[{\"name\":\"userID\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"sumOfCreditsToSubtract\",\"type\":\"double\"},{\"name\":\"oldTotalCredits\",\"type\":\"double\"},{\"name\":\"newTotalCredits\",\"type\":\"double\"},{\"name\":\"whenComputedMillis\",\"type\":\"long\"},{\"name\":\"referenceStartMillis\",\"type\":\"long\"},{\"name\":\"referenceStopMillis\",\"type\":\"long\"},{\"name\":\"billingYear\",\"type\":\"int\"},{\"name\":\"billingMonth\",\"type\":\"int\"},{\"name\":\"billingMonthDay\",\"type\":\"int\"},{\"name\":\"chargeslots\",\"type\":{\"type\":\"array\",\"items\":\"ChargeslotMsg\"}},{\"name\":\"resourceEvents\",\"type\":{\"type\":\"array\",\"items\":\"ResourceEventMsg\"}},{\"name\":\"resourceType\",\"type\":\"ResourceTypeMsg\"},{\"name\":\"isSynthetic\",\"type\":\"boolean\",\"default\":false}]},{\"type\":\"record\",\"name\":\"WalletEntriesMsg\",\"fields\":[{\"name\":\"entries\",\"type\":{\"type\":\"array\",\"items\":\"WalletEntryMsg\"}}]},{\"type\":\"record\",\"name\":\"UserStateMsg\",\"fields\":[{\"name\":\"originalID\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"inStoreID\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"userID\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"occurredMillis\",\"type\":\"long\"},{\"name\":\"parentOriginalID\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"parentInStoreID\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"stateOfResources\",\"type\":{\"type\":\"map\",\"values\":\"ResourcesChargingStateMsg\",\"avro.java.string\":\"String\"}},{\"name\":\"totalCredits\",\"type\":\"double\",\"default\":0.0},{\"name\":\"latestUpdateMillis\",\"type\":\"long\"},{\"name\":\"latestResourceEventOccurredMillis\",\"type\":\"long\"},{\"name\":\"billingPeriodOutOfSyncResourceEventsCounter\",\"type\":\"long\",\"default\":0},{\"name\":\"billingYear\",\"type\":\"int\"},{\"name\":\"billingMonth\",\"type\":\"int\"},{\"name\":\"billingMonthDay\",\"type\":\"int\"},{\"name\":\"isFullBillingMonth\",\"type\":\"boolean\",\"default\":false},{\"name\":\"walletEntries\",\"type\":{\"type\":\"array\",\"items\":\"WalletEntryMsg\"}}]}],\"messages\":{}}");

  @SuppressWarnings("all")
  public interface Callback extends AquariumUserState {
    public static final org.apache.avro.Protocol PROTOCOL = gr.grnet.aquarium.message.avro.gen.AquariumUserState.PROTOCOL;
  }
}