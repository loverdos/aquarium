/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package gr.grnet.aquarium.message.avro.gen;  
@SuppressWarnings("all")
public class BillEntryMsg extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"BillEntryMsg\",\"namespace\":\"gr.grnet.aquarium.message.avro.gen\",\"fields\":[{\"name\":\"id\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"userID\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"status\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"remainingCredits\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"deductedCredits\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"startTime\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"endTime\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"details\",\"type\":{\"type\":\"array\",\"items\":{\"type\":\"record\",\"name\":\"ServiceEntryMsg\",\"fields\":[{\"name\":\"serviceName\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"totalCredits\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"totalElapsedTime\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"totalUnits\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"unitName\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"details\",\"type\":{\"type\":\"array\",\"items\":{\"type\":\"record\",\"name\":\"ResourceEntryMsg\",\"fields\":[{\"name\":\"resourceName\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"resourceType\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"unitName\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"totalCredits\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"totalElapsedTime\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"totalUnits\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"details\",\"type\":{\"type\":\"array\",\"items\":{\"type\":\"record\",\"name\":\"EventEntryMsg\",\"fields\":[{\"name\":\"eventType\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"details\",\"type\":{\"type\":\"array\",\"items\":{\"type\":\"record\",\"name\":\"ChargeEntryMsg\",\"fields\":[{\"name\":\"id\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"unitPrice\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"startTime\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"endTime\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"totalCredits\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"totalElapsedTime\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"totalUnits\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}}]}}}]}}}]}}}]}}}]}");
  @Deprecated public java.lang.String id;
  @Deprecated public java.lang.String userID;
  @Deprecated public java.lang.String status;
  @Deprecated public java.lang.String remainingCredits;
  @Deprecated public java.lang.String deductedCredits;
  @Deprecated public java.lang.String startTime;
  @Deprecated public java.lang.String endTime;
  @Deprecated public java.util.List<gr.grnet.aquarium.message.avro.gen.ServiceEntryMsg> details;

  /**
   * Default constructor.
   */
  public BillEntryMsg() {}

  /**
   * All-args constructor.
   */
  public BillEntryMsg(java.lang.String id, java.lang.String userID, java.lang.String status, java.lang.String remainingCredits, java.lang.String deductedCredits, java.lang.String startTime, java.lang.String endTime, java.util.List<gr.grnet.aquarium.message.avro.gen.ServiceEntryMsg> details) {
    this.id = id;
    this.userID = userID;
    this.status = status;
    this.remainingCredits = remainingCredits;
    this.deductedCredits = deductedCredits;
    this.startTime = startTime;
    this.endTime = endTime;
    this.details = details;
  }

  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call. 
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return id;
    case 1: return userID;
    case 2: return status;
    case 3: return remainingCredits;
    case 4: return deductedCredits;
    case 5: return startTime;
    case 6: return endTime;
    case 7: return details;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  // Used by DatumReader.  Applications should not call. 
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: id = (java.lang.String)value$; break;
    case 1: userID = (java.lang.String)value$; break;
    case 2: status = (java.lang.String)value$; break;
    case 3: remainingCredits = (java.lang.String)value$; break;
    case 4: deductedCredits = (java.lang.String)value$; break;
    case 5: startTime = (java.lang.String)value$; break;
    case 6: endTime = (java.lang.String)value$; break;
    case 7: details = (java.util.List<gr.grnet.aquarium.message.avro.gen.ServiceEntryMsg>)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'id' field.
   */
  public java.lang.String getId() {
    return id;
  }

  /**
   * Sets the value of the 'id' field.
   * @param value the value to set.
   */
  public void setId(java.lang.String value) {
    this.id = value;
  }

  /**
   * Gets the value of the 'userID' field.
   */
  public java.lang.String getUserID() {
    return userID;
  }

  /**
   * Sets the value of the 'userID' field.
   * @param value the value to set.
   */
  public void setUserID(java.lang.String value) {
    this.userID = value;
  }

  /**
   * Gets the value of the 'status' field.
   */
  public java.lang.String getStatus() {
    return status;
  }

  /**
   * Sets the value of the 'status' field.
   * @param value the value to set.
   */
  public void setStatus(java.lang.String value) {
    this.status = value;
  }

  /**
   * Gets the value of the 'remainingCredits' field.
   */
  public java.lang.String getRemainingCredits() {
    return remainingCredits;
  }

  /**
   * Sets the value of the 'remainingCredits' field.
   * @param value the value to set.
   */
  public void setRemainingCredits(java.lang.String value) {
    this.remainingCredits = value;
  }

  /**
   * Gets the value of the 'deductedCredits' field.
   */
  public java.lang.String getDeductedCredits() {
    return deductedCredits;
  }

  /**
   * Sets the value of the 'deductedCredits' field.
   * @param value the value to set.
   */
  public void setDeductedCredits(java.lang.String value) {
    this.deductedCredits = value;
  }

  /**
   * Gets the value of the 'startTime' field.
   */
  public java.lang.String getStartTime() {
    return startTime;
  }

  /**
   * Sets the value of the 'startTime' field.
   * @param value the value to set.
   */
  public void setStartTime(java.lang.String value) {
    this.startTime = value;
  }

  /**
   * Gets the value of the 'endTime' field.
   */
  public java.lang.String getEndTime() {
    return endTime;
  }

  /**
   * Sets the value of the 'endTime' field.
   * @param value the value to set.
   */
  public void setEndTime(java.lang.String value) {
    this.endTime = value;
  }

  /**
   * Gets the value of the 'details' field.
   */
  public java.util.List<gr.grnet.aquarium.message.avro.gen.ServiceEntryMsg> getDetails() {
    return details;
  }

  /**
   * Sets the value of the 'details' field.
   * @param value the value to set.
   */
  public void setDetails(java.util.List<gr.grnet.aquarium.message.avro.gen.ServiceEntryMsg> value) {
    this.details = value;
  }

  /** Creates a new BillEntryMsg RecordBuilder */
  public static gr.grnet.aquarium.message.avro.gen.BillEntryMsg.Builder newBuilder() {
    return new gr.grnet.aquarium.message.avro.gen.BillEntryMsg.Builder();
  }
  
  /** Creates a new BillEntryMsg RecordBuilder by copying an existing Builder */
  public static gr.grnet.aquarium.message.avro.gen.BillEntryMsg.Builder newBuilder(gr.grnet.aquarium.message.avro.gen.BillEntryMsg.Builder other) {
    return new gr.grnet.aquarium.message.avro.gen.BillEntryMsg.Builder(other);
  }
  
  /** Creates a new BillEntryMsg RecordBuilder by copying an existing BillEntryMsg instance */
  public static gr.grnet.aquarium.message.avro.gen.BillEntryMsg.Builder newBuilder(gr.grnet.aquarium.message.avro.gen.BillEntryMsg other) {
    return new gr.grnet.aquarium.message.avro.gen.BillEntryMsg.Builder(other);
  }
  
  /**
   * RecordBuilder for BillEntryMsg instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<BillEntryMsg>
    implements org.apache.avro.data.RecordBuilder<BillEntryMsg> {

    private java.lang.String id;
    private java.lang.String userID;
    private java.lang.String status;
    private java.lang.String remainingCredits;
    private java.lang.String deductedCredits;
    private java.lang.String startTime;
    private java.lang.String endTime;
    private java.util.List<gr.grnet.aquarium.message.avro.gen.ServiceEntryMsg> details;

    /** Creates a new Builder */
    private Builder() {
      super(gr.grnet.aquarium.message.avro.gen.BillEntryMsg.SCHEMA$);
    }
    
    /** Creates a Builder by copying an existing Builder */
    private Builder(gr.grnet.aquarium.message.avro.gen.BillEntryMsg.Builder other) {
      super(other);
    }
    
    /** Creates a Builder by copying an existing BillEntryMsg instance */
    private Builder(gr.grnet.aquarium.message.avro.gen.BillEntryMsg other) {
            super(gr.grnet.aquarium.message.avro.gen.BillEntryMsg.SCHEMA$);
      if (isValidValue(fields()[0], other.id)) {
        this.id = (java.lang.String) data().deepCopy(fields()[0].schema(), other.id);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.userID)) {
        this.userID = (java.lang.String) data().deepCopy(fields()[1].schema(), other.userID);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.status)) {
        this.status = (java.lang.String) data().deepCopy(fields()[2].schema(), other.status);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.remainingCredits)) {
        this.remainingCredits = (java.lang.String) data().deepCopy(fields()[3].schema(), other.remainingCredits);
        fieldSetFlags()[3] = true;
      }
      if (isValidValue(fields()[4], other.deductedCredits)) {
        this.deductedCredits = (java.lang.String) data().deepCopy(fields()[4].schema(), other.deductedCredits);
        fieldSetFlags()[4] = true;
      }
      if (isValidValue(fields()[5], other.startTime)) {
        this.startTime = (java.lang.String) data().deepCopy(fields()[5].schema(), other.startTime);
        fieldSetFlags()[5] = true;
      }
      if (isValidValue(fields()[6], other.endTime)) {
        this.endTime = (java.lang.String) data().deepCopy(fields()[6].schema(), other.endTime);
        fieldSetFlags()[6] = true;
      }
      if (isValidValue(fields()[7], other.details)) {
        this.details = (java.util.List<gr.grnet.aquarium.message.avro.gen.ServiceEntryMsg>) data().deepCopy(fields()[7].schema(), other.details);
        fieldSetFlags()[7] = true;
      }
    }

    /** Gets the value of the 'id' field */
    public java.lang.String getId() {
      return id;
    }
    
    /** Sets the value of the 'id' field */
    public gr.grnet.aquarium.message.avro.gen.BillEntryMsg.Builder setId(java.lang.String value) {
      validate(fields()[0], value);
      this.id = value;
      fieldSetFlags()[0] = true;
      return this; 
    }
    
    /** Checks whether the 'id' field has been set */
    public boolean hasId() {
      return fieldSetFlags()[0];
    }
    
    /** Clears the value of the 'id' field */
    public gr.grnet.aquarium.message.avro.gen.BillEntryMsg.Builder clearId() {
      id = null;
      fieldSetFlags()[0] = false;
      return this;
    }

    /** Gets the value of the 'userID' field */
    public java.lang.String getUserID() {
      return userID;
    }
    
    /** Sets the value of the 'userID' field */
    public gr.grnet.aquarium.message.avro.gen.BillEntryMsg.Builder setUserID(java.lang.String value) {
      validate(fields()[1], value);
      this.userID = value;
      fieldSetFlags()[1] = true;
      return this; 
    }
    
    /** Checks whether the 'userID' field has been set */
    public boolean hasUserID() {
      return fieldSetFlags()[1];
    }
    
    /** Clears the value of the 'userID' field */
    public gr.grnet.aquarium.message.avro.gen.BillEntryMsg.Builder clearUserID() {
      userID = null;
      fieldSetFlags()[1] = false;
      return this;
    }

    /** Gets the value of the 'status' field */
    public java.lang.String getStatus() {
      return status;
    }
    
    /** Sets the value of the 'status' field */
    public gr.grnet.aquarium.message.avro.gen.BillEntryMsg.Builder setStatus(java.lang.String value) {
      validate(fields()[2], value);
      this.status = value;
      fieldSetFlags()[2] = true;
      return this; 
    }
    
    /** Checks whether the 'status' field has been set */
    public boolean hasStatus() {
      return fieldSetFlags()[2];
    }
    
    /** Clears the value of the 'status' field */
    public gr.grnet.aquarium.message.avro.gen.BillEntryMsg.Builder clearStatus() {
      status = null;
      fieldSetFlags()[2] = false;
      return this;
    }

    /** Gets the value of the 'remainingCredits' field */
    public java.lang.String getRemainingCredits() {
      return remainingCredits;
    }
    
    /** Sets the value of the 'remainingCredits' field */
    public gr.grnet.aquarium.message.avro.gen.BillEntryMsg.Builder setRemainingCredits(java.lang.String value) {
      validate(fields()[3], value);
      this.remainingCredits = value;
      fieldSetFlags()[3] = true;
      return this; 
    }
    
    /** Checks whether the 'remainingCredits' field has been set */
    public boolean hasRemainingCredits() {
      return fieldSetFlags()[3];
    }
    
    /** Clears the value of the 'remainingCredits' field */
    public gr.grnet.aquarium.message.avro.gen.BillEntryMsg.Builder clearRemainingCredits() {
      remainingCredits = null;
      fieldSetFlags()[3] = false;
      return this;
    }

    /** Gets the value of the 'deductedCredits' field */
    public java.lang.String getDeductedCredits() {
      return deductedCredits;
    }
    
    /** Sets the value of the 'deductedCredits' field */
    public gr.grnet.aquarium.message.avro.gen.BillEntryMsg.Builder setDeductedCredits(java.lang.String value) {
      validate(fields()[4], value);
      this.deductedCredits = value;
      fieldSetFlags()[4] = true;
      return this; 
    }
    
    /** Checks whether the 'deductedCredits' field has been set */
    public boolean hasDeductedCredits() {
      return fieldSetFlags()[4];
    }
    
    /** Clears the value of the 'deductedCredits' field */
    public gr.grnet.aquarium.message.avro.gen.BillEntryMsg.Builder clearDeductedCredits() {
      deductedCredits = null;
      fieldSetFlags()[4] = false;
      return this;
    }

    /** Gets the value of the 'startTime' field */
    public java.lang.String getStartTime() {
      return startTime;
    }
    
    /** Sets the value of the 'startTime' field */
    public gr.grnet.aquarium.message.avro.gen.BillEntryMsg.Builder setStartTime(java.lang.String value) {
      validate(fields()[5], value);
      this.startTime = value;
      fieldSetFlags()[5] = true;
      return this; 
    }
    
    /** Checks whether the 'startTime' field has been set */
    public boolean hasStartTime() {
      return fieldSetFlags()[5];
    }
    
    /** Clears the value of the 'startTime' field */
    public gr.grnet.aquarium.message.avro.gen.BillEntryMsg.Builder clearStartTime() {
      startTime = null;
      fieldSetFlags()[5] = false;
      return this;
    }

    /** Gets the value of the 'endTime' field */
    public java.lang.String getEndTime() {
      return endTime;
    }
    
    /** Sets the value of the 'endTime' field */
    public gr.grnet.aquarium.message.avro.gen.BillEntryMsg.Builder setEndTime(java.lang.String value) {
      validate(fields()[6], value);
      this.endTime = value;
      fieldSetFlags()[6] = true;
      return this; 
    }
    
    /** Checks whether the 'endTime' field has been set */
    public boolean hasEndTime() {
      return fieldSetFlags()[6];
    }
    
    /** Clears the value of the 'endTime' field */
    public gr.grnet.aquarium.message.avro.gen.BillEntryMsg.Builder clearEndTime() {
      endTime = null;
      fieldSetFlags()[6] = false;
      return this;
    }

    /** Gets the value of the 'details' field */
    public java.util.List<gr.grnet.aquarium.message.avro.gen.ServiceEntryMsg> getDetails() {
      return details;
    }
    
    /** Sets the value of the 'details' field */
    public gr.grnet.aquarium.message.avro.gen.BillEntryMsg.Builder setDetails(java.util.List<gr.grnet.aquarium.message.avro.gen.ServiceEntryMsg> value) {
      validate(fields()[7], value);
      this.details = value;
      fieldSetFlags()[7] = true;
      return this; 
    }
    
    /** Checks whether the 'details' field has been set */
    public boolean hasDetails() {
      return fieldSetFlags()[7];
    }
    
    /** Clears the value of the 'details' field */
    public gr.grnet.aquarium.message.avro.gen.BillEntryMsg.Builder clearDetails() {
      details = null;
      fieldSetFlags()[7] = false;
      return this;
    }

    @Override
    public BillEntryMsg build() {
      try {
        BillEntryMsg record = new BillEntryMsg();
        record.id = fieldSetFlags()[0] ? this.id : (java.lang.String) defaultValue(fields()[0]);
        record.userID = fieldSetFlags()[1] ? this.userID : (java.lang.String) defaultValue(fields()[1]);
        record.status = fieldSetFlags()[2] ? this.status : (java.lang.String) defaultValue(fields()[2]);
        record.remainingCredits = fieldSetFlags()[3] ? this.remainingCredits : (java.lang.String) defaultValue(fields()[3]);
        record.deductedCredits = fieldSetFlags()[4] ? this.deductedCredits : (java.lang.String) defaultValue(fields()[4]);
        record.startTime = fieldSetFlags()[5] ? this.startTime : (java.lang.String) defaultValue(fields()[5]);
        record.endTime = fieldSetFlags()[6] ? this.endTime : (java.lang.String) defaultValue(fields()[6]);
        record.details = fieldSetFlags()[7] ? this.details : (java.util.List<gr.grnet.aquarium.message.avro.gen.ServiceEntryMsg>) defaultValue(fields()[7]);
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }
}
