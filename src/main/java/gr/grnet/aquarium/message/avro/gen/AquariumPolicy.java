/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package gr.grnet.aquarium.message.avro.gen;

@SuppressWarnings("all")
public interface AquariumPolicy {
  public static final org.apache.avro.Protocol PROTOCOL = org.apache.avro.Protocol.parse("{\"protocol\":\"AquariumPolicy\",\"namespace\":\"gr.grnet.aquarium.message.avro.gen\",\"types\":[{\"type\":\"record\",\"name\":\"ResourceTypeMsg\",\"fields\":[{\"name\":\"name\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"unit\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"chargingBehaviorClass\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}}]},{\"type\":\"record\",\"name\":\"CronSpecTupleMsg\",\"fields\":[{\"name\":\"a\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"b\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}}]},{\"type\":\"record\",\"name\":\"EffectiveUnitPriceMsg\",\"fields\":[{\"name\":\"unitPrice\",\"type\":\"double\"},{\"name\":\"when\",\"type\":[\"CronSpecTupleMsg\",\"null\"]}]},{\"type\":\"record\",\"name\":\"EffectivePriceTableMsg\",\"fields\":[{\"name\":\"priceOverrides\",\"type\":{\"type\":\"array\",\"items\":\"EffectiveUnitPriceMsg\"}}]},{\"type\":\"record\",\"name\":\"SelectorValueMsg\",\"fields\":[{\"name\":\"selectorValue\",\"type\":[\"EffectivePriceTableMsg\",{\"type\":\"map\",\"values\":\"SelectorValueMsg\",\"avro.java.string\":\"String\"}]}]},{\"type\":\"record\",\"name\":\"FullPriceTableMsg\",\"fields\":[{\"name\":\"perResource\",\"type\":{\"type\":\"map\",\"values\":{\"type\":\"map\",\"values\":\"SelectorValueMsg\",\"avro.java.string\":\"String\"},\"avro.java.string\":\"String\"}}]},{\"type\":\"record\",\"name\":\"PolicyMsg\",\"fields\":[{\"name\":\"originalID\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"inStoreID\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"parentID\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"validFromMillis\",\"type\":\"long\"},{\"name\":\"validToMillis\",\"type\":\"long\"},{\"name\":\"resourceTypes\",\"type\":{\"type\":\"array\",\"items\":\"ResourceTypeMsg\"}},{\"name\":\"chargingBehaviors\",\"type\":{\"type\":\"array\",\"items\":{\"type\":\"string\",\"avro.java.string\":\"String\"}}},{\"name\":\"roleMapping\",\"type\":{\"type\":\"map\",\"values\":\"FullPriceTableMsg\",\"avro.java.string\":\"String\"}}]}],\"messages\":{}}");

  @SuppressWarnings("all")
  public interface Callback extends AquariumPolicy {
    public static final org.apache.avro.Protocol PROTOCOL = gr.grnet.aquarium.message.avro.gen.AquariumPolicy.PROTOCOL;
  }
}