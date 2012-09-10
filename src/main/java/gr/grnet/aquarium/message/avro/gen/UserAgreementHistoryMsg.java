/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package gr.grnet.aquarium.message.avro.gen;  
@SuppressWarnings("all")
public class UserAgreementHistoryMsg extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"UserAgreementHistoryMsg\",\"namespace\":\"gr.grnet.aquarium.message.avro.gen\",\"fields\":[{\"name\":\"agreements\",\"type\":{\"type\":\"array\",\"items\":{\"type\":\"record\",\"name\":\"UserAgreementMsg\",\"fields\":[{\"name\":\"id\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"relatedIMEventOriginalID\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"userID\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"validFromMillis\",\"type\":\"long\"},{\"name\":\"validToMillis\",\"type\":\"long\"},{\"name\":\"role\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"fullPriceTableRef\",\"type\":[{\"type\":\"record\",\"name\":\"FullPriceTableMsg\",\"fields\":[{\"name\":\"perResource\",\"type\":{\"type\":\"map\",\"values\":{\"type\":\"map\",\"values\":{\"type\":\"record\",\"name\":\"SelectorValueMsg\",\"fields\":[{\"name\":\"selectorValue\",\"type\":[{\"type\":\"record\",\"name\":\"EffectivePriceTableMsg\",\"fields\":[{\"name\":\"priceOverrides\",\"type\":{\"type\":\"array\",\"items\":{\"type\":\"record\",\"name\":\"EffectiveUnitPriceMsg\",\"fields\":[{\"name\":\"unitPrice\",\"type\":\"double\"},{\"name\":\"when\",\"type\":[{\"type\":\"record\",\"name\":\"CronSpecTupleMsg\",\"fields\":[{\"name\":\"a\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"b\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}}]},\"null\"]}]}}}]},{\"type\":\"map\",\"values\":\"SelectorValueMsg\",\"avro.java.string\":\"String\"}]}]},\"avro.java.string\":\"String\"},\"avro.java.string\":\"String\"}}]},\"null\"]}]}}}]}");
  @Deprecated public java.util.List<gr.grnet.aquarium.message.avro.gen.UserAgreementMsg> agreements;
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call. 
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return agreements;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  // Used by DatumReader.  Applications should not call. 
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: agreements = (java.util.List<gr.grnet.aquarium.message.avro.gen.UserAgreementMsg>)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
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
      if (isValidValue(fields()[0], other.agreements)) {
        this.agreements = (java.util.List<gr.grnet.aquarium.message.avro.gen.UserAgreementMsg>) data().deepCopy(fields()[0].schema(), other.agreements);
        fieldSetFlags()[0] = true;
      }
    }

    /** Gets the value of the 'agreements' field */
    public java.util.List<gr.grnet.aquarium.message.avro.gen.UserAgreementMsg> getAgreements() {
      return agreements;
    }
    
    /** Sets the value of the 'agreements' field */
    public gr.grnet.aquarium.message.avro.gen.UserAgreementHistoryMsg.Builder setAgreements(java.util.List<gr.grnet.aquarium.message.avro.gen.UserAgreementMsg> value) {
      validate(fields()[0], value);
      this.agreements = value;
      fieldSetFlags()[0] = true;
      return this; 
    }
    
    /** Checks whether the 'agreements' field has been set */
    public boolean hasAgreements() {
      return fieldSetFlags()[0];
    }
    
    /** Clears the value of the 'agreements' field */
    public gr.grnet.aquarium.message.avro.gen.UserAgreementHistoryMsg.Builder clearAgreements() {
      agreements = null;
      fieldSetFlags()[0] = false;
      return this;
    }

    @Override
    public UserAgreementHistoryMsg build() {
      try {
        UserAgreementHistoryMsg record = new UserAgreementHistoryMsg();
        record.agreements = fieldSetFlags()[0] ? this.agreements : (java.util.List<gr.grnet.aquarium.message.avro.gen.UserAgreementMsg>) defaultValue(fields()[0]);
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }
}
