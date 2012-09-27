/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package gr.grnet.aquarium.message.avro.gen;  
@SuppressWarnings("all")
public class ChargeslotMsg extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"ChargeslotMsg\",\"namespace\":\"gr.grnet.aquarium.message.avro.gen\",\"fields\":[{\"name\":\"startMillis\",\"type\":\"long\"},{\"name\":\"stopMillis\",\"type\":\"long\"},{\"name\":\"unitPrice\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"explanation\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"},\"default\":\"\"},{\"name\":\"creditsToSubtract\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"},\"default\":0.0}]}");
  @Deprecated public long startMillis;
  @Deprecated public long stopMillis;
  @Deprecated public java.lang.String unitPrice;
  @Deprecated public java.lang.String explanation;
  @Deprecated public java.lang.String creditsToSubtract;

  /**
   * Default constructor.
   */
  public ChargeslotMsg() {}

  /**
   * All-args constructor.
   */
  public ChargeslotMsg(java.lang.Long startMillis, java.lang.Long stopMillis, java.lang.String unitPrice, java.lang.String explanation, java.lang.String creditsToSubtract) {
    this.startMillis = startMillis;
    this.stopMillis = stopMillis;
    this.unitPrice = unitPrice;
    this.explanation = explanation;
    this.creditsToSubtract = creditsToSubtract;
  }

  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call. 
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return startMillis;
    case 1: return stopMillis;
    case 2: return unitPrice;
    case 3: return explanation;
    case 4: return creditsToSubtract;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  // Used by DatumReader.  Applications should not call. 
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: startMillis = (java.lang.Long)value$; break;
    case 1: stopMillis = (java.lang.Long)value$; break;
    case 2: unitPrice = (java.lang.String)value$; break;
    case 3: explanation = (java.lang.String)value$; break;
    case 4: creditsToSubtract = (java.lang.String)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'startMillis' field.
   */
  public java.lang.Long getStartMillis() {
    return startMillis;
  }

  /**
   * Sets the value of the 'startMillis' field.
   * @param value the value to set.
   */
  public void setStartMillis(java.lang.Long value) {
    this.startMillis = value;
  }

  /**
   * Gets the value of the 'stopMillis' field.
   */
  public java.lang.Long getStopMillis() {
    return stopMillis;
  }

  /**
   * Sets the value of the 'stopMillis' field.
   * @param value the value to set.
   */
  public void setStopMillis(java.lang.Long value) {
    this.stopMillis = value;
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
   * Gets the value of the 'explanation' field.
   */
  public java.lang.String getExplanation() {
    return explanation;
  }

  /**
   * Sets the value of the 'explanation' field.
   * @param value the value to set.
   */
  public void setExplanation(java.lang.String value) {
    this.explanation = value;
  }

  /**
   * Gets the value of the 'creditsToSubtract' field.
   */
  public java.lang.String getCreditsToSubtract() {
    return creditsToSubtract;
  }

  /**
   * Sets the value of the 'creditsToSubtract' field.
   * @param value the value to set.
   */
  public void setCreditsToSubtract(java.lang.String value) {
    this.creditsToSubtract = value;
  }

  /** Creates a new ChargeslotMsg RecordBuilder */
  public static gr.grnet.aquarium.message.avro.gen.ChargeslotMsg.Builder newBuilder() {
    return new gr.grnet.aquarium.message.avro.gen.ChargeslotMsg.Builder();
  }
  
  /** Creates a new ChargeslotMsg RecordBuilder by copying an existing Builder */
  public static gr.grnet.aquarium.message.avro.gen.ChargeslotMsg.Builder newBuilder(gr.grnet.aquarium.message.avro.gen.ChargeslotMsg.Builder other) {
    return new gr.grnet.aquarium.message.avro.gen.ChargeslotMsg.Builder(other);
  }
  
  /** Creates a new ChargeslotMsg RecordBuilder by copying an existing ChargeslotMsg instance */
  public static gr.grnet.aquarium.message.avro.gen.ChargeslotMsg.Builder newBuilder(gr.grnet.aquarium.message.avro.gen.ChargeslotMsg other) {
    return new gr.grnet.aquarium.message.avro.gen.ChargeslotMsg.Builder(other);
  }
  
  /**
   * RecordBuilder for ChargeslotMsg instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<ChargeslotMsg>
    implements org.apache.avro.data.RecordBuilder<ChargeslotMsg> {

    private long startMillis;
    private long stopMillis;
    private java.lang.String unitPrice;
    private java.lang.String explanation;
    private java.lang.String creditsToSubtract;

    /** Creates a new Builder */
    private Builder() {
      super(gr.grnet.aquarium.message.avro.gen.ChargeslotMsg.SCHEMA$);
    }
    
    /** Creates a Builder by copying an existing Builder */
    private Builder(gr.grnet.aquarium.message.avro.gen.ChargeslotMsg.Builder other) {
      super(other);
    }
    
    /** Creates a Builder by copying an existing ChargeslotMsg instance */
    private Builder(gr.grnet.aquarium.message.avro.gen.ChargeslotMsg other) {
            super(gr.grnet.aquarium.message.avro.gen.ChargeslotMsg.SCHEMA$);
      if (isValidValue(fields()[0], other.startMillis)) {
        this.startMillis = (java.lang.Long) data().deepCopy(fields()[0].schema(), other.startMillis);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.stopMillis)) {
        this.stopMillis = (java.lang.Long) data().deepCopy(fields()[1].schema(), other.stopMillis);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.unitPrice)) {
        this.unitPrice = (java.lang.String) data().deepCopy(fields()[2].schema(), other.unitPrice);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.explanation)) {
        this.explanation = (java.lang.String) data().deepCopy(fields()[3].schema(), other.explanation);
        fieldSetFlags()[3] = true;
      }
      if (isValidValue(fields()[4], other.creditsToSubtract)) {
        this.creditsToSubtract = (java.lang.String) data().deepCopy(fields()[4].schema(), other.creditsToSubtract);
        fieldSetFlags()[4] = true;
      }
    }

    /** Gets the value of the 'startMillis' field */
    public java.lang.Long getStartMillis() {
      return startMillis;
    }
    
    /** Sets the value of the 'startMillis' field */
    public gr.grnet.aquarium.message.avro.gen.ChargeslotMsg.Builder setStartMillis(long value) {
      validate(fields()[0], value);
      this.startMillis = value;
      fieldSetFlags()[0] = true;
      return this; 
    }
    
    /** Checks whether the 'startMillis' field has been set */
    public boolean hasStartMillis() {
      return fieldSetFlags()[0];
    }
    
    /** Clears the value of the 'startMillis' field */
    public gr.grnet.aquarium.message.avro.gen.ChargeslotMsg.Builder clearStartMillis() {
      fieldSetFlags()[0] = false;
      return this;
    }

    /** Gets the value of the 'stopMillis' field */
    public java.lang.Long getStopMillis() {
      return stopMillis;
    }
    
    /** Sets the value of the 'stopMillis' field */
    public gr.grnet.aquarium.message.avro.gen.ChargeslotMsg.Builder setStopMillis(long value) {
      validate(fields()[1], value);
      this.stopMillis = value;
      fieldSetFlags()[1] = true;
      return this; 
    }
    
    /** Checks whether the 'stopMillis' field has been set */
    public boolean hasStopMillis() {
      return fieldSetFlags()[1];
    }
    
    /** Clears the value of the 'stopMillis' field */
    public gr.grnet.aquarium.message.avro.gen.ChargeslotMsg.Builder clearStopMillis() {
      fieldSetFlags()[1] = false;
      return this;
    }

    /** Gets the value of the 'unitPrice' field */
    public java.lang.String getUnitPrice() {
      return unitPrice;
    }
    
    /** Sets the value of the 'unitPrice' field */
    public gr.grnet.aquarium.message.avro.gen.ChargeslotMsg.Builder setUnitPrice(java.lang.String value) {
      validate(fields()[2], value);
      this.unitPrice = value;
      fieldSetFlags()[2] = true;
      return this; 
    }
    
    /** Checks whether the 'unitPrice' field has been set */
    public boolean hasUnitPrice() {
      return fieldSetFlags()[2];
    }
    
    /** Clears the value of the 'unitPrice' field */
    public gr.grnet.aquarium.message.avro.gen.ChargeslotMsg.Builder clearUnitPrice() {
      unitPrice = null;
      fieldSetFlags()[2] = false;
      return this;
    }

    /** Gets the value of the 'explanation' field */
    public java.lang.String getExplanation() {
      return explanation;
    }
    
    /** Sets the value of the 'explanation' field */
    public gr.grnet.aquarium.message.avro.gen.ChargeslotMsg.Builder setExplanation(java.lang.String value) {
      validate(fields()[3], value);
      this.explanation = value;
      fieldSetFlags()[3] = true;
      return this; 
    }
    
    /** Checks whether the 'explanation' field has been set */
    public boolean hasExplanation() {
      return fieldSetFlags()[3];
    }
    
    /** Clears the value of the 'explanation' field */
    public gr.grnet.aquarium.message.avro.gen.ChargeslotMsg.Builder clearExplanation() {
      explanation = null;
      fieldSetFlags()[3] = false;
      return this;
    }

    /** Gets the value of the 'creditsToSubtract' field */
    public java.lang.String getCreditsToSubtract() {
      return creditsToSubtract;
    }
    
    /** Sets the value of the 'creditsToSubtract' field */
    public gr.grnet.aquarium.message.avro.gen.ChargeslotMsg.Builder setCreditsToSubtract(java.lang.String value) {
      validate(fields()[4], value);
      this.creditsToSubtract = value;
      fieldSetFlags()[4] = true;
      return this; 
    }
    
    /** Checks whether the 'creditsToSubtract' field has been set */
    public boolean hasCreditsToSubtract() {
      return fieldSetFlags()[4];
    }
    
    /** Clears the value of the 'creditsToSubtract' field */
    public gr.grnet.aquarium.message.avro.gen.ChargeslotMsg.Builder clearCreditsToSubtract() {
      creditsToSubtract = null;
      fieldSetFlags()[4] = false;
      return this;
    }

    @Override
    public ChargeslotMsg build() {
      try {
        ChargeslotMsg record = new ChargeslotMsg();
        record.startMillis = fieldSetFlags()[0] ? this.startMillis : (java.lang.Long) defaultValue(fields()[0]);
        record.stopMillis = fieldSetFlags()[1] ? this.stopMillis : (java.lang.Long) defaultValue(fields()[1]);
        record.unitPrice = fieldSetFlags()[2] ? this.unitPrice : (java.lang.String) defaultValue(fields()[2]);
        record.explanation = fieldSetFlags()[3] ? this.explanation : (java.lang.String) defaultValue(fields()[3]);
        record.creditsToSubtract = fieldSetFlags()[4] ? this.creditsToSubtract : (java.lang.String) defaultValue(fields()[4]);
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }
}
