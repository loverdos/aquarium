/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package gr.grnet.aquarium.message.avro.gen;  
@SuppressWarnings("all")
public class UserAgreementHistoryMsg extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"UserAgreementHistoryMsg\",\"namespace\":\"gr.grnet.aquarium.message.avro.gen\",\"fields\":[{\"name\":\"originalID\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"inStoreID\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"userID\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"latestOccurredMillis\",\"type\":\"long\"},{\"name\":\"latestValidFromMillis\",\"type\":\"long\"},{\"name\":\"userCreationTimeMillis\",\"type\":\"long\",\"default\":0},{\"name\":\"agreements\",\"type\":{\"type\":\"array\",\"items\":{\"type\":\"record\",\"name\":\"UserAgreementMsg\",\"fields\":[{\"name\":\"id\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"relatedIMEventOriginalID\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"userID\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"occurredMillis\",\"type\":\"long\",\"default\":0},{\"name\":\"validFromMillis\",\"type\":\"long\"},{\"name\":\"validToMillis\",\"type\":\"long\"},{\"name\":\"role\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"fullPriceTableRef\",\"type\":[{\"type\":\"record\",\"name\":\"FullPriceTableMsg\",\"fields\":[{\"name\":\"perResource\",\"type\":{\"type\":\"map\",\"values\":{\"type\":\"map\",\"values\":{\"type\":\"record\",\"name\":\"SelectorValueMsg\",\"fields\":[{\"name\":\"selectorValue\",\"type\":[{\"type\":\"record\",\"name\":\"EffectivePriceTableMsg\",\"fields\":[{\"name\":\"priceOverrides\",\"type\":{\"type\":\"array\",\"items\":{\"type\":\"record\",\"name\":\"EffectiveUnitPriceMsg\",\"fields\":[{\"name\":\"unitPrice\",\"type\":\"double\"},{\"name\":\"when\",\"type\":[{\"type\":\"record\",\"name\":\"CronSpecTupleMsg\",\"fields\":[{\"name\":\"a\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"b\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}}]},\"null\"]}]}}}]},{\"type\":\"map\",\"values\":\"SelectorValueMsg\",\"avro.java.string\":\"String\"}]}]},\"avro.java.string\":\"String\"},\"avro.java.string\":\"String\"}}]},\"null\"]},{\"name\":\"relatedIMEventMsg\",\"type\":[{\"type\":\"record\",\"name\":\"IMEventMsg\",\"fields\":[{\"name\":\"originalID\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"inStoreID\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"occurredMillis\",\"type\":\"long\"},{\"name\":\"receivedMillis\",\"type\":\"long\",\"default\":0},{\"name\":\"userID\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"clientID\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"eventVersion\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"},\"default\":\"1.0\"},{\"name\":\"eventType\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"isActive\",\"type\":\"boolean\"},{\"name\":\"role\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"isSynthetic\",\"type\":\"boolean\",\"default\":false},{\"name\":\"details\",\"type\":{\"type\":\"map\",\"values\":{\"type\":\"record\",\"name\":\"AnyValueMsg\",\"fields\":[{\"name\":\"anyValue\",\"type\":[\"null\",\"int\",\"long\",\"boolean\",\"double\",\"bytes\",{\"type\":\"string\",\"avro.java.string\":\"String\"},{\"type\":\"array\",\"items\":\"AnyValueMsg\"},{\"type\":\"map\",\"values\":\"AnyValueMsg\",\"avro.java.string\":\"String\"}]}]},\"avro.java.string\":\"String\"}}]},\"null\"]}]}}}]}");
  @Deprecated public java.lang.String originalID;
  @Deprecated public java.lang.String inStoreID;
  @Deprecated public java.lang.String userID;
  @Deprecated public long latestOccurredMillis;
  @Deprecated public long latestValidFromMillis;
  @Deprecated public long userCreationTimeMillis;
  @Deprecated public java.util.List<gr.grnet.aquarium.message.avro.gen.UserAgreementMsg> agreements;
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call. 
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return originalID;
    case 1: return inStoreID;
    case 2: return userID;
    case 3: return latestOccurredMillis;
    case 4: return latestValidFromMillis;
    case 5: return userCreationTimeMillis;
    case 6: return agreements;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  // Used by DatumReader.  Applications should not call. 
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: originalID = (java.lang.String)value$; break;
    case 1: inStoreID = (java.lang.String)value$; break;
    case 2: userID = (java.lang.String)value$; break;
    case 3: latestOccurredMillis = (java.lang.Long)value$; break;
    case 4: latestValidFromMillis = (java.lang.Long)value$; break;
    case 5: userCreationTimeMillis = (java.lang.Long)value$; break;
    case 6: agreements = (java.util.List<gr.grnet.aquarium.message.avro.gen.UserAgreementMsg>)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'originalID' field.
   */
  public java.lang.String getOriginalID() {
    return originalID;
  }

  /**
   * Sets the value of the 'originalID' field.
   * @param value the value to set.
   */
  public void setOriginalID(java.lang.String value) {
    this.originalID = value;
  }

  /**
   * Gets the value of the 'inStoreID' field.
   */
  public java.lang.String getInStoreID() {
    return inStoreID;
  }

  /**
   * Sets the value of the 'inStoreID' field.
   * @param value the value to set.
   */
  public void setInStoreID(java.lang.String value) {
    this.inStoreID = value;
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
   * Gets the value of the 'latestOccurredMillis' field.
   */
  public java.lang.Long getLatestOccurredMillis() {
    return latestOccurredMillis;
  }

  /**
   * Sets the value of the 'latestOccurredMillis' field.
   * @param value the value to set.
   */
  public void setLatestOccurredMillis(java.lang.Long value) {
    this.latestOccurredMillis = value;
  }

  /**
   * Gets the value of the 'latestValidFromMillis' field.
   */
  public java.lang.Long getLatestValidFromMillis() {
    return latestValidFromMillis;
  }

  /**
   * Sets the value of the 'latestValidFromMillis' field.
   * @param value the value to set.
   */
  public void setLatestValidFromMillis(java.lang.Long value) {
    this.latestValidFromMillis = value;
  }

  /**
   * Gets the value of the 'userCreationTimeMillis' field.
   */
  public java.lang.Long getUserCreationTimeMillis() {
    return userCreationTimeMillis;
  }

  /**
   * Sets the value of the 'userCreationTimeMillis' field.
   * @param value the value to set.
   */
  public void setUserCreationTimeMillis(java.lang.Long value) {
    this.userCreationTimeMillis = value;
  }

  /**
   * Gets the value of the 'agreements' field.
   */
  public java.util.List<gr.grnet.aquarium.message.avro.gen.UserAgreementMsg> getAgreements() {
    return agreements;
  }

  /**
   * Sets the value of the 'agreements' field.
   * @param value the value to set.
   */
  public void setAgreements(java.util.List<gr.grnet.aquarium.message.avro.gen.UserAgreementMsg> value) {
    this.agreements = value;
  }

  /** Creates a new UserAgreementHistoryMsg RecordBuilder */
  public static gr.grnet.aquarium.message.avro.gen.UserAgreementHistoryMsg.Builder newBuilder() {
    return new gr.grnet.aquarium.message.avro.gen.UserAgreementHistoryMsg.Builder();
  }
  
  /** Creates a new UserAgreementHistoryMsg RecordBuilder by copying an existing Builder */
  public static gr.grnet.aquarium.message.avro.gen.UserAgreementHistoryMsg.Builder newBuilder(gr.grnet.aquarium.message.avro.gen.UserAgreementHistoryMsg.Builder other) {
    return new gr.grnet.aquarium.message.avro.gen.UserAgreementHistoryMsg.Builder(other);
  }
  
  /** Creates a new UserAgreementHistoryMsg RecordBuilder by copying an existing UserAgreementHistoryMsg instance */
  public static gr.grnet.aquarium.message.avro.gen.UserAgreementHistoryMsg.Builder newBuilder(gr.grnet.aquarium.message.avro.gen.UserAgreementHistoryMsg other) {
    return new gr.grnet.aquarium.message.avro.gen.UserAgreementHistoryMsg.Builder(other);
  }
  
  /**
   * RecordBuilder for UserAgreementHistoryMsg instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<UserAgreementHistoryMsg>
    implements org.apache.avro.data.RecordBuilder<UserAgreementHistoryMsg> {

    private java.lang.String originalID;
    private java.lang.String inStoreID;
    private java.lang.String userID;
    private long latestOccurredMillis;
    private long latestValidFromMillis;
    private long userCreationTimeMillis;
    private java.util.List<gr.grnet.aquarium.message.avro.gen.UserAgreementMsg> agreements;

    /** Creates a new Builder */
    private Builder() {
      super(gr.grnet.aquarium.message.avro.gen.UserAgreementHistoryMsg.SCHEMA$);
    }
    
    /** Creates a Builder by copying an existing Builder */
    private Builder(gr.grnet.aquarium.message.avro.gen.UserAgreementHistoryMsg.Builder other) {
      super(other);
    }
    
    /** Creates a Builder by copying an existing UserAgreementHistoryMsg instance */
    private Builder(gr.grnet.aquarium.message.avro.gen.UserAgreementHistoryMsg other) {
            super(gr.grnet.aquarium.message.avro.gen.UserAgreementHistoryMsg.SCHEMA$);
      if (isValidValue(fields()[0], other.originalID)) {
        this.originalID = (java.lang.String) data().deepCopy(fields()[0].schema(), other.originalID);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.inStoreID)) {
        this.inStoreID = (java.lang.String) data().deepCopy(fields()[1].schema(), other.inStoreID);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.userID)) {
        this.userID = (java.lang.String) data().deepCopy(fields()[2].schema(), other.userID);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.latestOccurredMillis)) {
        this.latestOccurredMillis = (java.lang.Long) data().deepCopy(fields()[3].schema(), other.latestOccurredMillis);
        fieldSetFlags()[3] = true;
      }
      if (isValidValue(fields()[4], other.latestValidFromMillis)) {
        this.latestValidFromMillis = (java.lang.Long) data().deepCopy(fields()[4].schema(), other.latestValidFromMillis);
        fieldSetFlags()[4] = true;
      }
      if (isValidValue(fields()[5], other.userCreationTimeMillis)) {
        this.userCreationTimeMillis = (java.lang.Long) data().deepCopy(fields()[5].schema(), other.userCreationTimeMillis);
        fieldSetFlags()[5] = true;
      }
      if (isValidValue(fields()[6], other.agreements)) {
        this.agreements = (java.util.List<gr.grnet.aquarium.message.avro.gen.UserAgreementMsg>) data().deepCopy(fields()[6].schema(), other.agreements);
        fieldSetFlags()[6] = true;
      }
    }

    /** Gets the value of the 'originalID' field */
    public java.lang.String getOriginalID() {
      return originalID;
    }
    
    /** Sets the value of the 'originalID' field */
    public gr.grnet.aquarium.message.avro.gen.UserAgreementHistoryMsg.Builder setOriginalID(java.lang.String value) {
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
    public gr.grnet.aquarium.message.avro.gen.UserAgreementHistoryMsg.Builder clearOriginalID() {
      originalID = null;
      fieldSetFlags()[0] = false;
      return this;
    }

    /** Gets the value of the 'inStoreID' field */
    public java.lang.String getInStoreID() {
      return inStoreID;
    }
    
    /** Sets the value of the 'inStoreID' field */
    public gr.grnet.aquarium.message.avro.gen.UserAgreementHistoryMsg.Builder setInStoreID(java.lang.String value) {
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
    public gr.grnet.aquarium.message.avro.gen.UserAgreementHistoryMsg.Builder clearInStoreID() {
      inStoreID = null;
      fieldSetFlags()[1] = false;
      return this;
    }

    /** Gets the value of the 'userID' field */
    public java.lang.String getUserID() {
      return userID;
    }
    
    /** Sets the value of the 'userID' field */
    public gr.grnet.aquarium.message.avro.gen.UserAgreementHistoryMsg.Builder setUserID(java.lang.String value) {
      validate(fields()[2], value);
      this.userID = value;
      fieldSetFlags()[2] = true;
      return this; 
    }
    
    /** Checks whether the 'userID' field has been set */
    public boolean hasUserID() {
      return fieldSetFlags()[2];
    }
    
    /** Clears the value of the 'userID' field */
    public gr.grnet.aquarium.message.avro.gen.UserAgreementHistoryMsg.Builder clearUserID() {
      userID = null;
      fieldSetFlags()[2] = false;
      return this;
    }

    /** Gets the value of the 'latestOccurredMillis' field */
    public java.lang.Long getLatestOccurredMillis() {
      return latestOccurredMillis;
    }
    
    /** Sets the value of the 'latestOccurredMillis' field */
    public gr.grnet.aquarium.message.avro.gen.UserAgreementHistoryMsg.Builder setLatestOccurredMillis(long value) {
      validate(fields()[3], value);
      this.latestOccurredMillis = value;
      fieldSetFlags()[3] = true;
      return this; 
    }
    
    /** Checks whether the 'latestOccurredMillis' field has been set */
    public boolean hasLatestOccurredMillis() {
      return fieldSetFlags()[3];
    }
    
    /** Clears the value of the 'latestOccurredMillis' field */
    public gr.grnet.aquarium.message.avro.gen.UserAgreementHistoryMsg.Builder clearLatestOccurredMillis() {
      fieldSetFlags()[3] = false;
      return this;
    }

    /** Gets the value of the 'latestValidFromMillis' field */
    public java.lang.Long getLatestValidFromMillis() {
      return latestValidFromMillis;
    }
    
    /** Sets the value of the 'latestValidFromMillis' field */
    public gr.grnet.aquarium.message.avro.gen.UserAgreementHistoryMsg.Builder setLatestValidFromMillis(long value) {
      validate(fields()[4], value);
      this.latestValidFromMillis = value;
      fieldSetFlags()[4] = true;
      return this; 
    }
    
    /** Checks whether the 'latestValidFromMillis' field has been set */
    public boolean hasLatestValidFromMillis() {
      return fieldSetFlags()[4];
    }
    
    /** Clears the value of the 'latestValidFromMillis' field */
    public gr.grnet.aquarium.message.avro.gen.UserAgreementHistoryMsg.Builder clearLatestValidFromMillis() {
      fieldSetFlags()[4] = false;
      return this;
    }

    /** Gets the value of the 'userCreationTimeMillis' field */
    public java.lang.Long getUserCreationTimeMillis() {
      return userCreationTimeMillis;
    }
    
    /** Sets the value of the 'userCreationTimeMillis' field */
    public gr.grnet.aquarium.message.avro.gen.UserAgreementHistoryMsg.Builder setUserCreationTimeMillis(long value) {
      validate(fields()[5], value);
      this.userCreationTimeMillis = value;
      fieldSetFlags()[5] = true;
      return this; 
    }
    
    /** Checks whether the 'userCreationTimeMillis' field has been set */
    public boolean hasUserCreationTimeMillis() {
      return fieldSetFlags()[5];
    }
    
    /** Clears the value of the 'userCreationTimeMillis' field */
    public gr.grnet.aquarium.message.avro.gen.UserAgreementHistoryMsg.Builder clearUserCreationTimeMillis() {
      fieldSetFlags()[5] = false;
      return this;
    }

    /** Gets the value of the 'agreements' field */
    public java.util.List<gr.grnet.aquarium.message.avro.gen.UserAgreementMsg> getAgreements() {
      return agreements;
    }
    
    /** Sets the value of the 'agreements' field */
    public gr.grnet.aquarium.message.avro.gen.UserAgreementHistoryMsg.Builder setAgreements(java.util.List<gr.grnet.aquarium.message.avro.gen.UserAgreementMsg> value) {
      validate(fields()[6], value);
      this.agreements = value;
      fieldSetFlags()[6] = true;
      return this; 
    }
    
    /** Checks whether the 'agreements' field has been set */
    public boolean hasAgreements() {
      return fieldSetFlags()[6];
    }
    
    /** Clears the value of the 'agreements' field */
    public gr.grnet.aquarium.message.avro.gen.UserAgreementHistoryMsg.Builder clearAgreements() {
      agreements = null;
      fieldSetFlags()[6] = false;
      return this;
    }

    @Override
    public UserAgreementHistoryMsg build() {
      try {
        UserAgreementHistoryMsg record = new UserAgreementHistoryMsg();
        record.originalID = fieldSetFlags()[0] ? this.originalID : (java.lang.String) defaultValue(fields()[0]);
        record.inStoreID = fieldSetFlags()[1] ? this.inStoreID : (java.lang.String) defaultValue(fields()[1]);
        record.userID = fieldSetFlags()[2] ? this.userID : (java.lang.String) defaultValue(fields()[2]);
        record.latestOccurredMillis = fieldSetFlags()[3] ? this.latestOccurredMillis : (java.lang.Long) defaultValue(fields()[3]);
        record.latestValidFromMillis = fieldSetFlags()[4] ? this.latestValidFromMillis : (java.lang.Long) defaultValue(fields()[4]);
        record.userCreationTimeMillis = fieldSetFlags()[5] ? this.userCreationTimeMillis : (java.lang.Long) defaultValue(fields()[5]);
        record.agreements = fieldSetFlags()[6] ? this.agreements : (java.util.List<gr.grnet.aquarium.message.avro.gen.UserAgreementMsg>) defaultValue(fields()[6]);
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }
}
