/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package gr.grnet.aquarium.message.avro.gen;  
@SuppressWarnings("all")
public class EventEntryMsg extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"EventEntryMsg\",\"namespace\":\"gr.grnet.aquarium.message.avro.gen\",\"fields\":[{\"name\":\"eventType\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"details\",\"type\":{\"type\":\"array\",\"items\":{\"type\":\"record\",\"name\":\"ChargeEntryMsg\",\"fields\":[{\"name\":\"id\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"unitPrice\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"startTime\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"endTime\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"totalCredits\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"totalElapsedTime\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"totalUnits\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}}]}}}]}");
  @Deprecated public java.lang.String eventType;
  @Deprecated public java.util.List<gr.grnet.aquarium.message.avro.gen.ChargeEntryMsg> details;

  /**
   * Default constructor.
   */
  public EventEntryMsg() {}

  /**
   * All-args constructor.
   */
  public EventEntryMsg(java.lang.String eventType, java.util.List<gr.grnet.aquarium.message.avro.gen.ChargeEntryMsg> details) {
    this.eventType = eventType;
    this.details = details;
  }

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
    case 0: eventType = (java.lang.String)value$; break;
    case 1: details = (java.util.List<gr.grnet.aquarium.message.avro.gen.ChargeEntryMsg>)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'eventType' field.
   */
  public java.lang.String getEventType() {
    return eventType;
  }

  /**
   * Sets the value of the 'eventType' field.
   * @param value the value to set.
   */
  public void setEventType(java.lang.String value) {
    this.eventType = value;
  }

  /**
   * Gets the value of the 'details' field.
   */
  public java.util.List<gr.grnet.aquarium.message.avro.gen.ChargeEntryMsg> getDetails() {
    return details;
  }

  /**
   * Sets the value of the 'details' field.
   * @param value the value to set.
   */
  public void setDetails(java.util.List<gr.grnet.aquarium.message.avro.gen.ChargeEntryMsg> value) {
    this.details = value;
  }

  /** Creates a new EventEntryMsg RecordBuilder */
  public static gr.grnet.aquarium.message.avro.gen.EventEntryMsg.Builder newBuilder() {
    return new gr.grnet.aquarium.message.avro.gen.EventEntryMsg.Builder();
  }
  
  /** Creates a new EventEntryMsg RecordBuilder by copying an existing Builder */
  public static gr.grnet.aquarium.message.avro.gen.EventEntryMsg.Builder newBuilder(gr.grnet.aquarium.message.avro.gen.EventEntryMsg.Builder other) {
    return new gr.grnet.aquarium.message.avro.gen.EventEntryMsg.Builder(other);
  }
  
  /** Creates a new EventEntryMsg RecordBuilder by copying an existing EventEntryMsg instance */
  public static gr.grnet.aquarium.message.avro.gen.EventEntryMsg.Builder newBuilder(gr.grnet.aquarium.message.avro.gen.EventEntryMsg other) {
    return new gr.grnet.aquarium.message.avro.gen.EventEntryMsg.Builder(other);
  }
  
  /**
   * RecordBuilder for EventEntryMsg instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<EventEntryMsg>
    implements org.apache.avro.data.RecordBuilder<EventEntryMsg> {

    private java.lang.String eventType;
    private java.util.List<gr.grnet.aquarium.message.avro.gen.ChargeEntryMsg> details;

    /** Creates a new Builder */
    private Builder() {
      super(gr.grnet.aquarium.message.avro.gen.EventEntryMsg.SCHEMA$);
    }
    
    /** Creates a Builder by copying an existing Builder */
    private Builder(gr.grnet.aquarium.message.avro.gen.EventEntryMsg.Builder other) {
      super(other);
    }
    
    /** Creates a Builder by copying an existing EventEntryMsg instance */
    private Builder(gr.grnet.aquarium.message.avro.gen.EventEntryMsg other) {
            super(gr.grnet.aquarium.message.avro.gen.EventEntryMsg.SCHEMA$);
      if (isValidValue(fields()[0], other.eventType)) {
        this.eventType = (java.lang.String) data().deepCopy(fields()[0].schema(), other.eventType);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.details)) {
        this.details = (java.util.List<gr.grnet.aquarium.message.avro.gen.ChargeEntryMsg>) data().deepCopy(fields()[1].schema(), other.details);
        fieldSetFlags()[1] = true;
      }
    }

    /** Gets the value of the 'eventType' field */
    public java.lang.String getEventType() {
      return eventType;
    }
    
    /** Sets the value of the 'eventType' field */
    public gr.grnet.aquarium.message.avro.gen.EventEntryMsg.Builder setEventType(java.lang.String value) {
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
    public gr.grnet.aquarium.message.avro.gen.EventEntryMsg.Builder clearEventType() {
      eventType = null;
      fieldSetFlags()[0] = false;
      return this;
    }

    /** Gets the value of the 'details' field */
    public java.util.List<gr.grnet.aquarium.message.avro.gen.ChargeEntryMsg> getDetails() {
      return details;
    }
    
    /** Sets the value of the 'details' field */
    public gr.grnet.aquarium.message.avro.gen.EventEntryMsg.Builder setDetails(java.util.List<gr.grnet.aquarium.message.avro.gen.ChargeEntryMsg> value) {
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
    public gr.grnet.aquarium.message.avro.gen.EventEntryMsg.Builder clearDetails() {
      details = null;
      fieldSetFlags()[1] = false;
      return this;
    }

    @Override
    public EventEntryMsg build() {
      try {
        EventEntryMsg record = new EventEntryMsg();
        record.eventType = fieldSetFlags()[0] ? this.eventType : (java.lang.String) defaultValue(fields()[0]);
        record.details = fieldSetFlags()[1] ? this.details : (java.util.List<gr.grnet.aquarium.message.avro.gen.ChargeEntryMsg>) defaultValue(fields()[1]);
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }
}
