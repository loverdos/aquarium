/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package gr.grnet.aquarium.message.avro.gen;

@SuppressWarnings("all")
public interface AquariumEvents {
  public static final org.apache.avro.Protocol PROTOCOL = org.apache.avro.Protocol.parse("{\"protocol\":\"AquariumEvents\",\"namespace\":\"gr.grnet.aquarium.message.avro.gen\",\"types\":[{\"type\":\"record\",\"name\":\"AnyValueMsg\",\"fields\":[{\"name\":\"anyValue\",\"type\":[\"null\",\"int\",\"long\",\"boolean\",\"double\",\"bytes\",\"string\"]}]},{\"type\":\"record\",\"name\":\"ResourceEventMsg\",\"fields\":[{\"name\":\"id\",\"type\":\"string\",\"aliases\":[\"originalID\",\"ID\"]},{\"name\":\"idInStore\",\"type\":\"string\"},{\"name\":\"occurredMillis\",\"type\":\"long\"},{\"name\":\"receivedMillis\",\"type\":\"long\",\"default\":0},{\"name\":\"userID\",\"type\":\"string\"},{\"name\":\"clientID\",\"type\":\"string\"},{\"name\":\"eventVersion\",\"type\":\"string\",\"default\":\"1.0\"},{\"name\":\"resource\",\"type\":\"string\",\"aliases\":[\"resourceType\"]},{\"name\":\"instanceID\",\"type\":\"string\"},{\"name\":\"value\",\"type\":\"string\"},{\"name\":\"details\",\"type\":{\"type\":\"map\",\"values\":\"AnyValueMsg\"}}]},{\"type\":\"record\",\"name\":\"IMEventMsg\",\"fields\":[{\"name\":\"id\",\"type\":\"string\",\"aliases\":[\"originalID\",\"ID\"]},{\"name\":\"idInStore\",\"type\":\"string\"},{\"name\":\"occurredMillis\",\"type\":\"long\"},{\"name\":\"receivedMillis\",\"type\":\"long\",\"default\":0},{\"name\":\"userID\",\"type\":\"string\"},{\"name\":\"clientID\",\"type\":\"string\"},{\"name\":\"eventVersion\",\"type\":\"string\",\"default\":\"1.0\"},{\"name\":\"isActive\",\"type\":\"boolean\"},{\"name\":\"role\",\"type\":\"string\"},{\"name\":\"details\",\"type\":{\"type\":\"map\",\"values\":\"AnyValueMsg\"}}]}],\"messages\":{}}");

  @SuppressWarnings("all")
  public interface Callback extends AquariumEvents {
    public static final org.apache.avro.Protocol PROTOCOL = gr.grnet.aquarium.message.avro.gen.AquariumEvents.PROTOCOL;
  }
}