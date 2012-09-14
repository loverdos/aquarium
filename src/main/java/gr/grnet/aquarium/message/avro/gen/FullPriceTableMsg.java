/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package gr.grnet.aquarium.message.avro.gen;  
@SuppressWarnings("all")
public class FullPriceTableMsg extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"FullPriceTableMsg\",\"namespace\":\"gr.grnet.aquarium.message.avro.gen\",\"fields\":[{\"name\":\"perResource\",\"type\":{\"type\":\"map\",\"values\":{\"type\":\"map\",\"values\":{\"type\":\"record\",\"name\":\"SelectorValueMsg\",\"fields\":[{\"name\":\"selectorValue\",\"type\":[{\"type\":\"record\",\"name\":\"EffectivePriceTableMsg\",\"fields\":[{\"name\":\"priceOverrides\",\"type\":{\"type\":\"array\",\"items\":{\"type\":\"record\",\"name\":\"EffectiveUnitPriceMsg\",\"fields\":[{\"name\":\"unitPrice\",\"type\":\"double\"},{\"name\":\"when\",\"type\":[{\"type\":\"record\",\"name\":\"CronSpecTupleMsg\",\"fields\":[{\"name\":\"a\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"b\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}}]},\"null\"]}]}}}]},{\"type\":\"map\",\"values\":\"SelectorValueMsg\",\"avro.java.string\":\"String\"}]}]},\"avro.java.string\":\"String\"},\"avro.java.string\":\"String\"}}]}");
  @Deprecated public java.util.Map<java.lang.String,java.util.Map<java.lang.String,gr.grnet.aquarium.message.avro.gen.SelectorValueMsg>> perResource;
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call. 
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return perResource;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  // Used by DatumReader.  Applications should not call. 
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: perResource = (java.util.Map<java.lang.String,java.util.Map<java.lang.String,gr.grnet.aquarium.message.avro.gen.SelectorValueMsg>>)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'perResource' field.
   */
  public java.util.Map<java.lang.String,java.util.Map<java.lang.String,gr.grnet.aquarium.message.avro.gen.SelectorValueMsg>> getPerResource() {
    return perResource;
  }

  /**
   * Sets the value of the 'perResource' field.
   * @param value the value to set.
   */
  public void setPerResource(java.util.Map<java.lang.String,java.util.Map<java.lang.String,gr.grnet.aquarium.message.avro.gen.SelectorValueMsg>> value) {
    this.perResource = value;
  }

  /** Creates a new FullPriceTableMsg RecordBuilder */
  public static gr.grnet.aquarium.message.avro.gen.FullPriceTableMsg.Builder newBuilder() {
    return new gr.grnet.aquarium.message.avro.gen.FullPriceTableMsg.Builder();
  }
  
  /** Creates a new FullPriceTableMsg RecordBuilder by copying an existing Builder */
  public static gr.grnet.aquarium.message.avro.gen.FullPriceTableMsg.Builder newBuilder(gr.grnet.aquarium.message.avro.gen.FullPriceTableMsg.Builder other) {
    return new gr.grnet.aquarium.message.avro.gen.FullPriceTableMsg.Builder(other);
  }
  
  /** Creates a new FullPriceTableMsg RecordBuilder by copying an existing FullPriceTableMsg instance */
  public static gr.grnet.aquarium.message.avro.gen.FullPriceTableMsg.Builder newBuilder(gr.grnet.aquarium.message.avro.gen.FullPriceTableMsg other) {
    return new gr.grnet.aquarium.message.avro.gen.FullPriceTableMsg.Builder(other);
  }
  
  /**
   * RecordBuilder for FullPriceTableMsg instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<FullPriceTableMsg>
    implements org.apache.avro.data.RecordBuilder<FullPriceTableMsg> {

    private java.util.Map<java.lang.String,java.util.Map<java.lang.String,gr.grnet.aquarium.message.avro.gen.SelectorValueMsg>> perResource;

    /** Creates a new Builder */
    private Builder() {
      super(gr.grnet.aquarium.message.avro.gen.FullPriceTableMsg.SCHEMA$);
    }
    
    /** Creates a Builder by copying an existing Builder */
    private Builder(gr.grnet.aquarium.message.avro.gen.FullPriceTableMsg.Builder other) {
      super(other);
    }
    
    /** Creates a Builder by copying an existing FullPriceTableMsg instance */
    private Builder(gr.grnet.aquarium.message.avro.gen.FullPriceTableMsg other) {
            super(gr.grnet.aquarium.message.avro.gen.FullPriceTableMsg.SCHEMA$);
      if (isValidValue(fields()[0], other.perResource)) {
        this.perResource = (java.util.Map<java.lang.String,java.util.Map<java.lang.String,gr.grnet.aquarium.message.avro.gen.SelectorValueMsg>>) data().deepCopy(fields()[0].schema(), other.perResource);
        fieldSetFlags()[0] = true;
      }
    }

    /** Gets the value of the 'perResource' field */
    public java.util.Map<java.lang.String,java.util.Map<java.lang.String,gr.grnet.aquarium.message.avro.gen.SelectorValueMsg>> getPerResource() {
      return perResource;
    }
    
    /** Sets the value of the 'perResource' field */
    public gr.grnet.aquarium.message.avro.gen.FullPriceTableMsg.Builder setPerResource(java.util.Map<java.lang.String,java.util.Map<java.lang.String,gr.grnet.aquarium.message.avro.gen.SelectorValueMsg>> value) {
      validate(fields()[0], value);
      this.perResource = value;
      fieldSetFlags()[0] = true;
      return this; 
    }
    
    /** Checks whether the 'perResource' field has been set */
    public boolean hasPerResource() {
      return fieldSetFlags()[0];
    }
    
    /** Clears the value of the 'perResource' field */
    public gr.grnet.aquarium.message.avro.gen.FullPriceTableMsg.Builder clearPerResource() {
      perResource = null;
      fieldSetFlags()[0] = false;
      return this;
    }

    @Override
    public FullPriceTableMsg build() {
      try {
        FullPriceTableMsg record = new FullPriceTableMsg();
        record.perResource = fieldSetFlags()[0] ? this.perResource : (java.util.Map<java.lang.String,java.util.Map<java.lang.String,gr.grnet.aquarium.message.avro.gen.SelectorValueMsg>>) defaultValue(fields()[0]);
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }
}