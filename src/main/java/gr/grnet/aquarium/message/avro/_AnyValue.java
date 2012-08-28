/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package gr.grnet.aquarium.message.avro;  
@SuppressWarnings("all")
public class _AnyValue extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"_AnyValue\",\"namespace\":\"gr.grnet.aquarium.message.avro\",\"fields\":[{\"name\":\"anyValue\",\"type\":[\"null\",\"int\",\"long\",\"boolean\",\"double\",\"bytes\",\"string\"]}]}");
  @Deprecated public java.lang.Object anyValue;
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call. 
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return anyValue;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  // Used by DatumReader.  Applications should not call. 
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: anyValue = (java.lang.Object)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'anyValue' field.
   */
  public java.lang.Object getAnyValue() {
    return anyValue;
  }

  /**
   * Sets the value of the 'anyValue' field.
   * @param value the value to set.
   */
  public void setAnyValue(java.lang.Object value) {
    this.anyValue = value;
  }

  /** Creates a new _AnyValue RecordBuilder */
  public static gr.grnet.aquarium.message.avro._AnyValue.Builder newBuilder() {
    return new gr.grnet.aquarium.message.avro._AnyValue.Builder();
  }
  
  /** Creates a new _AnyValue RecordBuilder by copying an existing Builder */
  public static gr.grnet.aquarium.message.avro._AnyValue.Builder newBuilder(gr.grnet.aquarium.message.avro._AnyValue.Builder other) {
    return new gr.grnet.aquarium.message.avro._AnyValue.Builder(other);
  }
  
  /** Creates a new _AnyValue RecordBuilder by copying an existing _AnyValue instance */
  public static gr.grnet.aquarium.message.avro._AnyValue.Builder newBuilder(gr.grnet.aquarium.message.avro._AnyValue other) {
    return new gr.grnet.aquarium.message.avro._AnyValue.Builder(other);
  }
  
  /**
   * RecordBuilder for _AnyValue instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<_AnyValue>
    implements org.apache.avro.data.RecordBuilder<_AnyValue> {

    private java.lang.Object anyValue;

    /** Creates a new Builder */
    private Builder() {
      super(gr.grnet.aquarium.message.avro._AnyValue.SCHEMA$);
    }
    
    /** Creates a Builder by copying an existing Builder */
    private Builder(gr.grnet.aquarium.message.avro._AnyValue.Builder other) {
      super(other);
    }
    
    /** Creates a Builder by copying an existing _AnyValue instance */
    private Builder(gr.grnet.aquarium.message.avro._AnyValue other) {
            super(gr.grnet.aquarium.message.avro._AnyValue.SCHEMA$);
      if (isValidValue(fields()[0], other.anyValue)) {
        this.anyValue = (java.lang.Object) data().deepCopy(fields()[0].schema(), other.anyValue);
        fieldSetFlags()[0] = true;
      }
    }

    /** Gets the value of the 'anyValue' field */
    public java.lang.Object getAnyValue() {
      return anyValue;
    }
    
    /** Sets the value of the 'anyValue' field */
    public gr.grnet.aquarium.message.avro._AnyValue.Builder setAnyValue(java.lang.Object value) {
      validate(fields()[0], value);
      this.anyValue = value;
      fieldSetFlags()[0] = true;
      return this; 
    }
    
    /** Checks whether the 'anyValue' field has been set */
    public boolean hasAnyValue() {
      return fieldSetFlags()[0];
    }
    
    /** Clears the value of the 'anyValue' field */
    public gr.grnet.aquarium.message.avro._AnyValue.Builder clearAnyValue() {
      anyValue = null;
      fieldSetFlags()[0] = false;
      return this;
    }

    @Override
    public _AnyValue build() {
      try {
        _AnyValue record = new _AnyValue();
        record.anyValue = fieldSetFlags()[0] ? this.anyValue : (java.lang.Object) defaultValue(fields()[0]);
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }
}
