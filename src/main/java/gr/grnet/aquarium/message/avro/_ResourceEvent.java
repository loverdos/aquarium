/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package gr.grnet.aquarium.message.avro;  
@SuppressWarnings("all")
public class _ResourceEvent extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"_ResourceEvent\",\"namespace\":\"gr.grnet.aquarium.message.avro\",\"fields\":[{\"name\":\"id\",\"type\":\"string\",\"aliases\":[\"originalID\",\"ID\"]},{\"name\":\"idInStore\",\"type\":\"string\"},{\"name\":\"occurredMillis\",\"type\":\"long\"},{\"name\":\"receivedMillis\",\"type\":\"long\",\"default\":0},{\"name\":\"userID\",\"type\":\"string\"},{\"name\":\"clientID\",\"type\":\"string\"},{\"name\":\"eventVersion\",\"type\":\"string\",\"default\":\"1.0\"},{\"name\":\"resource\",\"type\":\"string\",\"aliases\":[\"resourceType\"]},{\"name\":\"instanceID\",\"type\":\"string\"},{\"name\":\"value\",\"type\":\"string\"},{\"name\":\"details\",\"type\":{\"type\":\"map\",\"values\":{\"type\":\"record\",\"name\":\"_AnyValue\",\"fields\":[{\"name\":\"anyValue\",\"type\":[\"null\",\"int\",\"long\",\"boolean\",\"double\",\"bytes\",\"string\"]}]}}}]}");
  @Deprecated public java.lang.CharSequence id;
  @Deprecated public java.lang.CharSequence idInStore;
  @Deprecated public long occurredMillis;
  @Deprecated public long receivedMillis;
  @Deprecated public java.lang.CharSequence userID;
  @Deprecated public java.lang.CharSequence clientID;
  @Deprecated public java.lang.CharSequence eventVersion;
  @Deprecated public java.lang.CharSequence resource;
  @Deprecated public java.lang.CharSequence instanceID;
  @Deprecated public java.lang.CharSequence value;
  @Deprecated public java.util.Map<java.lang.CharSequence,gr.grnet.aquarium.message.avro._AnyValue> details;
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call. 
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return id;
    case 1: return idInStore;
    case 2: return occurredMillis;
    case 3: return receivedMillis;
    case 4: return userID;
    case 5: return clientID;
    case 6: return eventVersion;
    case 7: return resource;
    case 8: return instanceID;
    case 9: return value;
    case 10: return details;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  // Used by DatumReader.  Applications should not call. 
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: id = (java.lang.CharSequence)value$; break;
    case 1: idInStore = (java.lang.CharSequence)value$; break;
    case 2: occurredMillis = (java.lang.Long)value$; break;
    case 3: receivedMillis = (java.lang.Long)value$; break;
    case 4: userID = (java.lang.CharSequence)value$; break;
    case 5: clientID = (java.lang.CharSequence)value$; break;
    case 6: eventVersion = (java.lang.CharSequence)value$; break;
    case 7: resource = (java.lang.CharSequence)value$; break;
    case 8: instanceID = (java.lang.CharSequence)value$; break;
    case 9: value = (java.lang.CharSequence)value$; break;
    case 10: details = (java.util.Map<java.lang.CharSequence,gr.grnet.aquarium.message.avro._AnyValue>)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'id' field.
   */
  public java.lang.CharSequence getId() {
    return id;
  }

  /**
   * Sets the value of the 'id' field.
   * @param value the value to set.
   */
  public void setId(java.lang.CharSequence value) {
    this.id = value;
  }

  /**
   * Gets the value of the 'idInStore' field.
   */
  public java.lang.CharSequence getIdInStore() {
    return idInStore;
  }

  /**
   * Sets the value of the 'idInStore' field.
   * @param value the value to set.
   */
  public void setIdInStore(java.lang.CharSequence value) {
    this.idInStore = value;
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
   * Gets the value of the 'resource' field.
   */
  public java.lang.CharSequence getResource() {
    return resource;
  }

  /**
   * Sets the value of the 'resource' field.
   * @param value the value to set.
   */
  public void setResource(java.lang.CharSequence value) {
    this.resource = value;
  }

  /**
   * Gets the value of the 'instanceID' field.
   */
  public java.lang.CharSequence getInstanceID() {
    return instanceID;
  }

  /**
   * Sets the value of the 'instanceID' field.
   * @param value the value to set.
   */
  public void setInstanceID(java.lang.CharSequence value) {
    this.instanceID = value;
  }

  /**
   * Gets the value of the 'value' field.
   */
  public java.lang.CharSequence getValue() {
    return value;
  }

  /**
   * Sets the value of the 'value' field.
   * @param value the value to set.
   */
  public void setValue(java.lang.CharSequence value) {
    this.value = value;
  }

  /**
   * Gets the value of the 'details' field.
   */
  public java.util.Map<java.lang.CharSequence,gr.grnet.aquarium.message.avro._AnyValue> getDetails() {
    return details;
  }

  /**
   * Sets the value of the 'details' field.
   * @param value the value to set.
   */
  public void setDetails(java.util.Map<java.lang.CharSequence,gr.grnet.aquarium.message.avro._AnyValue> value) {
    this.details = value;
  }

  /** Creates a new _ResourceEvent RecordBuilder */
  public static gr.grnet.aquarium.message.avro._ResourceEvent.Builder newBuilder() {
    return new gr.grnet.aquarium.message.avro._ResourceEvent.Builder();
  }
  
  /** Creates a new _ResourceEvent RecordBuilder by copying an existing Builder */
  public static gr.grnet.aquarium.message.avro._ResourceEvent.Builder newBuilder(gr.grnet.aquarium.message.avro._ResourceEvent.Builder other) {
    return new gr.grnet.aquarium.message.avro._ResourceEvent.Builder(other);
  }
  
  /** Creates a new _ResourceEvent RecordBuilder by copying an existing _ResourceEvent instance */
  public static gr.grnet.aquarium.message.avro._ResourceEvent.Builder newBuilder(gr.grnet.aquarium.message.avro._ResourceEvent other) {
    return new gr.grnet.aquarium.message.avro._ResourceEvent.Builder(other);
  }
  
  /**
   * RecordBuilder for _ResourceEvent instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<_ResourceEvent>
    implements org.apache.avro.data.RecordBuilder<_ResourceEvent> {

    private java.lang.CharSequence id;
    private java.lang.CharSequence idInStore;
    private long occurredMillis;
    private long receivedMillis;
    private java.lang.CharSequence userID;
    private java.lang.CharSequence clientID;
    private java.lang.CharSequence eventVersion;
    private java.lang.CharSequence resource;
    private java.lang.CharSequence instanceID;
    private java.lang.CharSequence value;
    private java.util.Map<java.lang.CharSequence,gr.grnet.aquarium.message.avro._AnyValue> details;

    /** Creates a new Builder */
    private Builder() {
      super(gr.grnet.aquarium.message.avro._ResourceEvent.SCHEMA$);
    }
    
    /** Creates a Builder by copying an existing Builder */
    private Builder(gr.grnet.aquarium.message.avro._ResourceEvent.Builder other) {
      super(other);
    }
    
    /** Creates a Builder by copying an existing _ResourceEvent instance */
    private Builder(gr.grnet.aquarium.message.avro._ResourceEvent other) {
            super(gr.grnet.aquarium.message.avro._ResourceEvent.SCHEMA$);
      if (isValidValue(fields()[0], other.id)) {
        this.id = (java.lang.CharSequence) data().deepCopy(fields()[0].schema(), other.id);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.idInStore)) {
        this.idInStore = (java.lang.CharSequence) data().deepCopy(fields()[1].schema(), other.idInStore);
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
      if (isValidValue(fields()[7], other.resource)) {
        this.resource = (java.lang.CharSequence) data().deepCopy(fields()[7].schema(), other.resource);
        fieldSetFlags()[7] = true;
      }
      if (isValidValue(fields()[8], other.instanceID)) {
        this.instanceID = (java.lang.CharSequence) data().deepCopy(fields()[8].schema(), other.instanceID);
        fieldSetFlags()[8] = true;
      }
      if (isValidValue(fields()[9], other.value)) {
        this.value = (java.lang.CharSequence) data().deepCopy(fields()[9].schema(), other.value);
        fieldSetFlags()[9] = true;
      }
      if (isValidValue(fields()[10], other.details)) {
        this.details = (java.util.Map<java.lang.CharSequence,gr.grnet.aquarium.message.avro._AnyValue>) data().deepCopy(fields()[10].schema(), other.details);
        fieldSetFlags()[10] = true;
      }
    }

    /** Gets the value of the 'id' field */
    public java.lang.CharSequence getId() {
      return id;
    }
    
    /** Sets the value of the 'id' field */
    public gr.grnet.aquarium.message.avro._ResourceEvent.Builder setId(java.lang.CharSequence value) {
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
    public gr.grnet.aquarium.message.avro._ResourceEvent.Builder clearId() {
      id = null;
      fieldSetFlags()[0] = false;
      return this;
    }

    /** Gets the value of the 'idInStore' field */
    public java.lang.CharSequence getIdInStore() {
      return idInStore;
    }
    
    /** Sets the value of the 'idInStore' field */
    public gr.grnet.aquarium.message.avro._ResourceEvent.Builder setIdInStore(java.lang.CharSequence value) {
      validate(fields()[1], value);
      this.idInStore = value;
      fieldSetFlags()[1] = true;
      return this; 
    }
    
    /** Checks whether the 'idInStore' field has been set */
    public boolean hasIdInStore() {
      return fieldSetFlags()[1];
    }
    
    /** Clears the value of the 'idInStore' field */
    public gr.grnet.aquarium.message.avro._ResourceEvent.Builder clearIdInStore() {
      idInStore = null;
      fieldSetFlags()[1] = false;
      return this;
    }

    /** Gets the value of the 'occurredMillis' field */
    public java.lang.Long getOccurredMillis() {
      return occurredMillis;
    }
    
    /** Sets the value of the 'occurredMillis' field */
    public gr.grnet.aquarium.message.avro._ResourceEvent.Builder setOccurredMillis(long value) {
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
    public gr.grnet.aquarium.message.avro._ResourceEvent.Builder clearOccurredMillis() {
      fieldSetFlags()[2] = false;
      return this;
    }

    /** Gets the value of the 'receivedMillis' field */
    public java.lang.Long getReceivedMillis() {
      return receivedMillis;
    }
    
    /** Sets the value of the 'receivedMillis' field */
    public gr.grnet.aquarium.message.avro._ResourceEvent.Builder setReceivedMillis(long value) {
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
    public gr.grnet.aquarium.message.avro._ResourceEvent.Builder clearReceivedMillis() {
      fieldSetFlags()[3] = false;
      return this;
    }

    /** Gets the value of the 'userID' field */
    public java.lang.CharSequence getUserID() {
      return userID;
    }
    
    /** Sets the value of the 'userID' field */
    public gr.grnet.aquarium.message.avro._ResourceEvent.Builder setUserID(java.lang.CharSequence value) {
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
    public gr.grnet.aquarium.message.avro._ResourceEvent.Builder clearUserID() {
      userID = null;
      fieldSetFlags()[4] = false;
      return this;
    }

    /** Gets the value of the 'clientID' field */
    public java.lang.CharSequence getClientID() {
      return clientID;
    }
    
    /** Sets the value of the 'clientID' field */
    public gr.grnet.aquarium.message.avro._ResourceEvent.Builder setClientID(java.lang.CharSequence value) {
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
    public gr.grnet.aquarium.message.avro._ResourceEvent.Builder clearClientID() {
      clientID = null;
      fieldSetFlags()[5] = false;
      return this;
    }

    /** Gets the value of the 'eventVersion' field */
    public java.lang.CharSequence getEventVersion() {
      return eventVersion;
    }
    
    /** Sets the value of the 'eventVersion' field */
    public gr.grnet.aquarium.message.avro._ResourceEvent.Builder setEventVersion(java.lang.CharSequence value) {
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
    public gr.grnet.aquarium.message.avro._ResourceEvent.Builder clearEventVersion() {
      eventVersion = null;
      fieldSetFlags()[6] = false;
      return this;
    }

    /** Gets the value of the 'resource' field */
    public java.lang.CharSequence getResource() {
      return resource;
    }
    
    /** Sets the value of the 'resource' field */
    public gr.grnet.aquarium.message.avro._ResourceEvent.Builder setResource(java.lang.CharSequence value) {
      validate(fields()[7], value);
      this.resource = value;
      fieldSetFlags()[7] = true;
      return this; 
    }
    
    /** Checks whether the 'resource' field has been set */
    public boolean hasResource() {
      return fieldSetFlags()[7];
    }
    
    /** Clears the value of the 'resource' field */
    public gr.grnet.aquarium.message.avro._ResourceEvent.Builder clearResource() {
      resource = null;
      fieldSetFlags()[7] = false;
      return this;
    }

    /** Gets the value of the 'instanceID' field */
    public java.lang.CharSequence getInstanceID() {
      return instanceID;
    }
    
    /** Sets the value of the 'instanceID' field */
    public gr.grnet.aquarium.message.avro._ResourceEvent.Builder setInstanceID(java.lang.CharSequence value) {
      validate(fields()[8], value);
      this.instanceID = value;
      fieldSetFlags()[8] = true;
      return this; 
    }
    
    /** Checks whether the 'instanceID' field has been set */
    public boolean hasInstanceID() {
      return fieldSetFlags()[8];
    }
    
    /** Clears the value of the 'instanceID' field */
    public gr.grnet.aquarium.message.avro._ResourceEvent.Builder clearInstanceID() {
      instanceID = null;
      fieldSetFlags()[8] = false;
      return this;
    }

    /** Gets the value of the 'value' field */
    public java.lang.CharSequence getValue() {
      return value;
    }
    
    /** Sets the value of the 'value' field */
    public gr.grnet.aquarium.message.avro._ResourceEvent.Builder setValue(java.lang.CharSequence value) {
      validate(fields()[9], value);
      this.value = value;
      fieldSetFlags()[9] = true;
      return this; 
    }
    
    /** Checks whether the 'value' field has been set */
    public boolean hasValue() {
      return fieldSetFlags()[9];
    }
    
    /** Clears the value of the 'value' field */
    public gr.grnet.aquarium.message.avro._ResourceEvent.Builder clearValue() {
      value = null;
      fieldSetFlags()[9] = false;
      return this;
    }

    /** Gets the value of the 'details' field */
    public java.util.Map<java.lang.CharSequence,gr.grnet.aquarium.message.avro._AnyValue> getDetails() {
      return details;
    }
    
    /** Sets the value of the 'details' field */
    public gr.grnet.aquarium.message.avro._ResourceEvent.Builder setDetails(java.util.Map<java.lang.CharSequence,gr.grnet.aquarium.message.avro._AnyValue> value) {
      validate(fields()[10], value);
      this.details = value;
      fieldSetFlags()[10] = true;
      return this; 
    }
    
    /** Checks whether the 'details' field has been set */
    public boolean hasDetails() {
      return fieldSetFlags()[10];
    }
    
    /** Clears the value of the 'details' field */
    public gr.grnet.aquarium.message.avro._ResourceEvent.Builder clearDetails() {
      details = null;
      fieldSetFlags()[10] = false;
      return this;
    }

    @Override
    public _ResourceEvent build() {
      try {
        _ResourceEvent record = new _ResourceEvent();
        record.id = fieldSetFlags()[0] ? this.id : (java.lang.CharSequence) defaultValue(fields()[0]);
        record.idInStore = fieldSetFlags()[1] ? this.idInStore : (java.lang.CharSequence) defaultValue(fields()[1]);
        record.occurredMillis = fieldSetFlags()[2] ? this.occurredMillis : (java.lang.Long) defaultValue(fields()[2]);
        record.receivedMillis = fieldSetFlags()[3] ? this.receivedMillis : (java.lang.Long) defaultValue(fields()[3]);
        record.userID = fieldSetFlags()[4] ? this.userID : (java.lang.CharSequence) defaultValue(fields()[4]);
        record.clientID = fieldSetFlags()[5] ? this.clientID : (java.lang.CharSequence) defaultValue(fields()[5]);
        record.eventVersion = fieldSetFlags()[6] ? this.eventVersion : (java.lang.CharSequence) defaultValue(fields()[6]);
        record.resource = fieldSetFlags()[7] ? this.resource : (java.lang.CharSequence) defaultValue(fields()[7]);
        record.instanceID = fieldSetFlags()[8] ? this.instanceID : (java.lang.CharSequence) defaultValue(fields()[8]);
        record.value = fieldSetFlags()[9] ? this.value : (java.lang.CharSequence) defaultValue(fields()[9]);
        record.details = fieldSetFlags()[10] ? this.details : (java.util.Map<java.lang.CharSequence,gr.grnet.aquarium.message.avro._AnyValue>) defaultValue(fields()[10]);
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }
}
