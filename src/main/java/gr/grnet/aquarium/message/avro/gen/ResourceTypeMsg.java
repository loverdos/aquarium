/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package gr.grnet.aquarium.message.avro.gen;  
@SuppressWarnings("all")
public class ResourceTypeMsg extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"ResourceTypeMsg\",\"namespace\":\"gr.grnet.aquarium.message.avro.gen\",\"fields\":[{\"name\":\"name\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"unit\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"chargingBehaviorClass\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}}]}");
  @Deprecated public java.lang.String name;
  @Deprecated public java.lang.String unit;
  @Deprecated public java.lang.String chargingBehaviorClass;
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call. 
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return name;
    case 1: return unit;
    case 2: return chargingBehaviorClass;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  // Used by DatumReader.  Applications should not call. 
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: name = (java.lang.String)value$; break;
    case 1: unit = (java.lang.String)value$; break;
    case 2: chargingBehaviorClass = (java.lang.String)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'name' field.
   */
  public java.lang.String getName() {
    return name;
  }

  /**
   * Sets the value of the 'name' field.
   * @param value the value to set.
   */
  public void setName(java.lang.String value) {
    this.name = value;
  }

  /**
   * Gets the value of the 'unit' field.
   */
  public java.lang.String getUnit() {
    return unit;
  }

  /**
   * Sets the value of the 'unit' field.
   * @param value the value to set.
   */
  public void setUnit(java.lang.String value) {
    this.unit = value;
  }

  /**
   * Gets the value of the 'chargingBehaviorClass' field.
   */
  public java.lang.String getChargingBehaviorClass() {
    return chargingBehaviorClass;
  }

  /**
   * Sets the value of the 'chargingBehaviorClass' field.
   * @param value the value to set.
   */
  public void setChargingBehaviorClass(java.lang.String value) {
    this.chargingBehaviorClass = value;
  }

  /** Creates a new ResourceTypeMsg RecordBuilder */
  public static gr.grnet.aquarium.message.avro.gen.ResourceTypeMsg.Builder newBuilder() {
    return new gr.grnet.aquarium.message.avro.gen.ResourceTypeMsg.Builder();
  }
  
  /** Creates a new ResourceTypeMsg RecordBuilder by copying an existing Builder */
  public static gr.grnet.aquarium.message.avro.gen.ResourceTypeMsg.Builder newBuilder(gr.grnet.aquarium.message.avro.gen.ResourceTypeMsg.Builder other) {
    return new gr.grnet.aquarium.message.avro.gen.ResourceTypeMsg.Builder(other);
  }
  
  /** Creates a new ResourceTypeMsg RecordBuilder by copying an existing ResourceTypeMsg instance */
  public static gr.grnet.aquarium.message.avro.gen.ResourceTypeMsg.Builder newBuilder(gr.grnet.aquarium.message.avro.gen.ResourceTypeMsg other) {
    return new gr.grnet.aquarium.message.avro.gen.ResourceTypeMsg.Builder(other);
  }
  
  /**
   * RecordBuilder for ResourceTypeMsg instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<ResourceTypeMsg>
    implements org.apache.avro.data.RecordBuilder<ResourceTypeMsg> {

    private java.lang.String name;
    private java.lang.String unit;
    private java.lang.String chargingBehaviorClass;

    /** Creates a new Builder */
    private Builder() {
      super(gr.grnet.aquarium.message.avro.gen.ResourceTypeMsg.SCHEMA$);
    }
    
    /** Creates a Builder by copying an existing Builder */
    private Builder(gr.grnet.aquarium.message.avro.gen.ResourceTypeMsg.Builder other) {
      super(other);
    }
    
    /** Creates a Builder by copying an existing ResourceTypeMsg instance */
    private Builder(gr.grnet.aquarium.message.avro.gen.ResourceTypeMsg other) {
            super(gr.grnet.aquarium.message.avro.gen.ResourceTypeMsg.SCHEMA$);
      if (isValidValue(fields()[0], other.name)) {
        this.name = (java.lang.String) data().deepCopy(fields()[0].schema(), other.name);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.unit)) {
        this.unit = (java.lang.String) data().deepCopy(fields()[1].schema(), other.unit);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.chargingBehaviorClass)) {
        this.chargingBehaviorClass = (java.lang.String) data().deepCopy(fields()[2].schema(), other.chargingBehaviorClass);
        fieldSetFlags()[2] = true;
      }
    }

    /** Gets the value of the 'name' field */
    public java.lang.String getName() {
      return name;
    }
    
    /** Sets the value of the 'name' field */
    public gr.grnet.aquarium.message.avro.gen.ResourceTypeMsg.Builder setName(java.lang.String value) {
      validate(fields()[0], value);
      this.name = value;
      fieldSetFlags()[0] = true;
      return this; 
    }
    
    /** Checks whether the 'name' field has been set */
    public boolean hasName() {
      return fieldSetFlags()[0];
    }
    
    /** Clears the value of the 'name' field */
    public gr.grnet.aquarium.message.avro.gen.ResourceTypeMsg.Builder clearName() {
      name = null;
      fieldSetFlags()[0] = false;
      return this;
    }

    /** Gets the value of the 'unit' field */
    public java.lang.String getUnit() {
      return unit;
    }
    
    /** Sets the value of the 'unit' field */
    public gr.grnet.aquarium.message.avro.gen.ResourceTypeMsg.Builder setUnit(java.lang.String value) {
      validate(fields()[1], value);
      this.unit = value;
      fieldSetFlags()[1] = true;
      return this; 
    }
    
    /** Checks whether the 'unit' field has been set */
    public boolean hasUnit() {
      return fieldSetFlags()[1];
    }
    
    /** Clears the value of the 'unit' field */
    public gr.grnet.aquarium.message.avro.gen.ResourceTypeMsg.Builder clearUnit() {
      unit = null;
      fieldSetFlags()[1] = false;
      return this;
    }

    /** Gets the value of the 'chargingBehaviorClass' field */
    public java.lang.String getChargingBehaviorClass() {
      return chargingBehaviorClass;
    }
    
    /** Sets the value of the 'chargingBehaviorClass' field */
    public gr.grnet.aquarium.message.avro.gen.ResourceTypeMsg.Builder setChargingBehaviorClass(java.lang.String value) {
      validate(fields()[2], value);
      this.chargingBehaviorClass = value;
      fieldSetFlags()[2] = true;
      return this; 
    }
    
    /** Checks whether the 'chargingBehaviorClass' field has been set */
    public boolean hasChargingBehaviorClass() {
      return fieldSetFlags()[2];
    }
    
    /** Clears the value of the 'chargingBehaviorClass' field */
    public gr.grnet.aquarium.message.avro.gen.ResourceTypeMsg.Builder clearChargingBehaviorClass() {
      chargingBehaviorClass = null;
      fieldSetFlags()[2] = false;
      return this;
    }

    @Override
    public ResourceTypeMsg build() {
      try {
        ResourceTypeMsg record = new ResourceTypeMsg();
        record.name = fieldSetFlags()[0] ? this.name : (java.lang.String) defaultValue(fields()[0]);
        record.unit = fieldSetFlags()[1] ? this.unit : (java.lang.String) defaultValue(fields()[1]);
        record.chargingBehaviorClass = fieldSetFlags()[2] ? this.chargingBehaviorClass : (java.lang.String) defaultValue(fields()[2]);
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }
}