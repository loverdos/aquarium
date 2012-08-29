/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package gr.grnet.aquarium.message.avro;

@SuppressWarnings("all")
public interface AquariumConf {
  public static final org.apache.avro.Protocol PROTOCOL = org.apache.avro.Protocol.parse("{\"protocol\":\"AquariumConf\",\"namespace\":\"gr.grnet.aquarium.message.avro\",\"types\":[{\"type\":\"enum\",\"name\":\"_EventVersion\",\"symbols\":[\"VERSION_1_0\"]},{\"type\":\"record\",\"name\":\"_AnyValue\",\"fields\":[{\"name\":\"anyValue\",\"type\":[\"null\",\"int\",\"long\",\"boolean\",\"double\",\"bytes\",\"string\"]}]},{\"type\":\"record\",\"name\":\"_ResourceType\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"unit\",\"type\":\"string\"},{\"name\":\"chargingBehaviorClass\",\"type\":\"string\"}]},{\"type\":\"record\",\"name\":\"_CronSpecTuple\",\"fields\":[{\"name\":\"a\",\"type\":\"string\"},{\"name\":\"b\",\"type\":\"string\"}]},{\"type\":\"record\",\"name\":\"_EffectiveUnitPrice\",\"fields\":[{\"name\":\"unitPrice\",\"type\":\"double\"},{\"name\":\"when\",\"type\":[\"_CronSpecTuple\",\"null\"]}]},{\"type\":\"record\",\"name\":\"_EffectivePriceTable\",\"fields\":[{\"name\":\"priceOverrides\",\"type\":{\"type\":\"array\",\"items\":\"_EffectiveUnitPrice\"}}]},{\"type\":\"record\",\"name\":\"_SelectorValue\",\"fields\":[{\"name\":\"selectorValue\",\"type\":[\"_EffectivePriceTable\",{\"type\":\"map\",\"values\":\"_SelectorValue\"}]}]},{\"type\":\"record\",\"name\":\"_FullPriceTable\",\"fields\":[{\"name\":\"perResource\",\"type\":{\"type\":\"map\",\"values\":{\"type\":\"map\",\"values\":\"_SelectorValue\"}}}]},{\"type\":\"record\",\"name\":\"_Policy\",\"fields\":[{\"name\":\"ID\",\"type\":\"string\",\"aliases\":[\"id, _id, idInStore, inStoreID\"]},{\"name\":\"parentID\",\"type\":\"string\"},{\"name\":\"validFromMillis\",\"type\":\"long\"},{\"name\":\"validToMillis\",\"type\":\"long\"},{\"name\":\"resourceTypes\",\"type\":{\"type\":\"array\",\"items\":\"_ResourceType\"}},{\"name\":\"chargingBehaviors\",\"type\":{\"type\":\"array\",\"items\":\"string\"}},{\"name\":\"roleMapping\",\"type\":{\"type\":\"map\",\"values\":\"_FullPriceTable\"}}]}],\"messages\":{}}");

  @SuppressWarnings("all")
  public interface Callback extends AquariumConf {
    public static final org.apache.avro.Protocol PROTOCOL = gr.grnet.aquarium.message.avro.AquariumConf.PROTOCOL;
  }
}