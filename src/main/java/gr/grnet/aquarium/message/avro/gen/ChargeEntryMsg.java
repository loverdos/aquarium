/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package gr.grnet.aquarium.message.avro.gen;  
@SuppressWarnings("all")
public class ChargeEntryMsg extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"ChargeEntryMsg\",\"namespace\":\"gr.grnet.aquarium.message.avro.gen\",\"fields\":[{\"name\":\"id\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"unitPrice\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"startTime\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"endTime\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"ellapsedTime\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"credits\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}}]}");
  @Deprecated public java.lang.String id;
  @Deprecated public java.lang.String unitPrice;
  @Deprecated public java.lang.String startTime;
  @Deprecated public java.lang.String endTime;
  @Deprecated public java.lang.String ellapsedTime;
  @Deprecated public java.lang.String credits;
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call. 
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return id;
    case 1: return unitPrice;
    case 2: return startTime;
    case 3: return endTime;
    case 4: return ellapsedTime;
    case 5: return credits;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  // Used by DatumReader.  Applications should not call. 
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: id = (java.lang.String)value$; break;
    case 1: unitPrice = (java.lang.String)value$; break;
    case 2: startTime = (java.lang.String)value$; break;
    case 3: endTime = (java.lang.String)value$; break;
    case 4: ellapsedTime = (java.lang.String)value$; break;
    case 5: credits = (java.lang.String)value$; break;
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
   * Gets the value of the 'unitPrice' field.
   */
  public java.lang.String getUnitPrice() {
    return unitPrice;
  }

  /**
   * Sets the value of the 'unitPrice' field.
   * @param value the value to set.
   */
  public void setUnitPrice(java.lang.String value) {
    this.unitPrice = value;
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
   * Gets the value of the 'ellapsedTime' field.
   */
  public java.lang.String getEllapsedTime() {
    return ellapsedTime;
  }

  /**
   * Sets the value of the 'ellapsedTime' field.
   * @param value the value to set.
   */
  public void setEllapsedTime(java.lang.String value) {
    this.ellapsedTime = value;
  }

  /**
   * Gets the value of the 'credits' field.
   */
  public java.lang.String getCredits() {
    return credits;
  }

  /**
   * Sets the value of the 'credits' field.
   * @param value the value to set.
   */
  public void setCredits(java.lang.String value) {
    this.credits = value;
  }

  /** Creates a new ChargeEntryMsg RecordBuilder */
  public static gr.grnet.aquarium.message.avro.gen.ChargeEntryMsg.Builder newBuilder() {
    return new gr.grnet.aquarium.message.avro.gen.ChargeEntryMsg.Builder();
  }
  
  /** Creates a new ChargeEntryMsg RecordBuilder by copying an existing Builder */
  public static gr.grnet.aquarium.message.avro.gen.ChargeEntryMsg.Builder newBuilder(gr.grnet.aquarium.message.avro.gen.ChargeEntryMsg.Builder other) {
    return new gr.grnet.aquarium.message.avro.gen.ChargeEntryMsg.Builder(other);
  }
  
  /** Creates a new ChargeEntryMsg RecordBuilder by copying an existing ChargeEntryMsg instance */
  public static gr.grnet.aquarium.message.avro.gen.ChargeEntryMsg.Builder newBuilder(gr.grnet.aquarium.message.avro.gen.ChargeEntryMsg other) {
    return new gr.grnet.aquarium.message.avro.gen.ChargeEntryMsg.Builder(other);
  }
  
  /**
   * RecordBuilder for ChargeEntryMsg instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<ChargeEntryMsg>
    implements org.apache.avro.data.RecordBuilder<ChargeEntryMsg> {

    private java.lang.String id;
    private java.lang.String unitPrice;
    private java.lang.String startTime;
    private java.lang.String endTime;
    private java.lang.String ellapsedTime;
    private java.lang.String credits;

    /** Creates a new Builder */
    private Builder() {
      super(gr.grnet.aquarium.message.avro.gen.ChargeEntryMsg.SCHEMA$);
    }
    
    /** Creates a Builder by copying an existing Builder */
    private Builder(gr.grnet.aquarium.message.avro.gen.ChargeEntryMsg.Builder other) {
      super(other);
    }
    
    /** Creates a Builder by copying an existing ChargeEntryMsg instance */
    private Builder(gr.grnet.aquarium.message.avro.gen.ChargeEntryMsg other) {
            super(gr.grnet.aquarium.message.avro.gen.ChargeEntryMsg.SCHEMA$);
      if (isValidValue(fields()[0], other.id)) {
        this.id = (java.lang.String) data().deepCopy(fields()[0].schema(), other.id);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.unitPrice)) {
        this.unitPrice = (java.lang.String) data().deepCopy(fields()[1].schema(), other.unitPrice);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.startTime)) {
        this.startTime = (java.lang.String) data().deepCopy(fields()[2].schema(), other.startTime);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.endTime)) {
        this.endTime = (java.lang.String) data().deepCopy(fields()[3].schema(), other.endTime);
        fieldSetFlags()[3] = true;
      }
      if (isValidValue(fields()[4], other.ellapsedTime)) {
        this.ellapsedTime = (java.lang.String) data().deepCopy(fields()[4].schema(), other.ellapsedTime);
        fieldSetFlags()[4] = true;
      }
      if (isValidValue(fields()[5], other.credits)) {
        this.credits = (java.lang.String) data().deepCopy(fields()[5].schema(), other.credits);
        fieldSetFlags()[5] = true;
      }
    }

    /** Gets the value of the 'id' field */
    public java.lang.String getId() {
      return id;
    }
    
    /** Sets the value of the 'id' field */
    public gr.grnet.aquarium.message.avro.gen.ChargeEntryMsg.Builder setId(java.lang.String value) {
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
    public gr.grnet.aquarium.message.avro.gen.ChargeEntryMsg.Builder clearId() {
      id = null;
      fieldSetFlags()[0] = false;
      return this;
    }

    /** Gets the value of the 'unitPrice' field */
    public java.lang.String getUnitPrice() {
      return unitPrice;
    }
    
    /** Sets the value of the 'unitPrice' field */
    public gr.grnet.aquarium.message.avro.gen.ChargeEntryMsg.Builder setUnitPrice(java.lang.String value) {
      validate(fields()[1], value);
      this.unitPrice = value;
      fieldSetFlags()[1] = true;
      return this; 
    }
    
    /** Checks whether the 'unitPrice' field has been set */
    public boolean hasUnitPrice() {
      return fieldSetFlags()[1];
    }
    
    /** Clears the value of the 'unitPrice' field */
    public gr.grnet.aquarium.message.avro.gen.ChargeEntryMsg.Builder clearUnitPrice() {
      unitPrice = null;
      fieldSetFlags()[1] = false;
      return this;
    }

    /** Gets the value of the 'startTime' field */
    public java.lang.String getStartTime() {
      return startTime;
    }
    
    /** Sets the value of the 'startTime' field */
    public gr.grnet.aquarium.message.avro.gen.ChargeEntryMsg.Builder setStartTime(java.lang.String value) {
      validate(fields()[2], value);
      this.startTime = value;
      fieldSetFlags()[2] = true;
      return this; 
    }
    
    /** Checks whether the 'startTime' field has been set */
    public boolean hasStartTime() {
      return fieldSetFlags()[2];
    }
    
    /** Clears the value of the 'startTime' field */
    public gr.grnet.aquarium.message.avro.gen.ChargeEntryMsg.Builder clearStartTime() {
      startTime = null;
      fieldSetFlags()[2] = false;
      return this;
    }

    /** Gets the value of the 'endTime' field */
    public java.lang.String getEndTime() {
      return endTime;
    }
    
    /** Sets the value of the 'endTime' field */
    public gr.grnet.aquarium.message.avro.gen.ChargeEntryMsg.Builder setEndTime(java.lang.String value) {
      validate(fields()[3], value);
      this.endTime = value;
      fieldSetFlags()[3] = true;
      return this; 
    }
    
    /** Checks whether the 'endTime' field has been set */
    public boolean hasEndTime() {
      return fieldSetFlags()[3];
    }
    
    /** Clears the value of the 'endTime' field */
    public gr.grnet.aquarium.message.avro.gen.ChargeEntryMsg.Builder clearEndTime() {
      endTime = null;
      fieldSetFlags()[3] = false;
      return this;
    }

    /** Gets the value of the 'ellapsedTime' field */
    public java.lang.String getEllapsedTime() {
      return ellapsedTime;
    }
    
    /** Sets the value of the 'ellapsedTime' field */
    public gr.grnet.aquarium.message.avro.gen.ChargeEntryMsg.Builder setEllapsedTime(java.lang.String value) {
      validate(fields()[4], value);
      this.ellapsedTime = value;
      fieldSetFlags()[4] = true;
      return this; 
    }
    
    /** Checks whether the 'ellapsedTime' field has been set */
    public boolean hasEllapsedTime() {
      return fieldSetFlags()[4];
    }
    
    /** Clears the value of the 'ellapsedTime' field */
    public gr.grnet.aquarium.message.avro.gen.ChargeEntryMsg.Builder clearEllapsedTime() {
      ellapsedTime = null;
      fieldSetFlags()[4] = false;
      return this;
    }

    /** Gets the value of the 'credits' field */
    public java.lang.String getCredits() {
      return credits;
    }
    
    /** Sets the value of the 'credits' field */
    public gr.grnet.aquarium.message.avro.gen.ChargeEntryMsg.Builder setCredits(java.lang.String value) {
      validate(fields()[5], value);
      this.credits = value;
      fieldSetFlags()[5] = true;
      return this; 
    }
    
    /** Checks whether the 'credits' field has been set */
    public boolean hasCredits() {
      return fieldSetFlags()[5];
    }
    
    /** Clears the value of the 'credits' field */
    public gr.grnet.aquarium.message.avro.gen.ChargeEntryMsg.Builder clearCredits() {
      credits = null;
      fieldSetFlags()[5] = false;
      return this;
    }

    @Override
    public ChargeEntryMsg build() {
      try {
        ChargeEntryMsg record = new ChargeEntryMsg();
        record.id = fieldSetFlags()[0] ? this.id : (java.lang.String) defaultValue(fields()[0]);
        record.unitPrice = fieldSetFlags()[1] ? this.unitPrice : (java.lang.String) defaultValue(fields()[1]);
        record.startTime = fieldSetFlags()[2] ? this.startTime : (java.lang.String) defaultValue(fields()[2]);
        record.endTime = fieldSetFlags()[3] ? this.endTime : (java.lang.String) defaultValue(fields()[3]);
        record.ellapsedTime = fieldSetFlags()[4] ? this.ellapsedTime : (java.lang.String) defaultValue(fields()[4]);
        record.credits = fieldSetFlags()[5] ? this.credits : (java.lang.String) defaultValue(fields()[5]);
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }
}
