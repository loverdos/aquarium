/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package gr.grnet.aquarium.message.avro;

@SuppressWarnings("all")
public interface AquariumEvents {
  public static final org.apache.avro.Protocol PROTOCOL = org.apache.avro.Protocol.parse("{\"protocol\":\"AquariumEvents\",\"namespace\":\"gr.grnet.aquarium.message.avro\",\"types\":[{\"type\":\"enum\",\"name\":\"_EventVersion\",\"symbols\":[\"VERSION_1_0\"]},{\"type\":\"record\",\"name\":\"_AnyValue\",\"fields\":[{\"name\":\"anyValue\",\"type\":[\"null\",\"int\",\"long\",\"boolean\",\"double\",\"bytes\",\"string\"]}]},{\"type\":\"record\",\"name\":\"_ResourceEvent\",\"fields\":[{\"name\":\"originalID\",\"type\":\"string\",\"aliases\":[\"ID\"]},{\"name\":\"inStoreID\",\"type\":\"string\"},{\"name\":\"occurredMillis\",\"type\":\"long\"},{\"name\":\"receivedMillis\",\"type\":\"long\"},{\"name\":\"userID\",\"type\":\"string\"},{\"name\":\"clientID\",\"type\":\"string\"},{\"name\":\"eventVersion\",\"type\":\"_EventVersion\"},{\"name\":\"resourceType\",\"type\":\"string\",\"aliases\":[\"resource\"]},{\"name\":\"instanceID\",\"type\":\"string\"},{\"name\":\"value\",\"type\":\"string\"},{\"name\":\"details\",\"type\":{\"type\":\"map\",\"values\":\"_AnyValue\"}}]},{\"type\":\"record\",\"name\":\"_IMEvent\",\"fields\":[{\"name\":\"originalID\",\"type\":\"string\",\"aliases\":[\"ID\"]},{\"name\":\"inStoreID\",\"type\":\"string\",\"aliases\":[\"_id, idInStore\"]},{\"name\":\"occurredMillis\",\"type\":\"long\"},{\"name\":\"receivedMillis\",\"type\":\"long\"},{\"name\":\"userID\",\"type\":\"string\"},{\"name\":\"clientID\",\"type\":\"string\"},{\"name\":\"eventVersion\",\"type\":\"_EventVersion\"},{\"name\":\"isActive\",\"type\":\"boolean\"},{\"name\":\"role\",\"type\":\"string\"},{\"name\":\"details\",\"type\":{\"type\":\"map\",\"values\":\"_AnyValue\"}}]}],\"messages\":{}}");

  @SuppressWarnings("all")
  public interface Callback extends AquariumEvents {
    public static final org.apache.avro.Protocol PROTOCOL = gr.grnet.aquarium.message.avro.AquariumEvents.PROTOCOL;
  }
}