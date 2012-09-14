/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package gr.grnet.aquarium.message.avro.gen;  
@SuppressWarnings("all")
public class WalletEntryMsg extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"WalletEntryMsg\",\"namespace\":\"gr.grnet.aquarium.message.avro.gen\",\"fields\":[{\"name\":\"userID\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"sumOfCreditsToSubtract\",\"type\":\"double\"},{\"name\":\"oldTotalCredits\",\"type\":\"double\"},{\"name\":\"newTotalCredits\",\"type\":\"double\"},{\"name\":\"whenComputedMillis\",\"type\":\"long\"},{\"name\":\"referenceStartMillis\",\"type\":\"long\"},{\"name\":\"referenceStopMillis\",\"type\":\"long\"},{\"name\":\"billingYear\",\"type\":\"int\"},{\"name\":\"billingMonth\",\"type\":\"int\"},{\"name\":\"billingMonthDay\",\"type\":\"int\"},{\"name\":\"chargeslots\",\"type\":{\"type\":\"array\",\"items\":{\"type\":\"record\",\"name\":\"ChargeslotMsg\",\"fields\":[{\"name\":\"startMillis\",\"type\":\"long\"},{\"name\":\"stopMillis\",\"type\":\"long\"},{\"name\":\"unitPrice\",\"type\":\"double\"},{\"name\":\"explanation\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"},\"default\":\"\"},{\"name\":\"creditsToSubtract\",\"type\":\"double\",\"default\":0.0}]}}},{\"name\":\"resourceEvents\",\"type\":{\"type\":\"array\",\"items\":{\"type\":\"record\",\"name\":\"ResourceEventMsg\",\"fields\":[{\"name\":\"originalID\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"inStoreID\",\"type\":[{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]},{\"name\":\"occurredMillis\",\"type\":\"long\"},{\"name\":\"receivedMillis\",\"type\":\"long\",\"default\":0},{\"name\":\"userID\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"clientID\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"eventVersion\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"},\"default\":\"1.0\"},{\"name\":\"resource\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"instanceID\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"value\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"isSynthetic\",\"type\":\"boolean\",\"default\":false},{\"name\":\"details\",\"type\":{\"type\":\"map\",\"values\":{\"type\":\"record\",\"name\":\"AnyValueMsg\",\"fields\":[{\"name\":\"anyValue\",\"type\":[\"null\",\"int\",\"long\",\"boolean\",\"double\",\"bytes\",{\"type\":\"string\",\"avro.java.string\":\"String\"},{\"type\":\"array\",\"items\":\"AnyValueMsg\"},{\"type\":\"map\",\"values\":\"AnyValueMsg\",\"avro.java.string\":\"String\"}]}]},\"avro.java.string\":\"String\"}}]}}},{\"name\":\"resourceType\",\"type\":{\"type\":\"record\",\"name\":\"ResourceTypeMsg\",\"fields\":[{\"name\":\"name\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"unit\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"chargingBehaviorClass\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}}]}},{\"name\":\"isSynthetic\",\"type\":\"boolean\",\"default\":false}]}");
  @Deprecated public java.lang.String userID;
  @Deprecated public double sumOfCreditsToSubtract;
  @Deprecated public double oldTotalCredits;
  @Deprecated public double newTotalCredits;
  @Deprecated public long whenComputedMillis;
  @Deprecated public long referenceStartMillis;
  @Deprecated public long referenceStopMillis;
  @Deprecated public int billingYear;
  @Deprecated public int billingMonth;
  @Deprecated public int billingMonthDay;
  @Deprecated public java.util.List<gr.grnet.aquarium.message.avro.gen.ChargeslotMsg> chargeslots;
  @Deprecated public java.util.List<gr.grnet.aquarium.message.avro.gen.ResourceEventMsg> resourceEvents;
  @Deprecated public gr.grnet.aquarium.message.avro.gen.ResourceTypeMsg resourceType;
  @Deprecated public boolean isSynthetic;
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call. 
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return userID;
    case 1: return sumOfCreditsToSubtract;
    case 2: return oldTotalCredits;
    case 3: return newTotalCredits;
    case 4: return whenComputedMillis;
    case 5: return referenceStartMillis;
    case 6: return referenceStopMillis;
    case 7: return billingYear;
    case 8: return billingMonth;
    case 9: return billingMonthDay;
    case 10: return chargeslots;
    case 11: return resourceEvents;
    case 12: return resourceType;
    case 13: return isSynthetic;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  // Used by DatumReader.  Applications should not call. 
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: userID = (java.lang.String)value$; break;
    case 1: sumOfCreditsToSubtract = (java.lang.Double)value$; break;
    case 2: oldTotalCredits = (java.lang.Double)value$; break;
    case 3: newTotalCredits = (java.lang.Double)value$; break;
    case 4: whenComputedMillis = (java.lang.Long)value$; break;
    case 5: referenceStartMillis = (java.lang.Long)value$; break;
    case 6: referenceStopMillis = (java.lang.Long)value$; break;
    case 7: billingYear = (java.lang.Integer)value$; break;
    case 8: billingMonth = (java.lang.Integer)value$; break;
    case 9: billingMonthDay = (java.lang.Integer)value$; break;
    case 10: chargeslots = (java.util.List<gr.grnet.aquarium.message.avro.gen.ChargeslotMsg>)value$; break;
    case 11: resourceEvents = (java.util.List<gr.grnet.aquarium.message.avro.gen.ResourceEventMsg>)value$; break;
    case 12: resourceType = (gr.grnet.aquarium.message.avro.gen.ResourceTypeMsg)value$; break;
    case 13: isSynthetic = (java.lang.Boolean)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
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
   * Gets the value of the 'sumOfCreditsToSubtract' field.
   */
  public java.lang.Double getSumOfCreditsToSubtract() {
    return sumOfCreditsToSubtract;
  }

  /**
   * Sets the value of the 'sumOfCreditsToSubtract' field.
   * @param value the value to set.
   */
  public void setSumOfCreditsToSubtract(java.lang.Double value) {
    this.sumOfCreditsToSubtract = value;
  }

  /**
   * Gets the value of the 'oldTotalCredits' field.
   */
  public java.lang.Double getOldTotalCredits() {
    return oldTotalCredits;
  }

  /**
   * Sets the value of the 'oldTotalCredits' field.
   * @param value the value to set.
   */
  public void setOldTotalCredits(java.lang.Double value) {
    this.oldTotalCredits = value;
  }

  /**
   * Gets the value of the 'newTotalCredits' field.
   */
  public java.lang.Double getNewTotalCredits() {
    return newTotalCredits;
  }

  /**
   * Sets the value of the 'newTotalCredits' field.
   * @param value the value to set.
   */
  public void setNewTotalCredits(java.lang.Double value) {
    this.newTotalCredits = value;
  }

  /**
   * Gets the value of the 'whenComputedMillis' field.
   */
  public java.lang.Long getWhenComputedMillis() {
    return whenComputedMillis;
  }

  /**
   * Sets the value of the 'whenComputedMillis' field.
   * @param value the value to set.
   */
  public void setWhenComputedMillis(java.lang.Long value) {
    this.whenComputedMillis = value;
  }

  /**
   * Gets the value of the 'referenceStartMillis' field.
   */
  public java.lang.Long getReferenceStartMillis() {
    return referenceStartMillis;
  }

  /**
   * Sets the value of the 'referenceStartMillis' field.
   * @param value the value to set.
   */
  public void setReferenceStartMillis(java.lang.Long value) {
    this.referenceStartMillis = value;
  }

  /**
   * Gets the value of the 'referenceStopMillis' field.
   */
  public java.lang.Long getReferenceStopMillis() {
    return referenceStopMillis;
  }

  /**
   * Sets the value of the 'referenceStopMillis' field.
   * @param value the value to set.
   */
  public void setReferenceStopMillis(java.lang.Long value) {
    this.referenceStopMillis = value;
  }

  /**
   * Gets the value of the 'billingYear' field.
   */
  public java.lang.Integer getBillingYear() {
    return billingYear;
  }

  /**
   * Sets the value of the 'billingYear' field.
   * @param value the value to set.
   */
  public void setBillingYear(java.lang.Integer value) {
    this.billingYear = value;
  }

  /**
   * Gets the value of the 'billingMonth' field.
   */
  public java.lang.Integer getBillingMonth() {
    return billingMonth;
  }

  /**
   * Sets the value of the 'billingMonth' field.
   * @param value the value to set.
   */
  public void setBillingMonth(java.lang.Integer value) {
    this.billingMonth = value;
  }

  /**
   * Gets the value of the 'billingMonthDay' field.
   */
  public java.lang.Integer getBillingMonthDay() {
    return billingMonthDay;
  }

  /**
   * Sets the value of the 'billingMonthDay' field.
   * @param value the value to set.
   */
  public void setBillingMonthDay(java.lang.Integer value) {
    this.billingMonthDay = value;
  }

  /**
   * Gets the value of the 'chargeslots' field.
   */
  public java.util.List<gr.grnet.aquarium.message.avro.gen.ChargeslotMsg> getChargeslots() {
    return chargeslots;
  }

  /**
   * Sets the value of the 'chargeslots' field.
   * @param value the value to set.
   */
  public void setChargeslots(java.util.List<gr.grnet.aquarium.message.avro.gen.ChargeslotMsg> value) {
    this.chargeslots = value;
  }

  /**
   * Gets the value of the 'resourceEvents' field.
   */
  public java.util.List<gr.grnet.aquarium.message.avro.gen.ResourceEventMsg> getResourceEvents() {
    return resourceEvents;
  }

  /**
   * Sets the value of the 'resourceEvents' field.
   * @param value the value to set.
   */
  public void setResourceEvents(java.util.List<gr.grnet.aquarium.message.avro.gen.ResourceEventMsg> value) {
    this.resourceEvents = value;
  }

  /**
   * Gets the value of the 'resourceType' field.
   */
  public gr.grnet.aquarium.message.avro.gen.ResourceTypeMsg getResourceType() {
    return resourceType;
  }

  /**
   * Sets the value of the 'resourceType' field.
   * @param value the value to set.
   */
  public void setResourceType(gr.grnet.aquarium.message.avro.gen.ResourceTypeMsg value) {
    this.resourceType = value;
  }

  /**
   * Gets the value of the 'isSynthetic' field.
   */
  public java.lang.Boolean getIsSynthetic() {
    return isSynthetic;
  }

  /**
   * Sets the value of the 'isSynthetic' field.
   * @param value the value to set.
   */
  public void setIsSynthetic(java.lang.Boolean value) {
    this.isSynthetic = value;
  }

  /** Creates a new WalletEntryMsg RecordBuilder */
  public static gr.grnet.aquarium.message.avro.gen.WalletEntryMsg.Builder newBuilder() {
    return new gr.grnet.aquarium.message.avro.gen.WalletEntryMsg.Builder();
  }
  
  /** Creates a new WalletEntryMsg RecordBuilder by copying an existing Builder */
  public static gr.grnet.aquarium.message.avro.gen.WalletEntryMsg.Builder newBuilder(gr.grnet.aquarium.message.avro.gen.WalletEntryMsg.Builder other) {
    return new gr.grnet.aquarium.message.avro.gen.WalletEntryMsg.Builder(other);
  }
  
  /** Creates a new WalletEntryMsg RecordBuilder by copying an existing WalletEntryMsg instance */
  public static gr.grnet.aquarium.message.avro.gen.WalletEntryMsg.Builder newBuilder(gr.grnet.aquarium.message.avro.gen.WalletEntryMsg other) {
    return new gr.grnet.aquarium.message.avro.gen.WalletEntryMsg.Builder(other);
  }
  
  /**
   * RecordBuilder for WalletEntryMsg instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<WalletEntryMsg>
    implements org.apache.avro.data.RecordBuilder<WalletEntryMsg> {

    private java.lang.String userID;
    private double sumOfCreditsToSubtract;
    private double oldTotalCredits;
    private double newTotalCredits;
    private long whenComputedMillis;
    private long referenceStartMillis;
    private long referenceStopMillis;
    private int billingYear;
    private int billingMonth;
    private int billingMonthDay;
    private java.util.List<gr.grnet.aquarium.message.avro.gen.ChargeslotMsg> chargeslots;
    private java.util.List<gr.grnet.aquarium.message.avro.gen.ResourceEventMsg> resourceEvents;
    private gr.grnet.aquarium.message.avro.gen.ResourceTypeMsg resourceType;
    private boolean isSynthetic;

    /** Creates a new Builder */
    private Builder() {
      super(gr.grnet.aquarium.message.avro.gen.WalletEntryMsg.SCHEMA$);
    }
    
    /** Creates a Builder by copying an existing Builder */
    private Builder(gr.grnet.aquarium.message.avro.gen.WalletEntryMsg.Builder other) {
      super(other);
    }
    
    /** Creates a Builder by copying an existing WalletEntryMsg instance */
    private Builder(gr.grnet.aquarium.message.avro.gen.WalletEntryMsg other) {
            super(gr.grnet.aquarium.message.avro.gen.WalletEntryMsg.SCHEMA$);
      if (isValidValue(fields()[0], other.userID)) {
        this.userID = (java.lang.String) data().deepCopy(fields()[0].schema(), other.userID);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.sumOfCreditsToSubtract)) {
        this.sumOfCreditsToSubtract = (java.lang.Double) data().deepCopy(fields()[1].schema(), other.sumOfCreditsToSubtract);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.oldTotalCredits)) {
        this.oldTotalCredits = (java.lang.Double) data().deepCopy(fields()[2].schema(), other.oldTotalCredits);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.newTotalCredits)) {
        this.newTotalCredits = (java.lang.Double) data().deepCopy(fields()[3].schema(), other.newTotalCredits);
        fieldSetFlags()[3] = true;
      }
      if (isValidValue(fields()[4], other.whenComputedMillis)) {
        this.whenComputedMillis = (java.lang.Long) data().deepCopy(fields()[4].schema(), other.whenComputedMillis);
        fieldSetFlags()[4] = true;
      }
      if (isValidValue(fields()[5], other.referenceStartMillis)) {
        this.referenceStartMillis = (java.lang.Long) data().deepCopy(fields()[5].schema(), other.referenceStartMillis);
        fieldSetFlags()[5] = true;
      }
      if (isValidValue(fields()[6], other.referenceStopMillis)) {
        this.referenceStopMillis = (java.lang.Long) data().deepCopy(fields()[6].schema(), other.referenceStopMillis);
        fieldSetFlags()[6] = true;
      }
      if (isValidValue(fields()[7], other.billingYear)) {
        this.billingYear = (java.lang.Integer) data().deepCopy(fields()[7].schema(), other.billingYear);
        fieldSetFlags()[7] = true;
      }
      if (isValidValue(fields()[8], other.billingMonth)) {
        this.billingMonth = (java.lang.Integer) data().deepCopy(fields()[8].schema(), other.billingMonth);
        fieldSetFlags()[8] = true;
      }
      if (isValidValue(fields()[9], other.billingMonthDay)) {
        this.billingMonthDay = (java.lang.Integer) data().deepCopy(fields()[9].schema(), other.billingMonthDay);
        fieldSetFlags()[9] = true;
      }
      if (isValidValue(fields()[10], other.chargeslots)) {
        this.chargeslots = (java.util.List<gr.grnet.aquarium.message.avro.gen.ChargeslotMsg>) data().deepCopy(fields()[10].schema(), other.chargeslots);
        fieldSetFlags()[10] = true;
      }
      if (isValidValue(fields()[11], other.resourceEvents)) {
        this.resourceEvents = (java.util.List<gr.grnet.aquarium.message.avro.gen.ResourceEventMsg>) data().deepCopy(fields()[11].schema(), other.resourceEvents);
        fieldSetFlags()[11] = true;
      }
      if (isValidValue(fields()[12], other.resourceType)) {
        this.resourceType = (gr.grnet.aquarium.message.avro.gen.ResourceTypeMsg) data().deepCopy(fields()[12].schema(), other.resourceType);
        fieldSetFlags()[12] = true;
      }
      if (isValidValue(fields()[13], other.isSynthetic)) {
        this.isSynthetic = (java.lang.Boolean) data().deepCopy(fields()[13].schema(), other.isSynthetic);
        fieldSetFlags()[13] = true;
      }
    }

    /** Gets the value of the 'userID' field */
    public java.lang.String getUserID() {
      return userID;
    }
    
    /** Sets the value of the 'userID' field */
    public gr.grnet.aquarium.message.avro.gen.WalletEntryMsg.Builder setUserID(java.lang.String value) {
      validate(fields()[0], value);
      this.userID = value;
      fieldSetFlags()[0] = true;
      return this; 
    }
    
    /** Checks whether the 'userID' field has been set */
    public boolean hasUserID() {
      return fieldSetFlags()[0];
    }
    
    /** Clears the value of the 'userID' field */
    public gr.grnet.aquarium.message.avro.gen.WalletEntryMsg.Builder clearUserID() {
      userID = null;
      fieldSetFlags()[0] = false;
      return this;
    }

    /** Gets the value of the 'sumOfCreditsToSubtract' field */
    public java.lang.Double getSumOfCreditsToSubtract() {
      return sumOfCreditsToSubtract;
    }
    
    /** Sets the value of the 'sumOfCreditsToSubtract' field */
    public gr.grnet.aquarium.message.avro.gen.WalletEntryMsg.Builder setSumOfCreditsToSubtract(double value) {
      validate(fields()[1], value);
      this.sumOfCreditsToSubtract = value;
      fieldSetFlags()[1] = true;
      return this; 
    }
    
    /** Checks whether the 'sumOfCreditsToSubtract' field has been set */
    public boolean hasSumOfCreditsToSubtract() {
      return fieldSetFlags()[1];
    }
    
    /** Clears the value of the 'sumOfCreditsToSubtract' field */
    public gr.grnet.aquarium.message.avro.gen.WalletEntryMsg.Builder clearSumOfCreditsToSubtract() {
      fieldSetFlags()[1] = false;
      return this;
    }

    /** Gets the value of the 'oldTotalCredits' field */
    public java.lang.Double getOldTotalCredits() {
      return oldTotalCredits;
    }
    
    /** Sets the value of the 'oldTotalCredits' field */
    public gr.grnet.aquarium.message.avro.gen.WalletEntryMsg.Builder setOldTotalCredits(double value) {
      validate(fields()[2], value);
      this.oldTotalCredits = value;
      fieldSetFlags()[2] = true;
      return this; 
    }
    
    /** Checks whether the 'oldTotalCredits' field has been set */
    public boolean hasOldTotalCredits() {
      return fieldSetFlags()[2];
    }
    
    /** Clears the value of the 'oldTotalCredits' field */
    public gr.grnet.aquarium.message.avro.gen.WalletEntryMsg.Builder clearOldTotalCredits() {
      fieldSetFlags()[2] = false;
      return this;
    }

    /** Gets the value of the 'newTotalCredits' field */
    public java.lang.Double getNewTotalCredits() {
      return newTotalCredits;
    }
    
    /** Sets the value of the 'newTotalCredits' field */
    public gr.grnet.aquarium.message.avro.gen.WalletEntryMsg.Builder setNewTotalCredits(double value) {
      validate(fields()[3], value);
      this.newTotalCredits = value;
      fieldSetFlags()[3] = true;
      return this; 
    }
    
    /** Checks whether the 'newTotalCredits' field has been set */
    public boolean hasNewTotalCredits() {
      return fieldSetFlags()[3];
    }
    
    /** Clears the value of the 'newTotalCredits' field */
    public gr.grnet.aquarium.message.avro.gen.WalletEntryMsg.Builder clearNewTotalCredits() {
      fieldSetFlags()[3] = false;
      return this;
    }

    /** Gets the value of the 'whenComputedMillis' field */
    public java.lang.Long getWhenComputedMillis() {
      return whenComputedMillis;
    }
    
    /** Sets the value of the 'whenComputedMillis' field */
    public gr.grnet.aquarium.message.avro.gen.WalletEntryMsg.Builder setWhenComputedMillis(long value) {
      validate(fields()[4], value);
      this.whenComputedMillis = value;
      fieldSetFlags()[4] = true;
      return this; 
    }
    
    /** Checks whether the 'whenComputedMillis' field has been set */
    public boolean hasWhenComputedMillis() {
      return fieldSetFlags()[4];
    }
    
    /** Clears the value of the 'whenComputedMillis' field */
    public gr.grnet.aquarium.message.avro.gen.WalletEntryMsg.Builder clearWhenComputedMillis() {
      fieldSetFlags()[4] = false;
      return this;
    }

    /** Gets the value of the 'referenceStartMillis' field */
    public java.lang.Long getReferenceStartMillis() {
      return referenceStartMillis;
    }
    
    /** Sets the value of the 'referenceStartMillis' field */
    public gr.grnet.aquarium.message.avro.gen.WalletEntryMsg.Builder setReferenceStartMillis(long value) {
      validate(fields()[5], value);
      this.referenceStartMillis = value;
      fieldSetFlags()[5] = true;
      return this; 
    }
    
    /** Checks whether the 'referenceStartMillis' field has been set */
    public boolean hasReferenceStartMillis() {
      return fieldSetFlags()[5];
    }
    
    /** Clears the value of the 'referenceStartMillis' field */
    public gr.grnet.aquarium.message.avro.gen.WalletEntryMsg.Builder clearReferenceStartMillis() {
      fieldSetFlags()[5] = false;
      return this;
    }

    /** Gets the value of the 'referenceStopMillis' field */
    public java.lang.Long getReferenceStopMillis() {
      return referenceStopMillis;
    }
    
    /** Sets the value of the 'referenceStopMillis' field */
    public gr.grnet.aquarium.message.avro.gen.WalletEntryMsg.Builder setReferenceStopMillis(long value) {
      validate(fields()[6], value);
      this.referenceStopMillis = value;
      fieldSetFlags()[6] = true;
      return this; 
    }
    
    /** Checks whether the 'referenceStopMillis' field has been set */
    public boolean hasReferenceStopMillis() {
      return fieldSetFlags()[6];
    }
    
    /** Clears the value of the 'referenceStopMillis' field */
    public gr.grnet.aquarium.message.avro.gen.WalletEntryMsg.Builder clearReferenceStopMillis() {
      fieldSetFlags()[6] = false;
      return this;
    }

    /** Gets the value of the 'billingYear' field */
    public java.lang.Integer getBillingYear() {
      return billingYear;
    }
    
    /** Sets the value of the 'billingYear' field */
    public gr.grnet.aquarium.message.avro.gen.WalletEntryMsg.Builder setBillingYear(int value) {
      validate(fields()[7], value);
      this.billingYear = value;
      fieldSetFlags()[7] = true;
      return this; 
    }
    
    /** Checks whether the 'billingYear' field has been set */
    public boolean hasBillingYear() {
      return fieldSetFlags()[7];
    }
    
    /** Clears the value of the 'billingYear' field */
    public gr.grnet.aquarium.message.avro.gen.WalletEntryMsg.Builder clearBillingYear() {
      fieldSetFlags()[7] = false;
      return this;
    }

    /** Gets the value of the 'billingMonth' field */
    public java.lang.Integer getBillingMonth() {
      return billingMonth;
    }
    
    /** Sets the value of the 'billingMonth' field */
    public gr.grnet.aquarium.message.avro.gen.WalletEntryMsg.Builder setBillingMonth(int value) {
      validate(fields()[8], value);
      this.billingMonth = value;
      fieldSetFlags()[8] = true;
      return this; 
    }
    
    /** Checks whether the 'billingMonth' field has been set */
    public boolean hasBillingMonth() {
      return fieldSetFlags()[8];
    }
    
    /** Clears the value of the 'billingMonth' field */
    public gr.grnet.aquarium.message.avro.gen.WalletEntryMsg.Builder clearBillingMonth() {
      fieldSetFlags()[8] = false;
      return this;
    }

    /** Gets the value of the 'billingMonthDay' field */
    public java.lang.Integer getBillingMonthDay() {
      return billingMonthDay;
    }
    
    /** Sets the value of the 'billingMonthDay' field */
    public gr.grnet.aquarium.message.avro.gen.WalletEntryMsg.Builder setBillingMonthDay(int value) {
      validate(fields()[9], value);
      this.billingMonthDay = value;
      fieldSetFlags()[9] = true;
      return this; 
    }
    
    /** Checks whether the 'billingMonthDay' field has been set */
    public boolean hasBillingMonthDay() {
      return fieldSetFlags()[9];
    }
    
    /** Clears the value of the 'billingMonthDay' field */
    public gr.grnet.aquarium.message.avro.gen.WalletEntryMsg.Builder clearBillingMonthDay() {
      fieldSetFlags()[9] = false;
      return this;
    }

    /** Gets the value of the 'chargeslots' field */
    public java.util.List<gr.grnet.aquarium.message.avro.gen.ChargeslotMsg> getChargeslots() {
      return chargeslots;
    }
    
    /** Sets the value of the 'chargeslots' field */
    public gr.grnet.aquarium.message.avro.gen.WalletEntryMsg.Builder setChargeslots(java.util.List<gr.grnet.aquarium.message.avro.gen.ChargeslotMsg> value) {
      validate(fields()[10], value);
      this.chargeslots = value;
      fieldSetFlags()[10] = true;
      return this; 
    }
    
    /** Checks whether the 'chargeslots' field has been set */
    public boolean hasChargeslots() {
      return fieldSetFlags()[10];
    }
    
    /** Clears the value of the 'chargeslots' field */
    public gr.grnet.aquarium.message.avro.gen.WalletEntryMsg.Builder clearChargeslots() {
      chargeslots = null;
      fieldSetFlags()[10] = false;
      return this;
    }

    /** Gets the value of the 'resourceEvents' field */
    public java.util.List<gr.grnet.aquarium.message.avro.gen.ResourceEventMsg> getResourceEvents() {
      return resourceEvents;
    }
    
    /** Sets the value of the 'resourceEvents' field */
    public gr.grnet.aquarium.message.avro.gen.WalletEntryMsg.Builder setResourceEvents(java.util.List<gr.grnet.aquarium.message.avro.gen.ResourceEventMsg> value) {
      validate(fields()[11], value);
      this.resourceEvents = value;
      fieldSetFlags()[11] = true;
      return this; 
    }
    
    /** Checks whether the 'resourceEvents' field has been set */
    public boolean hasResourceEvents() {
      return fieldSetFlags()[11];
    }
    
    /** Clears the value of the 'resourceEvents' field */
    public gr.grnet.aquarium.message.avro.gen.WalletEntryMsg.Builder clearResourceEvents() {
      resourceEvents = null;
      fieldSetFlags()[11] = false;
      return this;
    }

    /** Gets the value of the 'resourceType' field */
    public gr.grnet.aquarium.message.avro.gen.ResourceTypeMsg getResourceType() {
      return resourceType;
    }
    
    /** Sets the value of the 'resourceType' field */
    public gr.grnet.aquarium.message.avro.gen.WalletEntryMsg.Builder setResourceType(gr.grnet.aquarium.message.avro.gen.ResourceTypeMsg value) {
      validate(fields()[12], value);
      this.resourceType = value;
      fieldSetFlags()[12] = true;
      return this; 
    }
    
    /** Checks whether the 'resourceType' field has been set */
    public boolean hasResourceType() {
      return fieldSetFlags()[12];
    }
    
    /** Clears the value of the 'resourceType' field */
    public gr.grnet.aquarium.message.avro.gen.WalletEntryMsg.Builder clearResourceType() {
      resourceType = null;
      fieldSetFlags()[12] = false;
      return this;
    }

    /** Gets the value of the 'isSynthetic' field */
    public java.lang.Boolean getIsSynthetic() {
      return isSynthetic;
    }
    
    /** Sets the value of the 'isSynthetic' field */
    public gr.grnet.aquarium.message.avro.gen.WalletEntryMsg.Builder setIsSynthetic(boolean value) {
      validate(fields()[13], value);
      this.isSynthetic = value;
      fieldSetFlags()[13] = true;
      return this; 
    }
    
    /** Checks whether the 'isSynthetic' field has been set */
    public boolean hasIsSynthetic() {
      return fieldSetFlags()[13];
    }
    
    /** Clears the value of the 'isSynthetic' field */
    public gr.grnet.aquarium.message.avro.gen.WalletEntryMsg.Builder clearIsSynthetic() {
      fieldSetFlags()[13] = false;
      return this;
    }

    @Override
    public WalletEntryMsg build() {
      try {
        WalletEntryMsg record = new WalletEntryMsg();
        record.userID = fieldSetFlags()[0] ? this.userID : (java.lang.String) defaultValue(fields()[0]);
        record.sumOfCreditsToSubtract = fieldSetFlags()[1] ? this.sumOfCreditsToSubtract : (java.lang.Double) defaultValue(fields()[1]);
        record.oldTotalCredits = fieldSetFlags()[2] ? this.oldTotalCredits : (java.lang.Double) defaultValue(fields()[2]);
        record.newTotalCredits = fieldSetFlags()[3] ? this.newTotalCredits : (java.lang.Double) defaultValue(fields()[3]);
        record.whenComputedMillis = fieldSetFlags()[4] ? this.whenComputedMillis : (java.lang.Long) defaultValue(fields()[4]);
        record.referenceStartMillis = fieldSetFlags()[5] ? this.referenceStartMillis : (java.lang.Long) defaultValue(fields()[5]);
        record.referenceStopMillis = fieldSetFlags()[6] ? this.referenceStopMillis : (java.lang.Long) defaultValue(fields()[6]);
        record.billingYear = fieldSetFlags()[7] ? this.billingYear : (java.lang.Integer) defaultValue(fields()[7]);
        record.billingMonth = fieldSetFlags()[8] ? this.billingMonth : (java.lang.Integer) defaultValue(fields()[8]);
        record.billingMonthDay = fieldSetFlags()[9] ? this.billingMonthDay : (java.lang.Integer) defaultValue(fields()[9]);
        record.chargeslots = fieldSetFlags()[10] ? this.chargeslots : (java.util.List<gr.grnet.aquarium.message.avro.gen.ChargeslotMsg>) defaultValue(fields()[10]);
        record.resourceEvents = fieldSetFlags()[11] ? this.resourceEvents : (java.util.List<gr.grnet.aquarium.message.avro.gen.ResourceEventMsg>) defaultValue(fields()[11]);
        record.resourceType = fieldSetFlags()[12] ? this.resourceType : (gr.grnet.aquarium.message.avro.gen.ResourceTypeMsg) defaultValue(fields()[12]);
        record.isSynthetic = fieldSetFlags()[13] ? this.isSynthetic : (java.lang.Boolean) defaultValue(fields()[13]);
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }
}