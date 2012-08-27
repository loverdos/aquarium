/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package gr.grnet.aquarium.message.avro;

@SuppressWarnings("all")
public interface AquariumEvents {
  public static final org.apache.avro.Protocol PROTOCOL = org.apache.avro.Protocol.parse("{\"protocol\":\"AquariumEvents\",\"namespace\":\"gr.grnet.aquarium.message.avro\",\"types\":[{\"type\":\"enum\",\"name\":\"EventVersion\",\"symbols\":[\"VERSION_1_0\"]},{\"type\":\"record\",\"name\":\"AnyValue\",\"fields\":[{\"name\":\"anyValue\",\"type\":[\"null\",\"int\",\"long\",\"boolean\",\"double\",\"bytes\",\"string\"]}]},{\"type\":\"record\",\"name\":\"ResourceEvent\",\"fields\":[{\"name\":\"originalID\",\"type\":\"string\",\"aliases\":[\"ID\"]},{\"name\":\"inStoreID\",\"type\":\"string\"},{\"name\":\"occurredMillis\",\"type\":\"long\"},{\"name\":\"receivedMillis\",\"type\":\"long\"},{\"name\":\"userID\",\"type\":\"string\"},{\"name\":\"clientID\",\"type\":\"string\"},{\"name\":\"eventVersion\",\"type\":\"EventVersion\"},{\"name\":\"resourceType\",\"type\":\"string\",\"aliases\":[\"resource\"]},{\"name\":\"instanceID\",\"type\":\"string\"},{\"name\":\"value\",\"type\":\"string\"},{\"name\":\"details\",\"type\":{\"type\":\"map\",\"values\":\"AnyValue\"}}]},{\"type\":\"record\",\"name\":\"IMEvent\",\"fields\":[{\"name\":\"originalID\",\"type\":\"string\",\"aliases\":[\"ID\"]},{\"name\":\"inStoreID\",\"type\":\"string\"},{\"name\":\"occurredMillis\",\"type\":\"long\"},{\"name\":\"receivedMillis\",\"type\":\"long\"},{\"name\":\"userID\",\"type\":\"string\"},{\"name\":\"clientID\",\"type\":\"string\"},{\"name\":\"eventVersion\",\"type\":\"EventVersion\"},{\"name\":\"isActive\",\"type\":\"boolean\"},{\"name\":\"role\",\"type\":\"string\"},{\"name\":\"details\",\"type\":{\"type\":\"map\",\"values\":\"AnyValue\"}}]}],\"messages\":{}}");

  @SuppressWarnings("all")
  public interface Callback extends AquariumEvents {
    public static final org.apache.avro.Protocol PROTOCOL = gr.grnet.aquarium.message.avro.AquariumEvents.PROTOCOL;
  }
}