/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package gr.grnet.aquarium.message.avro.gen;  
@SuppressWarnings("all")
public class _ResourceEntry extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"_ResourceEntry\",\"namespace\":\"gr.grnet.aquarium.message.avro.gen\",\"fields\":[{\"name\":\"resourceName\",\"type\":\"string\"},{\"name\":\"resourceType\",\"type\":\"string\"},{\"name\":\"unitName\",\"type\":\"string\"},{\"name\":\"totalCredits\",\"type\":\"string\"},{\"name\":\"details\",\"type\":{\"type\":\"array\",\"items\":{\"type\":\"record\",\"name\":\"_EventEntry\",\"fields\":[{\"name\":\"eventType\",\"type\":\"string\"},{\"name\":\"details\",\"type\":{\"type\":\"array\",\"items\":{\"type\":\"record\",\"name\":\"_ChargeEntry\",\"fields\":[{\"name\":\"id\",\"type\":\"string\"},{\"name\":\"unitPrice\",\"type\":\"string\"},{\"name\":\"startTime\",\"type\":\"string\"},{\"name\":\"endTime\",\"type\":\"string\"},{\"name\":\"ellapsedTime\",\"type\":\"string\"},{\"name\":\"credits\",\"type\":\"string\"}]}}}]}}}]}");
  @Deprecated public java.lang.CharSequence resourceName;
  @Deprecated public java.lang.CharSequence resourceType;
  @Deprecated public java.lang.CharSequence unitName;
  @Deprecated public java.lang.CharSequence totalCredits;
  @Deprecated public java.util.List<gr.grnet.aquarium.message.avro.gen._EventEntry> details;
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call. 
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return resourceName;
    case 1: return resourceType;
    case 2: return unitName;
    case 3: return totalCredits;
    case 4: return details;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  // Used by DatumReader.  Applications should not call. 
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: resourceName = (java.lang.CharSequence)value$; break;
    case 1: resourceType = (java.lang.CharSequence)value$; break;
    case 2: unitName = (java.lang.CharSequence)value$; break;
    case 3: totalCredits = (java.lang.CharSequence)value$; break;
    case 4: details = (java.util.List<gr.grnet.aquarium.message.avro.gen._EventEntry>)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'resourceName' field.
   */
  public java.lang.CharSequence getResourceName() {
    return resourceName;
  }

  /**
   * Sets the value of the 'resourceName' field.
   * @param value the value to set.
   */
  public void setResourceName(java.lang.CharSequence value) {
    this.resourceName = value;
  }

  /**
   * Gets the value of the 'resourceType' field.
   */
  public java.lang.CharSequence getResourceType() {
    return resourceType;
  }

  /**
   * Sets the value of the 'resourceType' field.
   * @param value the value to set.
   */
  public void setResourceType(java.lang.CharSequence value) {
    this.resourceType = value;
  }

  /**
   * Gets the value of the 'unitName' field.
   */
  public java.lang.CharSequence getUnitName() {
    return unitName;
  }

  /**
   * Sets the value of the 'unitName' field.
   * @param value the value to set.
   */
  public void setUnitName(java.lang.CharSequence value) {
    this.unitName = value;
  }

  /**
   * Gets the value of the 'totalCredits' field.
   */
  public java.lang.CharSequence getTotalCredits() {
    return totalCredits;
  }

  /**
   * Sets the value of the 'totalCredits' field.
   * @param value the value to set.
   */
  public void setTotalCredits(java.lang.CharSequence value) {
    this.totalCredits = value;
  }

  /**
   * Gets the value of the 'details' field.
   */
  public java.util.List<gr.grnet.aquarium.message.avro.gen._EventEntry> getDetails() {
    return details;
  }

  /**
   * Sets the value of the 'details' field.
   * @param value the value to set.
   */
  public void setDetails(java.util.List<gr.grnet.aquarium.message.avro.gen._EventEntry> value) {
    this.details = value;
  }

  /** Creates a new _ResourceEntry RecordBuilder */
  public static gr.grnet.aquarium.message.avro.gen._ResourceEntry.Builder newBuilder() {
    return new gr.grnet.aquarium.message.avro.gen._ResourceEntry.Builder();
  }
  
  /** Creates a new _ResourceEntry RecordBuilder by copying an existing Builder */
  public static gr.grnet.aquarium.message.avro.gen._ResourceEntry.Builder newBuilder(gr.grnet.aquarium.message.avro.gen._ResourceEntry.Builder other) {
    return new gr.grnet.aquarium.message.avro.gen._ResourceEntry.Builder(other);
  }
  
  /** Creates a new _ResourceEntry RecordBuilder by copying an existing _ResourceEntry instance */
  public static gr.grnet.aquarium.message.avro.gen._ResourceEntry.Builder newBuilder(gr.grnet.aquarium.message.avro.gen._ResourceEntry other) {
    return new gr.grnet.aquarium.message.avro.gen._ResourceEntry.Builder(other);
  }
  
  /**
   * RecordBuilder for _ResourceEntry instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<_ResourceEntry>
    implements org.apache.avro.data.RecordBuilder<_ResourceEntry> {

    private java.lang.CharSequence resourceName;
    private java.lang.CharSequence resourceType;
    private java.lang.CharSequence unitName;
    private java.lang.CharSequence totalCredits;
    private java.util.List<gr.grnet.aquarium.message.avro.gen._EventEntry> details;

    /** Creates a new Builder */
    private Builder() {
      super(gr.grnet.aquarium.message.avro.gen._ResourceEntry.SCHEMA$);
    }
    
    /** Creates a Builder by copying an existing Builder */
    private Builder(gr.grnet.aquarium.message.avro.gen._ResourceEntry.Builder other) {
      super(other);
    }
    
    /** Creates a Builder by copying an existing _ResourceEntry instance */
    private Builder(gr.grnet.aquarium.message.avro.gen._ResourceEntry other) {
            super(gr.grnet.aquarium.message.avro.gen._ResourceEntry.SCHEMA$);
      if (isValidValue(fields()[0], other.resourceName)) {
        this.resourceName = (java.lang.CharSequence) data().deepCopy(fields()[0].schema(), other.resourceName);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.resourceType)) {
        this.resourceType = (java.lang.CharSequence) data().deepCopy(fields()[1].schema(), other.resourceType);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.unitName)) {
        this.unitName = (java.lang.CharSequence) data().deepCopy(fields()[2].schema(), other.unitName);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.totalCredits)) {
        this.totalCredits = (java.lang.CharSequence) data().deepCopy(fields()[3].schema(), other.totalCredits);
        fieldSetFlags()[3] = true;
      }
      if (isValidValue(fields()[4], other.details)) {
        this.details = (java.util.List<gr.grnet.aquarium.message.avro.gen._EventEntry>) data().deepCopy(fields()[4].schema(), other.details);
        fieldSetFlags()[4] = true;
      }
    }

    /** Gets the value of the 'resourceName' field */
    public java.lang.CharSequence getResourceName() {
      return resourceName;
    }
    
    /** Sets the value of the 'resourceName' field */
    public gr.grnet.aquarium.message.avro.gen._ResourceEntry.Builder setResourceName(java.lang.CharSequence value) {
      validate(fields()[0], value);
      this.resourceName = value;
      fieldSetFlags()[0] = true;
      return this; 
    }
    
    /** Checks whether the 'resourceName' field has been set */
    public boolean hasResourceName() {
      return fieldSetFlags()[0];
    }
    
    /** Clears the value of the 'resourceName' field */
    public gr.grnet.aquarium.message.avro.gen._ResourceEntry.Builder clearResourceName() {
      resourceName = null;
      fieldSetFlags()[0] = false;
      return this;
    }

    /** Gets the value of the 'resourceType' field */
    public java.lang.CharSequence getResourceType() {
      return resourceType;
    }
    
    /** Sets the value of the 'resourceType' field */
    public gr.grnet.aquarium.message.avro.gen._ResourceEntry.Builder setResourceType(java.lang.CharSequence value) {
      validate(fields()[1], value);
      this.resourceType = value;
      fieldSetFlags()[1] = true;
      return this; 
    }
    
    /** Checks whether the 'resourceType' field has been set */
    public boolean hasResourceType() {
      return fieldSetFlags()[1];
    }
    
    /** Clears the value of the 'resourceType' field */
    public gr.grnet.aquarium.message.avro.gen._ResourceEntry.Builder clearResourceType() {
      resourceType = null;
      fieldSetFlags()[1] = false;
      return this;
    }

    /** Gets the value of the 'unitName' field */
    public java.lang.CharSequence getUnitName() {
      return unitName;
    }
    
    /** Sets the value of the 'unitName' field */
    public gr.grnet.aquarium.message.avro.gen._ResourceEntry.Builder setUnitName(java.lang.CharSequence value) {
      validate(fields()[2], value);
      this.unitName = value;
      fieldSetFlags()[2] = true;
      return this; 
    }
    
    /** Checks whether the 'unitName' field has been set */
    public boolean hasUnitName() {
      return fieldSetFlags()[2];
    }
    
    /** Clears the value of the 'unitName' field */
    public gr.grnet.aquarium.message.avro.gen._ResourceEntry.Builder clearUnitName() {
      unitName = null;
      fieldSetFlags()[2] = false;
      return this;
    }

    /** Gets the value of the 'totalCredits' field */
    public java.lang.CharSequence getTotalCredits() {
      return totalCredits;
    }
    
    /** Sets the value of the 'totalCredits' field */
    public gr.grnet.aquarium.message.avro.gen._ResourceEntry.Builder setTotalCredits(java.lang.CharSequence value) {
      validate(fields()[3], value);
      this.totalCredits = value;
      fieldSetFlags()[3] = true;
      return this; 
    }
    
    /** Checks whether the 'totalCredits' field has been set */
    public boolean hasTotalCredits() {
      return fieldSetFlags()[3];
    }
    
    /** Clears the value of the 'totalCredits' field */
    public gr.grnet.aquarium.message.avro.gen._ResourceEntry.Builder clearTotalCredits() {
      totalCredits = null;
      fieldSetFlags()[3] = false;
      return this;
    }

    /** Gets the value of the 'details' field */
    public java.util.List<gr.grnet.aquarium.message.avro.gen._EventEntry> getDetails() {
      return details;
    }
    
    /** Sets the value of the 'details' field */
    public gr.grnet.aquarium.message.avro.gen._ResourceEntry.Builder setDetails(java.util.List<gr.grnet.aquarium.message.avro.gen._EventEntry> value) {
      validate(fields()[4], value);
      this.details = value;
      fieldSetFlags()[4] = true;
      return this; 
    }
    
    /** Checks whether the 'details' field has been set */
    public boolean hasDetails() {
      return fieldSetFlags()[4];
    }
    
    /** Clears the value of the 'details' field */
    public gr.grnet.aquarium.message.avro.gen._ResourceEntry.Builder clearDetails() {
      details = null;
      fieldSetFlags()[4] = false;
      return this;
    }

    @Override
    public _ResourceEntry build() {
      try {
        _ResourceEntry record = new _ResourceEntry();
        record.resourceName = fieldSetFlags()[0] ? this.resourceName : (java.lang.CharSequence) defaultValue(fields()[0]);
        record.resourceType = fieldSetFlags()[1] ? this.resourceType : (java.lang.CharSequence) defaultValue(fields()[1]);
        record.unitName = fieldSetFlags()[2] ? this.unitName : (java.lang.CharSequence) defaultValue(fields()[2]);
        record.totalCredits = fieldSetFlags()[3] ? this.totalCredits : (java.lang.CharSequence) defaultValue(fields()[3]);
        record.details = fieldSetFlags()[4] ? this.details : (java.util.List<gr.grnet.aquarium.message.avro.gen._EventEntry>) defaultValue(fields()[4]);
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }
}
