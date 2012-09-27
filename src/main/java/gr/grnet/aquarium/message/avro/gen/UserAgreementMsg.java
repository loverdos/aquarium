/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package gr.grnet.aquarium.message.avro.gen;  
@SuppressWarnings("all")
public class UserAgreementMsg extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"UserAgreementMsg\",\"namespace\":\"gr.grnet.aquarium.message.avro.gen\",\"fields\":[{\"name\":\"id\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"relatedIMEventOriginalID\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"userID\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"occurredMillis\",\"type\":\"long\",\"default\":0},{\"name\":\"validFromMillis\",\"type\":\"long\"},{\"name\":\"validToMillis\",\"type\":\"long\"},{\"name\":\"role\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"fullPriceTableRef\",\"type\":[{\"type\":\"record\",\"name\":\"FullPriceTableMsg\",\"fields\":[{\"name\":\"perResource\",\"type\":{\"type\":\"map\",\"values\":{\"type\":\"map\",\"values\":{\"type\":\"record\",\"name\":\"SelectorValueMsg\",\"fields\":[{\"name\":\"selectorValue\",\"type\":[{\"type\":\"record\",\"name\":\"EffectivePriceTableMsg\",\"fields\":[{\"name\":\"priceOverrides\",\"type\":{\"type\":\"array\",\"items\":{\"type\":\"record\",\"name\":\"EffectiveUnitPriceMsg\",\"fields\":[{\"name\":\"unitPrice\",\"type\":\"double\"},{\"name\":\"when\",\"type\":[{\"type\":\"record\",\"name\":\"CronSpecTupleMsg\",\"fields\":[{\"name\":\"a\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"b\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}}]},\"null\"]}]}}}]},{\"type\":\"map\",\"values\":\"SelectorValueMsg\",\"avro.java.string\":\"String\"}]}]},\"avro.java.string\":\"String\"},\"avro.java.string\":\"String\"}}]},\"null\"]},{\"name\":\"relatedIMEventMsg\",\"type\":[{\"type\":\"record\",\"name\":\"IMEventMsg\",\"fields\":[{\"name\":\"originalID\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"inStoreID\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"occurredMillis\",\"type\":\"long\"},{\"name\":\"receivedMillis\",\"type\":\"long\",\"default\":0},{\"name\":\"userID\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"clientID\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"eventVersion\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"},\"default\":\"1.0\"},{\"name\":\"eventType\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"isActive\",\"type\":\"boolean\"},{\"name\":\"role\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"isSynthetic\",\"type\":\"boolean\",\"default\":false},{\"name\":\"details\",\"type\":{\"type\":\"map\",\"values\":{\"type\":\"record\",\"name\":\"AnyValueMsg\",\"fields\":[{\"name\":\"anyValue\",\"type\":[\"null\",\"int\",\"long\",\"boolean\",\"double\",\"bytes\",{\"type\":\"string\",\"avro.java.string\":\"String\"},{\"type\":\"array\",\"items\":\"AnyValueMsg\"},{\"type\":\"map\",\"values\":\"AnyValueMsg\",\"avro.java.string\":\"String\"}]}]},\"avro.java.string\":\"String\"}}]},\"null\"]}]}");
  @Deprecated public java.lang.String id;
  @Deprecated public java.lang.String relatedIMEventOriginalID;
  @Deprecated public java.lang.String userID;
  @Deprecated public long occurredMillis;
  @Deprecated public long validFromMillis;
  @Deprecated public long validToMillis;
  @Deprecated public java.lang.String role;
  @Deprecated public gr.grnet.aquarium.message.avro.gen.FullPriceTableMsg fullPriceTableRef;
  @Deprecated public gr.grnet.aquarium.message.avro.gen.IMEventMsg relatedIMEventMsg;

  /**
   * Default constructor.
   */
  public UserAgreementMsg() {}

  /**
   * All-args constructor.
   */
  public UserAgreementMsg(java.lang.String id, java.lang.String relatedIMEventOriginalID, java.lang.String userID, java.lang.Long occurredMillis, java.lang.Long validFromMillis, java.lang.Long validToMillis, java.lang.String role, gr.grnet.aquarium.message.avro.gen.FullPriceTableMsg fullPriceTableRef, gr.grnet.aquarium.message.avro.gen.IMEventMsg relatedIMEventMsg) {
    this.id = id;
    this.relatedIMEventOriginalID = relatedIMEventOriginalID;
    this.userID = userID;
    this.occurredMillis = occurredMillis;
    this.validFromMillis = validFromMillis;
    this.validToMillis = validToMillis;
    this.role = role;
    this.fullPriceTableRef = fullPriceTableRef;
    this.relatedIMEventMsg = relatedIMEventMsg;
  }

  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call. 
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return id;
    case 1: return relatedIMEventOriginalID;
    case 2: return userID;
    case 3: return occurredMillis;
    case 4: return validFromMillis;
    case 5: return validToMillis;
    case 6: return role;
    case 7: return fullPriceTableRef;
    case 8: return relatedIMEventMsg;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  // Used by DatumReader.  Applications should not call. 
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: id = (java.lang.String)value$; break;
    case 1: relatedIMEventOriginalID = (java.lang.String)value$; break;
    case 2: userID = (java.lang.String)value$; break;
    case 3: occurredMillis = (java.lang.Long)value$; break;
    case 4: validFromMillis = (java.lang.Long)value$; break;
    case 5: validToMillis = (java.lang.Long)value$; break;
    case 6: role = (java.lang.String)value$; break;
    case 7: fullPriceTableRef = (gr.grnet.aquarium.message.avro.gen.FullPriceTableMsg)value$; break;
    case 8: relatedIMEventMsg = (gr.grnet.aquarium.message.avro.gen.IMEventMsg)value$; break;
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
   * Gets the value of the 'relatedIMEventOriginalID' field.
   */
  public java.lang.String getRelatedIMEventOriginalID() {
    return relatedIMEventOriginalID;
  }

  /**
   * Sets the value of the 'relatedIMEventOriginalID' field.
   * @param value the value to set.
   */
  public void setRelatedIMEventOriginalID(java.lang.String value) {
    this.relatedIMEventOriginalID = value;
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
   * Gets the value of the 'validFromMillis' field.
   */
  public java.lang.Long getValidFromMillis() {
    return validFromMillis;
  }

  /**
   * Sets the value of the 'validFromMillis' field.
   * @param value the value to set.
   */
  public void setValidFromMillis(java.lang.Long value) {
    this.validFromMillis = value;
  }

  /**
   * Gets the value of the 'validToMillis' field.
   */
  public java.lang.Long getValidToMillis() {
    return validToMillis;
  }

  /**
   * Sets the value of the 'validToMillis' field.
   * @param value the value to set.
   */
  public void setValidToMillis(java.lang.Long value) {
    this.validToMillis = value;
  }

  /**
   * Gets the value of the 'role' field.
   */
  public java.lang.String getRole() {
    return role;
  }

  /**
   * Sets the value of the 'role' field.
   * @param value the value to set.
   */
  public void setRole(java.lang.String value) {
    this.role = value;
  }

  /**
   * Gets the value of the 'fullPriceTableRef' field.
   */
  public gr.grnet.aquarium.message.avro.gen.FullPriceTableMsg getFullPriceTableRef() {
    return fullPriceTableRef;
  }

  /**
   * Sets the value of the 'fullPriceTableRef' field.
   * @param value the value to set.
   */
  public void setFullPriceTableRef(gr.grnet.aquarium.message.avro.gen.FullPriceTableMsg value) {
    this.fullPriceTableRef = value;
  }

  /**
   * Gets the value of the 'relatedIMEventMsg' field.
   */
  public gr.grnet.aquarium.message.avro.gen.IMEventMsg getRelatedIMEventMsg() {
    return relatedIMEventMsg;
  }

  /**
   * Sets the value of the 'relatedIMEventMsg' field.
   * @param value the value to set.
   */
  public void setRelatedIMEventMsg(gr.grnet.aquarium.message.avro.gen.IMEventMsg value) {
    this.relatedIMEventMsg = value;
  }

  /** Creates a new UserAgreementMsg RecordBuilder */
  public static gr.grnet.aquarium.message.avro.gen.UserAgreementMsg.Builder newBuilder() {
    return new gr.grnet.aquarium.message.avro.gen.UserAgreementMsg.Builder();
  }
  
  /** Creates a new UserAgreementMsg RecordBuilder by copying an existing Builder */
  public static gr.grnet.aquarium.message.avro.gen.UserAgreementMsg.Builder newBuilder(gr.grnet.aquarium.message.avro.gen.UserAgreementMsg.Builder other) {
    return new gr.grnet.aquarium.message.avro.gen.UserAgreementMsg.Builder(other);
  }
  
  /** Creates a new UserAgreementMsg RecordBuilder by copying an existing UserAgreementMsg instance */
  public static gr.grnet.aquarium.message.avro.gen.UserAgreementMsg.Builder newBuilder(gr.grnet.aquarium.message.avro.gen.UserAgreementMsg other) {
    return new gr.grnet.aquarium.message.avro.gen.UserAgreementMsg.Builder(other);
  }
  
  /**
   * RecordBuilder for UserAgreementMsg instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<UserAgreementMsg>
    implements org.apache.avro.data.RecordBuilder<UserAgreementMsg> {

    private java.lang.String id;
    private java.lang.String relatedIMEventOriginalID;
    private java.lang.String userID;
    private long occurredMillis;
    private long validFromMillis;
    private long validToMillis;
    private java.lang.String role;
    private gr.grnet.aquarium.message.avro.gen.FullPriceTableMsg fullPriceTableRef;
    private gr.grnet.aquarium.message.avro.gen.IMEventMsg relatedIMEventMsg;

    /** Creates a new Builder */
    private Builder() {
      super(gr.grnet.aquarium.message.avro.gen.UserAgreementMsg.SCHEMA$);
    }
    
    /** Creates a Builder by copying an existing Builder */
    private Builder(gr.grnet.aquarium.message.avro.gen.UserAgreementMsg.Builder other) {
      super(other);
    }
    
    /** Creates a Builder by copying an existing UserAgreementMsg instance */
    private Builder(gr.grnet.aquarium.message.avro.gen.UserAgreementMsg other) {
            super(gr.grnet.aquarium.message.avro.gen.UserAgreementMsg.SCHEMA$);
      if (isValidValue(fields()[0], other.id)) {
        this.id = (java.lang.String) data().deepCopy(fields()[0].schema(), other.id);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.relatedIMEventOriginalID)) {
        this.relatedIMEventOriginalID = (java.lang.String) data().deepCopy(fields()[1].schema(), other.relatedIMEventOriginalID);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.userID)) {
        this.userID = (java.lang.String) data().deepCopy(fields()[2].schema(), other.userID);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.occurredMillis)) {
        this.occurredMillis = (java.lang.Long) data().deepCopy(fields()[3].schema(), other.occurredMillis);
        fieldSetFlags()[3] = true;
      }
      if (isValidValue(fields()[4], other.validFromMillis)) {
        this.validFromMillis = (java.lang.Long) data().deepCopy(fields()[4].schema(), other.validFromMillis);
        fieldSetFlags()[4] = true;
      }
      if (isValidValue(fields()[5], other.validToMillis)) {
        this.validToMillis = (java.lang.Long) data().deepCopy(fields()[5].schema(), other.validToMillis);
        fieldSetFlags()[5] = true;
      }
      if (isValidValue(fields()[6], other.role)) {
        this.role = (java.lang.String) data().deepCopy(fields()[6].schema(), other.role);
        fieldSetFlags()[6] = true;
      }
      if (isValidValue(fields()[7], other.fullPriceTableRef)) {
        this.fullPriceTableRef = (gr.grnet.aquarium.message.avro.gen.FullPriceTableMsg) data().deepCopy(fields()[7].schema(), other.fullPriceTableRef);
        fieldSetFlags()[7] = true;
      }
      if (isValidValue(fields()[8], other.relatedIMEventMsg)) {
        this.relatedIMEventMsg = (gr.grnet.aquarium.message.avro.gen.IMEventMsg) data().deepCopy(fields()[8].schema(), other.relatedIMEventMsg);
        fieldSetFlags()[8] = true;
      }
    }

    /** Gets the value of the 'id' field */
    public java.lang.String getId() {
      return id;
    }
    
    /** Sets the value of the 'id' field */
    public gr.grnet.aquarium.message.avro.gen.UserAgreementMsg.Builder setId(java.lang.String value) {
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
    public gr.grnet.aquarium.message.avro.gen.UserAgreementMsg.Builder clearId() {
      id = null;
      fieldSetFlags()[0] = false;
      return this;
    }

    /** Gets the value of the 'relatedIMEventOriginalID' field */
    public java.lang.String getRelatedIMEventOriginalID() {
      return relatedIMEventOriginalID;
    }
    
    /** Sets the value of the 'relatedIMEventOriginalID' field */
    public gr.grnet.aquarium.message.avro.gen.UserAgreementMsg.Builder setRelatedIMEventOriginalID(java.lang.String value) {
      validate(fields()[1], value);
      this.relatedIMEventOriginalID = value;
      fieldSetFlags()[1] = true;
      return this; 
    }
    
    /** Checks whether the 'relatedIMEventOriginalID' field has been set */
    public boolean hasRelatedIMEventOriginalID() {
      return fieldSetFlags()[1];
    }
    
    /** Clears the value of the 'relatedIMEventOriginalID' field */
    public gr.grnet.aquarium.message.avro.gen.UserAgreementMsg.Builder clearRelatedIMEventOriginalID() {
      relatedIMEventOriginalID = null;
      fieldSetFlags()[1] = false;
      return this;
    }

    /** Gets the value of the 'userID' field */
    public java.lang.String getUserID() {
      return userID;
    }
    
    /** Sets the value of the 'userID' field */
    public gr.grnet.aquarium.message.avro.gen.UserAgreementMsg.Builder setUserID(java.lang.String value) {
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
    public gr.grnet.aquarium.message.avro.gen.UserAgreementMsg.Builder clearUserID() {
      userID = null;
      fieldSetFlags()[2] = false;
      return this;
    }

    /** Gets the value of the 'occurredMillis' field */
    public java.lang.Long getOccurredMillis() {
      return occurredMillis;
    }
    
    /** Sets the value of the 'occurredMillis' field */
    public gr.grnet.aquarium.message.avro.gen.UserAgreementMsg.Builder setOccurredMillis(long value) {
      validate(fields()[3], value);
      this.occurredMillis = value;
      fieldSetFlags()[3] = true;
      return this; 
    }
    
    /** Checks whether the 'occurredMillis' field has been set */
    public boolean hasOccurredMillis() {
      return fieldSetFlags()[3];
    }
    
    /** Clears the value of the 'occurredMillis' field */
    public gr.grnet.aquarium.message.avro.gen.UserAgreementMsg.Builder clearOccurredMillis() {
      fieldSetFlags()[3] = false;
      return this;
    }

    /** Gets the value of the 'validFromMillis' field */
    public java.lang.Long getValidFromMillis() {
      return validFromMillis;
    }
    
    /** Sets the value of the 'validFromMillis' field */
    public gr.grnet.aquarium.message.avro.gen.UserAgreementMsg.Builder setValidFromMillis(long value) {
      validate(fields()[4], value);
      this.validFromMillis = value;
      fieldSetFlags()[4] = true;
      return this; 
    }
    
    /** Checks whether the 'validFromMillis' field has been set */
    public boolean hasValidFromMillis() {
      return fieldSetFlags()[4];
    }
    
    /** Clears the value of the 'validFromMillis' field */
    public gr.grnet.aquarium.message.avro.gen.UserAgreementMsg.Builder clearValidFromMillis() {
      fieldSetFlags()[4] = false;
      return this;
    }

    /** Gets the value of the 'validToMillis' field */
    public java.lang.Long getValidToMillis() {
      return validToMillis;
    }
    
    /** Sets the value of the 'validToMillis' field */
    public gr.grnet.aquarium.message.avro.gen.UserAgreementMsg.Builder setValidToMillis(long value) {
      validate(fields()[5], value);
      this.validToMillis = value;
      fieldSetFlags()[5] = true;
      return this; 
    }
    
    /** Checks whether the 'validToMillis' field has been set */
    public boolean hasValidToMillis() {
      return fieldSetFlags()[5];
    }
    
    /** Clears the value of the 'validToMillis' field */
    public gr.grnet.aquarium.message.avro.gen.UserAgreementMsg.Builder clearValidToMillis() {
      fieldSetFlags()[5] = false;
      return this;
    }

    /** Gets the value of the 'role' field */
    public java.lang.String getRole() {
      return role;
    }
    
    /** Sets the value of the 'role' field */
    public gr.grnet.aquarium.message.avro.gen.UserAgreementMsg.Builder setRole(java.lang.String value) {
      validate(fields()[6], value);
      this.role = value;
      fieldSetFlags()[6] = true;
      return this; 
    }
    
    /** Checks whether the 'role' field has been set */
    public boolean hasRole() {
      return fieldSetFlags()[6];
    }
    
    /** Clears the value of the 'role' field */
    public gr.grnet.aquarium.message.avro.gen.UserAgreementMsg.Builder clearRole() {
      role = null;
      fieldSetFlags()[6] = false;
      return this;
    }

    /** Gets the value of the 'fullPriceTableRef' field */
    public gr.grnet.aquarium.message.avro.gen.FullPriceTableMsg getFullPriceTableRef() {
      return fullPriceTableRef;
    }
    
    /** Sets the value of the 'fullPriceTableRef' field */
    public gr.grnet.aquarium.message.avro.gen.UserAgreementMsg.Builder setFullPriceTableRef(gr.grnet.aquarium.message.avro.gen.FullPriceTableMsg value) {
      validate(fields()[7], value);
      this.fullPriceTableRef = value;
      fieldSetFlags()[7] = true;
      return this; 
    }
    
    /** Checks whether the 'fullPriceTableRef' field has been set */
    public boolean hasFullPriceTableRef() {
      return fieldSetFlags()[7];
    }
    
    /** Clears the value of the 'fullPriceTableRef' field */
    public gr.grnet.aquarium.message.avro.gen.UserAgreementMsg.Builder clearFullPriceTableRef() {
      fullPriceTableRef = null;
      fieldSetFlags()[7] = false;
      return this;
    }

    /** Gets the value of the 'relatedIMEventMsg' field */
    public gr.grnet.aquarium.message.avro.gen.IMEventMsg getRelatedIMEventMsg() {
      return relatedIMEventMsg;
    }
    
    /** Sets the value of the 'relatedIMEventMsg' field */
    public gr.grnet.aquarium.message.avro.gen.UserAgreementMsg.Builder setRelatedIMEventMsg(gr.grnet.aquarium.message.avro.gen.IMEventMsg value) {
      validate(fields()[8], value);
      this.relatedIMEventMsg = value;
      fieldSetFlags()[8] = true;
      return this; 
    }
    
    /** Checks whether the 'relatedIMEventMsg' field has been set */
    public boolean hasRelatedIMEventMsg() {
      return fieldSetFlags()[8];
    }
    
    /** Clears the value of the 'relatedIMEventMsg' field */
    public gr.grnet.aquarium.message.avro.gen.UserAgreementMsg.Builder clearRelatedIMEventMsg() {
      relatedIMEventMsg = null;
      fieldSetFlags()[8] = false;
      return this;
    }

    @Override
    public UserAgreementMsg build() {
      try {
        UserAgreementMsg record = new UserAgreementMsg();
        record.id = fieldSetFlags()[0] ? this.id : (java.lang.String) defaultValue(fields()[0]);
        record.relatedIMEventOriginalID = fieldSetFlags()[1] ? this.relatedIMEventOriginalID : (java.lang.String) defaultValue(fields()[1]);
        record.userID = fieldSetFlags()[2] ? this.userID : (java.lang.String) defaultValue(fields()[2]);
        record.occurredMillis = fieldSetFlags()[3] ? this.occurredMillis : (java.lang.Long) defaultValue(fields()[3]);
        record.validFromMillis = fieldSetFlags()[4] ? this.validFromMillis : (java.lang.Long) defaultValue(fields()[4]);
        record.validToMillis = fieldSetFlags()[5] ? this.validToMillis : (java.lang.Long) defaultValue(fields()[5]);
        record.role = fieldSetFlags()[6] ? this.role : (java.lang.String) defaultValue(fields()[6]);
        record.fullPriceTableRef = fieldSetFlags()[7] ? this.fullPriceTableRef : (gr.grnet.aquarium.message.avro.gen.FullPriceTableMsg) defaultValue(fields()[7]);
        record.relatedIMEventMsg = fieldSetFlags()[8] ? this.relatedIMEventMsg : (gr.grnet.aquarium.message.avro.gen.IMEventMsg) defaultValue(fields()[8]);
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }
}
