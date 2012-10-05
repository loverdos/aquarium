/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package gr.grnet.aquarium.message.avro.gen;  
@SuppressWarnings("all")
public class ServiceEntryMsg extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"ServiceEntryMsg\",\"namespace\":\"gr.grnet.aquarium.message.avro.gen\",\"fields\":[{\"name\":\"serviceName\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"totalCredits\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"totalElapsedTime\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"totalUnits\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"unitName\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"details\",\"type\":{\"type\":\"array\",\"items\":{\"type\":\"record\",\"name\":\"ResourceEntryMsg\",\"fields\":[{\"name\":\"resourceName\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"resourceType\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"unitName\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"totalCredits\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"totalElapsedTime\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"totalUnits\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"details\",\"type\":{\"type\":\"array\",\"items\":{\"type\":\"record\",\"name\":\"EventEntryMsg\",\"fields\":[{\"name\":\"eventType\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"details\",\"type\":{\"type\":\"array\",\"items\":{\"type\":\"record\",\"name\":\"ChargeEntryMsg\",\"fields\":[{\"name\":\"id\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"unitPrice\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"startTime\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"endTime\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"totalCredits\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"totalElapsedTime\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"totalUnits\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}}]}}}]}}}]}}}]}");
  @Deprecated public java.lang.String serviceName;
  @Deprecated public java.lang.String totalCredits;
  @Deprecated public java.lang.String totalElapsedTime;
  @Deprecated public java.lang.String totalUnits;
  @Deprecated public java.lang.String unitName;
  @Deprecated public java.util.List<gr.grnet.aquarium.message.avro.gen.ResourceEntryMsg> details;

  /**
   * Default constructor.
   */
  public ServiceEntryMsg() {}

  /**
   * All-args constructor.
   */
  public ServiceEntryMsg(java.lang.String serviceName, java.lang.String totalCredits, java.lang.String totalElapsedTime, java.lang.String totalUnits, java.lang.String unitName, java.util.List<gr.grnet.aquarium.message.avro.gen.ResourceEntryMsg> details) {
    this.serviceName = serviceName;
    this.totalCredits = totalCredits;
    this.totalElapsedTime = totalElapsedTime;
    this.totalUnits = totalUnits;
    this.unitName = unitName;
    this.details = details;
  }

  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call. 
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return serviceName;
    case 1: return totalCredits;
    case 2: return totalElapsedTime;
    case 3: return totalUnits;
    case 4: return unitName;
    case 5: return details;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  // Used by DatumReader.  Applications should not call. 
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: serviceName = (java.lang.String)value$; break;
    case 1: totalCredits = (java.lang.String)value$; break;
    case 2: totalElapsedTime = (java.lang.String)value$; break;
    case 3: totalUnits = (java.lang.String)value$; break;
    case 4: unitName = (java.lang.String)value$; break;
    case 5: details = (java.util.List<gr.grnet.aquarium.message.avro.gen.ResourceEntryMsg>)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'serviceName' field.
   */
  public java.lang.String getServiceName() {
    return serviceName;
  }

  /**
   * Sets the value of the 'serviceName' field.
   * @param value the value to set.
   */
  public void setServiceName(java.lang.String value) {
    this.serviceName = value;
  }

  /**
   * Gets the value of the 'totalCredits' field.
   */
  public java.lang.String getTotalCredits() {
    return totalCredits;
  }

  /**
   * Sets the value of the 'totalCredits' field.
   * @param value the value to set.
   */
  public void setTotalCredits(java.lang.String value) {
    this.totalCredits = value;
  }

  /**
   * Gets the value of the 'totalElapsedTime' field.
   */
  public java.lang.String getTotalElapsedTime() {
    return totalElapsedTime;
  }

  /**
   * Sets the value of the 'totalElapsedTime' field.
   * @param value the value to set.
   */
  public void setTotalElapsedTime(java.lang.String value) {
    this.totalElapsedTime = value;
  }

  /**
   * Gets the value of the 'totalUnits' field.
   */
  public java.lang.String getTotalUnits() {
    return totalUnits;
  }

  /**
   * Sets the value of the 'totalUnits' field.
   * @param value the value to set.
   */
  public void setTotalUnits(java.lang.String value) {
    this.totalUnits = value;
  }

  /**
   * Gets the value of the 'unitName' field.
   */
  public java.lang.String getUnitName() {
    return unitName;
  }

  /**
   * Sets the value of the 'unitName' field.
   * @param value the value to set.
   */
  public void setUnitName(java.lang.String value) {
    this.unitName = value;
  }

  /**
   * Gets the value of the 'details' field.
   */
  public java.util.List<gr.grnet.aquarium.message.avro.gen.ResourceEntryMsg> getDetails() {
    return details;
  }

  /**
   * Sets the value of the 'details' field.
   * @param value the value to set.
   */
  public void setDetails(java.util.List<gr.grnet.aquarium.message.avro.gen.ResourceEntryMsg> value) {
    this.details = value;
  }

  /** Creates a new ServiceEntryMsg RecordBuilder */
  public static gr.grnet.aquarium.message.avro.gen.ServiceEntryMsg.Builder newBuilder() {
    return new gr.grnet.aquarium.message.avro.gen.ServiceEntryMsg.Builder();
  }
  
  /** Creates a new ServiceEntryMsg RecordBuilder by copying an existing Builder */
  public static gr.grnet.aquarium.message.avro.gen.ServiceEntryMsg.Builder newBuilder(gr.grnet.aquarium.message.avro.gen.ServiceEntryMsg.Builder other) {
    return new gr.grnet.aquarium.message.avro.gen.ServiceEntryMsg.Builder(other);
  }
  
  /** Creates a new ServiceEntryMsg RecordBuilder by copying an existing ServiceEntryMsg instance */
  public static gr.grnet.aquarium.message.avro.gen.ServiceEntryMsg.Builder newBuilder(gr.grnet.aquarium.message.avro.gen.ServiceEntryMsg other) {
    return new gr.grnet.aquarium.message.avro.gen.ServiceEntryMsg.Builder(other);
  }
  
  /**
   * RecordBuilder for ServiceEntryMsg instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<ServiceEntryMsg>
    implements org.apache.avro.data.RecordBuilder<ServiceEntryMsg> {

    private java.lang.String serviceName;
    private java.lang.String totalCredits;
    private java.lang.String totalElapsedTime;
    private java.lang.String totalUnits;
    private java.lang.String unitName;
    private java.util.List<gr.grnet.aquarium.message.avro.gen.ResourceEntryMsg> details;

    /** Creates a new Builder */
    private Builder() {
      super(gr.grnet.aquarium.message.avro.gen.ServiceEntryMsg.SCHEMA$);
    }
    
    /** Creates a Builder by copying an existing Builder */
    private Builder(gr.grnet.aquarium.message.avro.gen.ServiceEntryMsg.Builder other) {
      super(other);
    }
    
    /** Creates a Builder by copying an existing ServiceEntryMsg instance */
    private Builder(gr.grnet.aquarium.message.avro.gen.ServiceEntryMsg other) {
            super(gr.grnet.aquarium.message.avro.gen.ServiceEntryMsg.SCHEMA$);
      if (isValidValue(fields()[0], other.serviceName)) {
        this.serviceName = (java.lang.String) data().deepCopy(fields()[0].schema(), other.serviceName);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.totalCredits)) {
        this.totalCredits = (java.lang.String) data().deepCopy(fields()[1].schema(), other.totalCredits);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.totalElapsedTime)) {
        this.totalElapsedTime = (java.lang.String) data().deepCopy(fields()[2].schema(), other.totalElapsedTime);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.totalUnits)) {
        this.totalUnits = (java.lang.String) data().deepCopy(fields()[3].schema(), other.totalUnits);
        fieldSetFlags()[3] = true;
      }
      if (isValidValue(fields()[4], other.unitName)) {
        this.unitName = (java.lang.String) data().deepCopy(fields()[4].schema(), other.unitName);
        fieldSetFlags()[4] = true;
      }
      if (isValidValue(fields()[5], other.details)) {
        this.details = (java.util.List<gr.grnet.aquarium.message.avro.gen.ResourceEntryMsg>) data().deepCopy(fields()[5].schema(), other.details);
        fieldSetFlags()[5] = true;
      }
    }

    /** Gets the value of the 'serviceName' field */
    public java.lang.String getServiceName() {
      return serviceName;
    }
    
    /** Sets the value of the 'serviceName' field */
    public gr.grnet.aquarium.message.avro.gen.ServiceEntryMsg.Builder setServiceName(java.lang.String value) {
      validate(fields()[0], value);
      this.serviceName = value;
      fieldSetFlags()[0] = true;
      return this; 
    }
    
    /** Checks whether the 'serviceName' field has been set */
    public boolean hasServiceName() {
      return fieldSetFlags()[0];
    }
    
    /** Clears the value of the 'serviceName' field */
    public gr.grnet.aquarium.message.avro.gen.ServiceEntryMsg.Builder clearServiceName() {
      serviceName = null;
      fieldSetFlags()[0] = false;
      return this;
    }

    /** Gets the value of the 'totalCredits' field */
    public java.lang.String getTotalCredits() {
      return totalCredits;
    }
    
    /** Sets the value of the 'totalCredits' field */
    public gr.grnet.aquarium.message.avro.gen.ServiceEntryMsg.Builder setTotalCredits(java.lang.String value) {
      validate(fields()[1], value);
      this.totalCredits = value;
      fieldSetFlags()[1] = true;
      return this; 
    }
    
    /** Checks whether the 'totalCredits' field has been set */
    public boolean hasTotalCredits() {
      return fieldSetFlags()[1];
    }
    
    /** Clears the value of the 'totalCredits' field */
    public gr.grnet.aquarium.message.avro.gen.ServiceEntryMsg.Builder clearTotalCredits() {
      totalCredits = null;
      fieldSetFlags()[1] = false;
      return this;
    }

    /** Gets the value of the 'totalElapsedTime' field */
    public java.lang.String getTotalElapsedTime() {
      return totalElapsedTime;
    }
    
    /** Sets the value of the 'totalElapsedTime' field */
    public gr.grnet.aquarium.message.avro.gen.ServiceEntryMsg.Builder setTotalElapsedTime(java.lang.String value) {
      validate(fields()[2], value);
      this.totalElapsedTime = value;
      fieldSetFlags()[2] = true;
      return this; 
    }
    
    /** Checks whether the 'totalElapsedTime' field has been set */
    public boolean hasTotalElapsedTime() {
      return fieldSetFlags()[2];
    }
    
    /** Clears the value of the 'totalElapsedTime' field */
    public gr.grnet.aquarium.message.avro.gen.ServiceEntryMsg.Builder clearTotalElapsedTime() {
      totalElapsedTime = null;
      fieldSetFlags()[2] = false;
      return this;
    }

    /** Gets the value of the 'totalUnits' field */
    public java.lang.String getTotalUnits() {
      return totalUnits;
    }
    
    /** Sets the value of the 'totalUnits' field */
    public gr.grnet.aquarium.message.avro.gen.ServiceEntryMsg.Builder setTotalUnits(java.lang.String value) {
      validate(fields()[3], value);
      this.totalUnits = value;
      fieldSetFlags()[3] = true;
      return this; 
    }
    
    /** Checks whether the 'totalUnits' field has been set */
    public boolean hasTotalUnits() {
      return fieldSetFlags()[3];
    }
    
    /** Clears the value of the 'totalUnits' field */
    public gr.grnet.aquarium.message.avro.gen.ServiceEntryMsg.Builder clearTotalUnits() {
      totalUnits = null;
      fieldSetFlags()[3] = false;
      return this;
    }

    /** Gets the value of the 'unitName' field */
    public java.lang.String getUnitName() {
      return unitName;
    }
    
    /** Sets the value of the 'unitName' field */
    public gr.grnet.aquarium.message.avro.gen.ServiceEntryMsg.Builder setUnitName(java.lang.String value) {
      validate(fields()[4], value);
      this.unitName = value;
      fieldSetFlags()[4] = true;
      return this; 
    }
    
    /** Checks whether the 'unitName' field has been set */
    public boolean hasUnitName() {
      return fieldSetFlags()[4];
    }
    
    /** Clears the value of the 'unitName' field */
    public gr.grnet.aquarium.message.avro.gen.ServiceEntryMsg.Builder clearUnitName() {
      unitName = null;
      fieldSetFlags()[4] = false;
      return this;
    }

    /** Gets the value of the 'details' field */
    public java.util.List<gr.grnet.aquarium.message.avro.gen.ResourceEntryMsg> getDetails() {
      return details;
    }
    
    /** Sets the value of the 'details' field */
    public gr.grnet.aquarium.message.avro.gen.ServiceEntryMsg.Builder setDetails(java.util.List<gr.grnet.aquarium.message.avro.gen.ResourceEntryMsg> value) {
      validate(fields()[5], value);
      this.details = value;
      fieldSetFlags()[5] = true;
      return this; 
    }
    
    /** Checks whether the 'details' field has been set */
    public boolean hasDetails() {
      return fieldSetFlags()[5];
    }
    
    /** Clears the value of the 'details' field */
    public gr.grnet.aquarium.message.avro.gen.ServiceEntryMsg.Builder clearDetails() {
      details = null;
      fieldSetFlags()[5] = false;
      return this;
    }

    @Override
    public ServiceEntryMsg build() {
      try {
        ServiceEntryMsg record = new ServiceEntryMsg();
        record.serviceName = fieldSetFlags()[0] ? this.serviceName : (java.lang.String) defaultValue(fields()[0]);
        record.totalCredits = fieldSetFlags()[1] ? this.totalCredits : (java.lang.String) defaultValue(fields()[1]);
        record.totalElapsedTime = fieldSetFlags()[2] ? this.totalElapsedTime : (java.lang.String) defaultValue(fields()[2]);
        record.totalUnits = fieldSetFlags()[3] ? this.totalUnits : (java.lang.String) defaultValue(fields()[3]);
        record.unitName = fieldSetFlags()[4] ? this.unitName : (java.lang.String) defaultValue(fields()[4]);
        record.details = fieldSetFlags()[5] ? this.details : (java.util.List<gr.grnet.aquarium.message.avro.gen.ResourceEntryMsg>) defaultValue(fields()[5]);
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }
}
