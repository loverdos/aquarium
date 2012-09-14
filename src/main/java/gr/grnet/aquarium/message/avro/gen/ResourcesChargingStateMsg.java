/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package gr.grnet.aquarium.message.avro.gen;  
@SuppressWarnings("all")
public class ResourcesChargingStateMsg extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"ResourcesChargingStateMsg\",\"namespace\":\"gr.grnet.aquarium.message.avro.gen\",\"fields\":[{\"name\":\"resource\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"details\",\"type\":{\"type\":\"map\",\"values\":{\"type\":\"record\",\"name\":\"AnyValueMsg\",\"fields\":[{\"name\":\"anyValue\",\"type\":[\"null\",\"int\",\"long\",\"boolean\",\"double\",\"bytes\",{\"type\":\"string\",\"avro.java.string\":\"String\"},{\"type\":\"array\",\"items\":\"AnyValueMsg\"},{\"type\":\"map\",\"values\":\"AnyValueMsg\",\"avro.java.string\":\"String\"}]}]},\"avro.java.string\":\"String\"}},{\"name\":\"stateOfResourceInstance\",\"type\":{\"type\":\"map\",\"values\":{\"type\":\"record\",\"name\":\"ResourceInstanceChargingStateMsg\",\"fields\":[{\"name\":\"clientID\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"resource\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"instanceID\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"details\",\"type\":{\"type\":\"map\",\"values\":\"AnyValueMsg\",\"avro.java.string\":\"String\"}},{\"name\":\"previousEvents\",\"type\":{\"type\":\"array\",\"items\":{\"type\":\"record\",\"name\":\"ResourceEventMsg\",\"fields\":[{\"name\":\"originalID\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"inStoreID\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"occurredMillis\",\"type\":\"long\"},{\"name\":\"receivedMillis\",\"type\":\"long\",\"default\":0},{\"name\":\"userID\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"clientID\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"eventVersion\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"},\"default\":\"1.0\"},{\"name\":\"resource\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"instanceID\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"value\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"isSynthetic\",\"type\":\"boolean\",\"default\":false},{\"name\":\"details\",\"type\":{\"type\":\"map\",\"values\":\"AnyValueMsg\",\"avro.java.string\":\"String\"}}]}}},{\"name\":\"implicitlyIssuedStartEvents\",\"type\":{\"type\":\"array\",\"items\":\"ResourceEventMsg\"}},{\"name\":\"accumulatingAmount\",\"type\":\"double\"},{\"name\":\"oldAccumulatingAmount\",\"type\":\"double\"},{\"name\":\"previousValue\",\"type\":\"double\"},{\"name\":\"currentValue\",\"type\":\"double\"}]},\"avro.java.string\":\"String\"}}]}");
  @Deprecated public java.lang.String resource;
  @Deprecated public java.util.Map<java.lang.String,gr.grnet.aquarium.message.avro.gen.AnyValueMsg> details;
  @Deprecated public java.util.Map<java.lang.String,gr.grnet.aquarium.message.avro.gen.ResourceInstanceChargingStateMsg> stateOfResourceInstance;
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call. 
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return resource;
    case 1: return details;
    case 2: return stateOfResourceInstance;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  // Used by DatumReader.  Applications should not call. 
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: resource = (java.lang.String)value$; break;
    case 1: details = (java.util.Map<java.lang.String,gr.grnet.aquarium.message.avro.gen.AnyValueMsg>)value$; break;
    case 2: stateOfResourceInstance = (java.util.Map<java.lang.String,gr.grnet.aquarium.message.avro.gen.ResourceInstanceChargingStateMsg>)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'resource' field.
   */
  public java.lang.String getResource() {
    return resource;
  }

  /**
   * Sets the value of the 'resource' field.
   * @param value the value to set.
   */
  public void setResource(java.lang.String value) {
    this.resource = value;
  }

  /**
   * Gets the value of the 'details' field.
   */
  public java.util.Map<java.lang.String,gr.grnet.aquarium.message.avro.gen.AnyValueMsg> getDetails() {
    return details;
  }

  /**
   * Sets the value of the 'details' field.
   * @param value the value to set.
   */
  public void setDetails(java.util.Map<java.lang.String,gr.grnet.aquarium.message.avro.gen.AnyValueMsg> value) {
    this.details = value;
  }

  /**
   * Gets the value of the 'stateOfResourceInstance' field.
   */
  public java.util.Map<java.lang.String,gr.grnet.aquarium.message.avro.gen.ResourceInstanceChargingStateMsg> getStateOfResourceInstance() {
    return stateOfResourceInstance;
  }

  /**
   * Sets the value of the 'stateOfResourceInstance' field.
   * @param value the value to set.
   */
  public void setStateOfResourceInstance(java.util.Map<java.lang.String,gr.grnet.aquarium.message.avro.gen.ResourceInstanceChargingStateMsg> value) {
    this.stateOfResourceInstance = value;
  }

  /** Creates a new ResourcesChargingStateMsg RecordBuilder */
  public static gr.grnet.aquarium.message.avro.gen.ResourcesChargingStateMsg.Builder newBuilder() {
    return new gr.grnet.aquarium.message.avro.gen.ResourcesChargingStateMsg.Builder();
  }
  
  /** Creates a new ResourcesChargingStateMsg RecordBuilder by copying an existing Builder */
  public static gr.grnet.aquarium.message.avro.gen.ResourcesChargingStateMsg.Builder newBuilder(gr.grnet.aquarium.message.avro.gen.ResourcesChargingStateMsg.Builder other) {
    return new gr.grnet.aquarium.message.avro.gen.ResourcesChargingStateMsg.Builder(other);
  }
  
  /** Creates a new ResourcesChargingStateMsg RecordBuilder by copying an existing ResourcesChargingStateMsg instance */
  public static gr.grnet.aquarium.message.avro.gen.ResourcesChargingStateMsg.Builder newBuilder(gr.grnet.aquarium.message.avro.gen.ResourcesChargingStateMsg other) {
    return new gr.grnet.aquarium.message.avro.gen.ResourcesChargingStateMsg.Builder(other);
  }
  
  /**
   * RecordBuilder for ResourcesChargingStateMsg instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<ResourcesChargingStateMsg>
    implements org.apache.avro.data.RecordBuilder<ResourcesChargingStateMsg> {

    private java.lang.String resource;
    private java.util.Map<java.lang.String,gr.grnet.aquarium.message.avro.gen.AnyValueMsg> details;
    private java.util.Map<java.lang.String,gr.grnet.aquarium.message.avro.gen.ResourceInstanceChargingStateMsg> stateOfResourceInstance;

    /** Creates a new Builder */
    private Builder() {
      super(gr.grnet.aquarium.message.avro.gen.ResourcesChargingStateMsg.SCHEMA$);
    }
    
    /** Creates a Builder by copying an existing Builder */
    private Builder(gr.grnet.aquarium.message.avro.gen.ResourcesChargingStateMsg.Builder other) {
      super(other);
    }
    
    /** Creates a Builder by copying an existing ResourcesChargingStateMsg instance */
    private Builder(gr.grnet.aquarium.message.avro.gen.ResourcesChargingStateMsg other) {
            super(gr.grnet.aquarium.message.avro.gen.ResourcesChargingStateMsg.SCHEMA$);
      if (isValidValue(fields()[0], other.resource)) {
        this.resource = (java.lang.String) data().deepCopy(fields()[0].schema(), other.resource);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.details)) {
        this.details = (java.util.Map<java.lang.String,gr.grnet.aquarium.message.avro.gen.AnyValueMsg>) data().deepCopy(fields()[1].schema(), other.details);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.stateOfResourceInstance)) {
        this.stateOfResourceInstance = (java.util.Map<java.lang.String,gr.grnet.aquarium.message.avro.gen.ResourceInstanceChargingStateMsg>) data().deepCopy(fields()[2].schema(), other.stateOfResourceInstance);
        fieldSetFlags()[2] = true;
      }
    }

    /** Gets the value of the 'resource' field */
    public java.lang.String getResource() {
      return resource;
    }
    
    /** Sets the value of the 'resource' field */
    public gr.grnet.aquarium.message.avro.gen.ResourcesChargingStateMsg.Builder setResource(java.lang.String value) {
      validate(fields()[0], value);
      this.resource = value;
      fieldSetFlags()[0] = true;
      return this; 
    }
    
    /** Checks whether the 'resource' field has been set */
    public boolean hasResource() {
      return fieldSetFlags()[0];
    }
    
    /** Clears the value of the 'resource' field */
    public gr.grnet.aquarium.message.avro.gen.ResourcesChargingStateMsg.Builder clearResource() {
      resource = null;
      fieldSetFlags()[0] = false;
      return this;
    }

    /** Gets the value of the 'details' field */
    public java.util.Map<java.lang.String,gr.grnet.aquarium.message.avro.gen.AnyValueMsg> getDetails() {
      return details;
    }
    
    /** Sets the value of the 'details' field */
    public gr.grnet.aquarium.message.avro.gen.ResourcesChargingStateMsg.Builder setDetails(java.util.Map<java.lang.String,gr.grnet.aquarium.message.avro.gen.AnyValueMsg> value) {
      validate(fields()[1], value);
      this.details = value;
      fieldSetFlags()[1] = true;
      return this; 
    }
    
    /** Checks whether the 'details' field has been set */
    public boolean hasDetails() {
      return fieldSetFlags()[1];
    }
    
    /** Clears the value of the 'details' field */
    public gr.grnet.aquarium.message.avro.gen.ResourcesChargingStateMsg.Builder clearDetails() {
      details = null;
      fieldSetFlags()[1] = false;
      return this;
    }

    /** Gets the value of the 'stateOfResourceInstance' field */
    public java.util.Map<java.lang.String,gr.grnet.aquarium.message.avro.gen.ResourceInstanceChargingStateMsg> getStateOfResourceInstance() {
      return stateOfResourceInstance;
    }
    
    /** Sets the value of the 'stateOfResourceInstance' field */
    public gr.grnet.aquarium.message.avro.gen.ResourcesChargingStateMsg.Builder setStateOfResourceInstance(java.util.Map<java.lang.String,gr.grnet.aquarium.message.avro.gen.ResourceInstanceChargingStateMsg> value) {
      validate(fields()[2], value);
      this.stateOfResourceInstance = value;
      fieldSetFlags()[2] = true;
      return this; 
    }
    
    /** Checks whether the 'stateOfResourceInstance' field has been set */
    public boolean hasStateOfResourceInstance() {
      return fieldSetFlags()[2];
    }
    
    /** Clears the value of the 'stateOfResourceInstance' field */
    public gr.grnet.aquarium.message.avro.gen.ResourcesChargingStateMsg.Builder clearStateOfResourceInstance() {
      stateOfResourceInstance = null;
      fieldSetFlags()[2] = false;
      return this;
    }

    @Override
    public ResourcesChargingStateMsg build() {
      try {
        ResourcesChargingStateMsg record = new ResourcesChargingStateMsg();
        record.resource = fieldSetFlags()[0] ? this.resource : (java.lang.String) defaultValue(fields()[0]);
        record.details = fieldSetFlags()[1] ? this.details : (java.util.Map<java.lang.String,gr.grnet.aquarium.message.avro.gen.AnyValueMsg>) defaultValue(fields()[1]);
        record.stateOfResourceInstance = fieldSetFlags()[2] ? this.stateOfResourceInstance : (java.util.Map<java.lang.String,gr.grnet.aquarium.message.avro.gen.ResourceInstanceChargingStateMsg>) defaultValue(fields()[2]);
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }
}