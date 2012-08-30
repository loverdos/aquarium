/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package gr.grnet.aquarium.message.avro.gen;  
@SuppressWarnings("all")
public class _EventEntry extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"_EventEntry\",\"namespace\":\"gr.grnet.aquarium.message.avro.gen\",\"fields\":[{\"name\":\"eventType\",\"type\":\"string\"},{\"name\":\"details\",\"type\":{\"type\":\"array\",\"items\":{\"type\":\"record\",\"name\":\"_ChargeEntry\",\"fields\":[{\"name\":\"id\",\"type\":\"string\"},{\"name\":\"unitPrice\",\"type\":\"string\"},{\"name\":\"startTime\",\"type\":\"string\"},{\"name\":\"endTime\",\"type\":\"string\"},{\"name\":\"ellapsedTime\",\"type\":\"string\"},{\"name\":\"credits\",\"type\":\"string\"}]}}}]}");
  @Deprecated public java.lang.CharSequence eventType;
  @Deprecated public java.util.List<gr.grnet.aquarium.message.avro.gen._ChargeEntry> details;
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call. 
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return eventType;
    case 1: return details;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  // Used by DatumReader.  Applications should not call. 
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: eventType = (java.lang.CharSequence)value$; break;
    case 1: details = (java.util.List<gr.grnet.aquarium.message.avro.gen._ChargeEntry>)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'eventType' field.
   */
  public java.lang.CharSequence getEventType() {
    return eventType;
  }

  /**
   * Sets the value of the 'eventType' field.
   * @param value the value to set.
   */
  public void setEventType(java.lang.CharSequence value) {
    this.eventType = value;
  }

  /**
   * Gets the value of the 'details' field.
   */
  public java.util.List<gr.grnet.aquarium.message.avro.gen._ChargeEntry> getDetails() {
    return details;
  }

  /**
   * Sets the value of the 'details' field.
   * @param value the value to set.
   */
  public void setDetails(java.util.List<gr.grnet.aquarium.message.avro.gen._ChargeEntry> value) {
    this.details = value;
  }

  /** Creates a new _EventEntry RecordBuilder */
  public static gr.grnet.aquarium.message.avro.gen._EventEntry.Builder newBuilder() {
    return new gr.grnet.aquarium.message.avro.gen._EventEntry.Builder();
  }
  
  /** Creates a new _EventEntry RecordBuilder by copying an existing Builder */
  public static gr.grnet.aquarium.message.avro.gen._EventEntry.Builder newBuilder(gr.grnet.aquarium.message.avro.gen._EventEntry.Builder other) {
    return new gr.grnet.aquarium.message.avro.gen._EventEntry.Builder(other);
  }
  
  /** Creates a new _EventEntry RecordBuilder by copying an existing _EventEntry instance */
  public static gr.grnet.aquarium.message.avro.gen._EventEntry.Builder newBuilder(gr.grnet.aquarium.message.avro.gen._EventEntry other) {
    return new gr.grnet.aquarium.message.avro.gen._EventEntry.Builder(other);
  }
  
  /**
   * RecordBuilder for _EventEntry instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<_EventEntry>
    implements org.apache.avro.data.RecordBuilder<_EventEntry> {

    private java.lang.CharSequence eventType;
    private java.util.List<gr.grnet.aquarium.message.avro.gen._ChargeEntry> details;

    /** Creates a new Builder */
    private Builder() {
      super(gr.grnet.aquarium.message.avro.gen._EventEntry.SCHEMA$);
    }
    
    /** Creates a Builder by copying an existing Builder */
    private Builder(gr.grnet.aquarium.message.avro.gen._EventEntry.Builder other) {
      super(other);
    }
    
    /** Creates a Builder by copying an existing _EventEntry instance */
    private Builder(gr.grnet.aquarium.message.avro.gen._EventEntry other) {
            super(gr.grnet.aquarium.message.avro.gen._EventEntry.SCHEMA$);
      if (isValidValue(fields()[0], other.eventType)) {
        this.eventType = (java.lang.CharSequence) data().deepCopy(fields()[0].schema(), other.eventType);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.details)) {
        this.details = (java.util.List<gr.grnet.aquarium.message.avro.gen._ChargeEntry>) data().deepCopy(fields()[1].schema(), other.details);
        fieldSetFlags()[1] = true;
      }
    }

    /** Gets the value of the 'eventType' field */
    public java.lang.CharSequence getEventType() {
      return eventType;
    }
    
    /** Sets the value of the 'eventType' field */
    public gr.grnet.aquarium.message.avro.gen._EventEntry.Builder setEventType(java.lang.CharSequence value) {
      validate(fields()[0], value);
      this.eventType = value;
      fieldSetFlags()[0] = true;
      return this; 
    }
    
    /** Checks whether the 'eventType' field has been set */
    public boolean hasEventType() {
      return fieldSetFlags()[0];
    }
    
    /** Clears the value of the 'eventType' field */
    public gr.grnet.aquarium.message.avro.gen._EventEntry.Builder clearEventType() {
      eventType = null;
      fieldSetFlags()[0] = false;
      return this;
    }

    /** Gets the value of the 'details' field */
    public java.util.List<gr.grnet.aquarium.message.avro.gen._ChargeEntry> getDetails() {
      return details;
    }
    
    /** Sets the value of the 'details' field */
    public gr.grnet.aquarium.message.avro.gen._EventEntry.Builder setDetails(java.util.List<gr.grnet.aquarium.message.avro.gen._ChargeEntry> value) {
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
    public gr.grnet.aquarium.message.avro.gen._EventEntry.Builder clearDetails() {
      details = null;
      fieldSetFlags()[1] = false;
      return this;
    }

    @Override
    public _EventEntry build() {
      try {
        _EventEntry record = new _EventEntry();
        record.eventType = fieldSetFlags()[0] ? this.eventType : (java.lang.CharSequence) defaultValue(fields()[0]);
        record.details = fieldSetFlags()[1] ? this.details : (java.util.List<gr.grnet.aquarium.message.avro.gen._ChargeEntry>) defaultValue(fields()[1]);
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }
}
