/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package gr.grnet.aquarium.message.avro.gen;

@SuppressWarnings("all")
public interface AquariumAny {
  public static final org.apache.avro.Protocol PROTOCOL = org.apache.avro.Protocol.parse("{\"protocol\":\"AquariumAny\",\"namespace\":\"gr.grnet.aquarium.message.avro.gen\",\"types\":[{\"type\":\"record\",\"name\":\"AnyValueMsg\",\"fields\":[{\"name\":\"anyValue\",\"type\":[\"null\",\"int\",\"long\",\"boolean\",\"double\",\"bytes\",{\"type\":\"string\",\"avro.java.string\":\"String\"},{\"type\":\"array\",\"items\":\"AnyValueMsg\"},{\"type\":\"map\",\"values\":\"AnyValueMsg\",\"avro.java.string\":\"String\"}]}]}],\"messages\":{}}");

  @SuppressWarnings("all")
  public interface Callback extends AquariumAny {
    public static final org.apache.avro.Protocol PROTOCOL = gr.grnet.aquarium.message.avro.gen.AquariumAny.PROTOCOL;
  }
}