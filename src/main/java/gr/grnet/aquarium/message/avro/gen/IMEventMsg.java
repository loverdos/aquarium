/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package gr.grnet.aquarium.message.avro.gen;  
@SuppressWarnings("all")
public class IMEventMsg extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"IMEventMsg\",\"namespace\":\"gr.grnet.aquarium.message.avro.gen\",\"fields\":[{\"name\":\"originalID\",\"type\":\"string\",\"aliases\":[\"id\"]},{\"name\":\"inStoreID\",\"type\":[\"string\",\"null\"],\"aliases\":[\"_id\",\"idInStore\"]},{\"name\":\"occurredMillis\",\"type\":\"long\"},{\"name\":\"receivedMillis\",\"type\":\"long\",\"default\":0},{\"name\":\"userID\",\"type\":\"string\"},{\"name\":\"clientID\",\"type\":\"string\"},{\"name\":\"eventVersion\",\"type\":\"string\",\"default\":\"1.0\"},{\"name\":\"isActive\",\"type\":\"boolean\"},{\"name\":\"role\",\"type\":\"string\"},{\"name\":\"details\",\"type\":{\"type\":\"map\",\"values\":{\"type\":\"record\",\"name\":\"AnyValueMsg\",\"fields\":[{\"name\":\"anyValue\",\"type\":[\"null\",\"int\",\"long\",\"boolean\",\"double\",\"bytes\",\"string\"]}]}}}]}");
  @Deprecated public java.lang.CharSequence originalID;
  @Deprecated public java.lang.CharSequence inStoreID;
  @Deprecated public long occurredMillis;
  @Deprecated public long receivedMillis;
  @Deprecated public java.lang.CharSequence userID;
  @Deprecated public java.lang.CharSequence clientID;
  @Deprecated public java.lang.CharSequence eventVersion;
  @Deprecated public boolean isActive;
  @Deprecated public java.lang.CharSequence role;
  @Deprecated public java.util.Map<java.lang.CharSequence,gr.grnet.aquarium.message.avro.gen.AnyValueMsg> details;
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call. 
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return originalID;
    case 1: return inStoreID;
    case 2: return occurredMillis;
    case 3: return receivedMillis;
    case 4: return userID;
    case 5: return clientID;
    case 6: return eventVersion;
    case 7: return isActive;
    case 8: return role;
    case 9: return details;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  // Used by DatumReader.  Applications should not call. 
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: originalID = (java.lang.CharSequence)value$; break;
    case 1: inStoreID = (java.lang.CharSequence)value$; break;
    case 2: occurredMillis = (java.lang.Long)value$; break;
    case 3: receivedMillis = (java.lang.Long)value$; break;
    case 4: userID = (java.lang.CharSequence)value$; break;
    case 5: clientID = (java.lang.CharSequence)value$; break;
    case 6: eventVersion = (java.lang.CharSequence)value$; break;
    case 7: isActive = (java.lang.Boolean)value$; break;
    case 8: role = (java.lang.CharSequence)value$; break;
    case 9: details = (java.util.Map<java.lang.CharSequence,gr.grnet.aquarium.message.avro.gen.AnyValueMsg>)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'originalID' field.
   */
  public java.lang.CharSequence getOriginalID() {
    return originalID;
  }

  /**
   * Sets the value of the 'originalID' field.
   * @param value the value to set.
   */
  public void setOriginalID(java.lang.CharSequence value) {
    this.originalID = value;
  }

  /**
   * Gets the value of the 'inStoreID' field.
   */
  public java.lang.CharSequence getInStoreID() {
    return inStoreID;
  }

  /**
   * Sets the value of the 'inStoreID' field.
   * @param value the value to set.
   */
  public void setInStoreID(java.lang.CharSequence value) {
    this.inStoreID = value;
  }

  /**
   * Gets the value of the 'occurredMillis' field.
   */
  public java.lang.Long getOccurredMillis() {
    return occurredMillis;
  }

  /**
   * Sets the value of the 'occurredMillis' field.
   * @param value the value to set.
   */
  public void setOccurredMillis(java.lang.Long value) {
    this.occurredMillis = value;
  }

  /**
   * Gets the value of the 'receivedMillis' field.
   */
  public java.lang.Long getReceivedMillis() {
    return receivedMillis;
  }

  /**
   * Sets the value of the 'receivedMillis' field.
   * @param value the value to set.
   */
  public void setReceivedMillis(java.lang.Long value) {
    this.receivedMillis = value;
  }

  /**
   * Gets the value of the 'userID' field.
   */
  public java.lang.CharSequence getUserID() {
    return userID;
  }

  /**
   * Sets the value of the 'userID' field.
   * @param value the value to set.
   */
  public void setUserID(java.lang.CharSequence value) {
    this.userID = value;
  }

  /**
   * Gets the value of the 'clientID' field.
   */
  public java.lang.CharSequence getClientID() {
    return clientID;
  }

  /**
   * Sets the value of the 'clientID' field.
   * @param value the value to set.
   */
  public void setClientID(java.lang.CharSequence value) {
    this.clientID = value;
  }

  /**
   * Gets the value of the 'eventVersion' field.
   */
  public java.lang.CharSequence getEventVersion() {
    return eventVersion;
  }

  /**
   * Sets the value of the 'eventVersion' field.
   * @param value the value to set.
   */
  public void setEventVersion(java.lang.CharSequence value) {
    this.eventVersion = value;
  }

  /**
   * Gets the value of the 'isActive' field.
   */
  public java.lang.Boolean getIsActive() {
    return isActive;
  }

  /**
   * Sets the value of the 'isActive' field.
   * @param value the value to set.
   */
  public void setIsActive(java.lang.Boolean value) {
    this.isActive = value;
  }

  /**
   * Gets the value of the 'role' field.
   */
  public java.lang.CharSequence getRole() {
    return role;
  }

  /**
   * Sets the value of the 'role' field.
   * @param value the value to set.
   */
  public void setRole(java.lang.CharSequence value) {
    this.role = value;
  }

  /**
   * Gets the value of the 'details' field.
   */
  public java.util.Map<java.lang.CharSequence,gr.grnet.aquarium.message.avro.gen.AnyValueMsg> getDetails() {
    return details;
  }

  /**
   * Sets the value of the 'details' field.
   * @param value the value to set.
   */
  public void setDetails(java.util.Map<java.lang.CharSequence,gr.grnet.aquarium.message.avro.gen.AnyValueMsg> value) {
    this.details = value;
  }

  /** Creates a new IMEventMsg RecordBuilder */
  public static gr.grnet.aquarium.message.avro.gen.IMEventMsg.Builder newBuilder() {
    return new gr.grnet.aquarium.message.avro.gen.IMEventMsg.Builder();
  }
  
  /** Creates a new IMEventMsg RecordBuilder by copying an existing Builder */
  public static gr.grnet.aquarium.message.avro.gen.IMEventMsg.Builder newBuilder(gr.grnet.aquarium.message.avro.gen.IMEventMsg.Builder other) {
    return new gr.grnet.aquarium.message.avro.gen.IMEventMsg.Builder(other);
  }
  
  /** Creates a new IMEventMsg RecordBuilder by copying an existing IMEventMsg instance */
  public static gr.grnet.aquarium.message.avro.gen.IMEventMsg.Builder newBuilder(gr.grnet.aquarium.message.avro.gen.IMEventMsg other) {
    return new gr.grnet.aquarium.message.avro.gen.IMEventMsg.Builder(other);
  }
  
  /**
   * RecordBuilder for IMEventMsg instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<IMEventMsg>
    implements org.apache.avro.data.RecordBuilder<IMEventMsg> {

    private java.lang.CharSequence originalID;
    private java.lang.CharSequence inStoreID;
    private long occurredMillis;
    private long receivedMillis;
    private java.lang.CharSequence userID;
    private java.lang.CharSequence clientID;
    private java.lang.CharSequence eventVersion;
    private boolean isActive;
    private java.lang.CharSequence role;
    private java.util.Map<java.lang.CharSequence,gr.grnet.aquarium.message.avro.gen.AnyValueMsg> details;

    /** Creates a new Builder */
    private Builder() {
      super(gr.grnet.aquarium.message.avro.gen.IMEventMsg.SCHEMA$);
    }
    
    /** Creates a Builder by copying an existing Builder */
    private Builder(gr.grnet.aquarium.message.avro.gen.IMEventMsg.Builder other) {
      super(other);
    }
    
    /** Creates a Builder by copying an existing IMEventMsg instance */
    private Builder(gr.grnet.aquarium.message.avro.gen.IMEventMsg other) {
            super(gr.grnet.aquarium.message.avro.gen.IMEventMsg.SCHEMA$);
      if (isValidValue(fields()[0], other.originalID)) {
        this.originalID = (java.lang.CharSequence) data().deepCopy(fields()[0].schema(), other.originalID);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.inStoreID)) {
        this.inStoreID = (java.lang.CharSequence) data().deepCopy(fields()[1].schema(), other.inStoreID);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.occurredMillis)) {
        this.occurredMillis = (java.lang.Long) data().deepCopy(fields()[2].schema(), other.occurredMillis);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.receivedMillis)) {
        this.receivedMillis = (java.lang.Long) data().deepCopy(fields()[3].schema(), other.receivedMillis);
        fieldSetFlags()[3] = true;
      }
      if (isValidValue(fields()[4], other.userID)) {
        this.userID = (java.lang.CharSequence) data().deepCopy(fields()[4].schema(), other.userID);
        fieldSetFlags()[4] = true;
      }
      if (isValidValue(fields()[5], other.clientID)) {
        this.clientID = (java.lang.CharSequence) data().deepCopy(fields()[5].schema(), other.clientID);
        fieldSetFlags()[5] = true;
      }
      if (isValidValue(fields()[6], other.eventVersion)) {
        this.eventVersion = (java.lang.CharSequence) data().deepCopy(fields()[6].schema(), other.eventVersion);
        fieldSetFlags()[6] = true;
      }
      if (isValidValue(fields()[7], other.isActive)) {
        this.isActive = (java.lang.Boolean) data().deepCopy(fields()[7].schema(), other.isActive);
        fieldSetFlags()[7] = true;
      }
      if (isValidValue(fields()[8], other.role)) {
        this.role = (java.lang.CharSequence) data().deepCopy(fields()[8].schema(), other.role);
        fieldSetFlags()[8] = true;
      }
      if (isValidValue(fields()[9], other.details)) {
        this.details = (java.util.Map<java.lang.CharSequence,gr.grnet.aquarium.message.avro.gen.AnyValueMsg>) data().deepCopy(fields()[9].schema(), other.details);
        fieldSetFlags()[9] = true;
      }
    }

    /** Gets the value of the 'originalID' field */
    public java.lang.CharSequence getOriginalID() {
      return originalID;
    }
    
    /** Sets the value of the 'originalID' field */
    public gr.grnet.aquarium.message.avro.gen.IMEventMsg.Builder setOriginalID(java.lang.CharSequence value) {
      validate(fields()[0], value);
      this.originalID = value;
      fieldSetFlags()[0] = true;
      return this; 
    }
    
    /** Checks whether the 'originalID' field has been set */
    public boolean hasOriginalID() {
      return fieldSetFlags()[0];
    }
    
    /** Clears the value of the 'originalID' field */
    public gr.grnet.aquarium.message.avro.gen.IMEventMsg.Builder clearOriginalID() {
      originalID = null;
      fieldSetFlags()[0] = false;
      return this;
    }

    /** Gets the value of the 'inStoreID' field */
    public java.lang.CharSequence getInStoreID() {
      return inStoreID;
    }
    
    /** Sets the value of the 'inStoreID' field */
    public gr.grnet.aquarium.message.avro.gen.IMEventMsg.Builder setInStoreID(java.lang.CharSequence value) {
      validate(fields()[1], value);
      this.inStoreID = value;
      fieldSetFlags()[1] = true;
      return this; 
    }
    
    /** Checks whether the 'inStoreID' field has been set */
    public boolean hasInStoreID() {
      return fieldSetFlags()[1];
    }
    
    /** Clears the value of the 'inStoreID' field */
    public gr.grnet.aquarium.message.avro.gen.IMEventMsg.Builder clearInStoreID() {
      inStoreID = null;
      fieldSetFlags()[1] = false;
      return this;
    }

    /** Gets the value of the 'occurredMillis' field */
    public java.lang.Long getOccurredMillis() {
      return occurredMillis;
    }
    
    /** Sets the value of the 'occurredMillis' field */
    public gr.grnet.aquarium.message.avro.gen.IMEventMsg.Builder setOccurredMillis(long value) {
      validate(fields()[2], value);
      this.occurredMillis = value;
      fieldSetFlags()[2] = true;
      return this; 
    }
    
    /** Checks whether the 'occurredMillis' field has been set */
    public boolean hasOccurredMillis() {
      return fieldSetFlags()[2];
    }
    
    /** Clears the value of the 'occurredMillis' field */
    public gr.grnet.aquarium.message.avro.gen.IMEventMsg.Builder clearOccurredMillis() {
      fieldSetFlags()[2] = false;
      return this;
    }

    /** Gets the value of the 'receivedMillis' field */
    public java.lang.Long getReceivedMillis() {
      return receivedMillis;
    }
    
    /** Sets the value of the 'receivedMillis' field */
    public gr.grnet.aquarium.message.avro.gen.IMEventMsg.Builder setReceivedMillis(long value) {
      validate(fields()[3], value);
      this.receivedMillis = value;
      fieldSetFlags()[3] = true;
      return this; 
    }
    
    /** Checks whether the 'receivedMillis' field has been set */
    public boolean hasReceivedMillis() {
      return fieldSetFlags()[3];
    }
    
    /** Clears the value of the 'receivedMillis' field */
    public gr.grnet.aquarium.message.avro.gen.IMEventMsg.Builder clearReceivedMillis() {
      fieldSetFlags()[3] = false;
      return this;
    }

    /** Gets the value of the 'userID' field */
    public java.lang.CharSequence getUserID() {
      return userID;
    }
    
    /** Sets the value of the 'userID' field */
    public gr.grnet.aquarium.message.avro.gen.IMEventMsg.Builder setUserID(java.lang.CharSequence value) {
      validate(fields()[4], value);
      this.userID = value;
      fieldSetFlags()[4] = true;
      return this; 
    }
    
    /** Checks whether the 'userID' field has been set */
    public boolean hasUserID() {
      return fieldSetFlags()[4];
    }
    
    /** Clears the value of the 'userID' field */
    public gr.grnet.aquarium.message.avro.gen.IMEventMsg.Builder clearUserID() {
      userID = null;
      fieldSetFlags()[4] = false;
      return this;
    }

    /** Gets the value of the 'clientID' field */
    public java.lang.CharSequence getClientID() {
      return clientID;
    }
    
    /** Sets the value of the 'clientID' field */
    public gr.grnet.aquarium.message.avro.gen.IMEventMsg.Builder setClientID(java.lang.CharSequence value) {
      validate(fields()[5], value);
      this.clientID = value;
      fieldSetFlags()[5] = true;
      return this; 
    }
    
    /** Checks whether the 'clientID' field has been set */
    public boolean hasClientID() {
      return fieldSetFlags()[5];
    }
    
    /** Clears the value of the 'clientID' field */
    public gr.grnet.aquarium.message.avro.gen.IMEventMsg.Builder clearClientID() {
      clientID = null;
      fieldSetFlags()[5] = false;
      return this;
    }

    /** Gets the value of the 'eventVersion' field */
    public java.lang.CharSequence getEventVersion() {
      return eventVersion;
    }
    
    /** Sets the value of the 'eventVersion' field */
    public gr.grnet.aquarium.message.avro.gen.IMEventMsg.Builder setEventVersion(java.lang.CharSequence value) {
      validate(fields()[6], value);
      this.eventVersion = value;
      fieldSetFlags()[6] = true;
      return this; 
    }
    
    /** Checks whether the 'eventVersion' field has been set */
    public boolean hasEventVersion() {
      return fieldSetFlags()[6];
    }
    
    /** Clears the value of the 'eventVersion' field */
    public gr.grnet.aquarium.message.avro.gen.IMEventMsg.Builder clearEventVersion() {
      eventVersion = null;
      fieldSetFlags()[6] = false;
      return this;
    }

    /** Gets the value of the 'isActive' field */
    public java.lang.Boolean getIsActive() {
      return isActive;
    }
    
    /** Sets the value of the 'isActive' field */
    public gr.grnet.aquarium.message.avro.gen.IMEventMsg.Builder setIsActive(boolean value) {
      validate(fields()[7], value);
      this.isActive = value;
      fieldSetFlags()[7] = true;
      return this; 
    }
    
    /** Checks whether the 'isActive' field has been set */
    public boolean hasIsActive() {
      return fieldSetFlags()[7];
    }
    
    /** Clears the value of the 'isActive' field */
    public gr.grnet.aquarium.message.avro.gen.IMEventMsg.Builder clearIsActive() {
      fieldSetFlags()[7] = false;
      return this;
    }

    /** Gets the value of the 'role' field */
    public java.lang.CharSequence getRole() {
      return role;
    }
    
    /** Sets the value of the 'role' field */
    public gr.grnet.aquarium.message.avro.gen.IMEventMsg.Builder setRole(java.lang.CharSequence value) {
      validate(fields()[8], value);
      this.role = value;
      fieldSetFlags()[8] = true;
      return this; 
    }
    
    /** Checks whether the 'role' field has been set */
    public boolean hasRole() {
      return fieldSetFlags()[8];
    }
    
    /** Clears the value of the 'role' field */
    public gr.grnet.aquarium.message.avro.gen.IMEventMsg.Builder clearRole() {
      role = null;
      fieldSetFlags()[8] = false;
      return this;
    }

    /** Gets the value of the 'details' field */
    public java.util.Map<java.lang.CharSequence,gr.grnet.aquarium.message.avro.gen.AnyValueMsg> getDetails() {
      return details;
    }
    
    /** Sets the value of the 'details' field */
    public gr.grnet.aquarium.message.avro.gen.IMEventMsg.Builder setDetails(java.util.Map<java.lang.CharSequence,gr.grnet.aquarium.message.avro.gen.AnyValueMsg> value) {
      validate(fields()[9], value);
      this.details = value;
      fieldSetFlags()[9] = true;
      return this; 
    }
    
    /** Checks whether the 'details' field has been set */
    public boolean hasDetails() {
      return fieldSetFlags()[9];
    }
    
    /** Clears the value of the 'details' field */
    public gr.grnet.aquarium.message.avro.gen.IMEventMsg.Builder clearDetails() {
      details = null;
      fieldSetFlags()[9] = false;
      return this;
    }

    @Override
    public IMEventMsg build() {
      try {
        IMEventMsg record = new IMEventMsg();
        record.originalID = fieldSetFlags()[0] ? this.originalID : (java.lang.CharSequence) defaultValue(fields()[0]);
        record.inStoreID = fieldSetFlags()[1] ? this.inStoreID : (java.lang.CharSequence) defaultValue(fields()[1]);
        record.occurredMillis = fieldSetFlags()[2] ? this.occurredMillis : (java.lang.Long) defaultValue(fields()[2]);
        record.receivedMillis = fieldSetFlags()[3] ? this.receivedMillis : (java.lang.Long) defaultValue(fields()[3]);
        record.userID = fieldSetFlags()[4] ? this.userID : (java.lang.CharSequence) defaultValue(fields()[4]);
        record.clientID = fieldSetFlags()[5] ? this.clientID : (java.lang.CharSequence) defaultValue(fields()[5]);
        record.eventVersion = fieldSetFlags()[6] ? this.eventVersion : (java.lang.CharSequence) defaultValue(fields()[6]);
        record.isActive = fieldSetFlags()[7] ? this.isActive : (java.lang.Boolean) defaultValue(fields()[7]);
        record.role = fieldSetFlags()[8] ? this.role : (java.lang.CharSequence) defaultValue(fields()[8]);
        record.details = fieldSetFlags()[9] ? this.details : (java.util.Map<java.lang.CharSequence,gr.grnet.aquarium.message.avro.gen.AnyValueMsg>) defaultValue(fields()[9]);
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }
}
