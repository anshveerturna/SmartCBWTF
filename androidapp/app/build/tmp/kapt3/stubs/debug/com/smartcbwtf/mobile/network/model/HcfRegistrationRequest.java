package com.smartcbwtf.mobile.network.model;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\b\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\b\n\u0002\b\u0003\n\u0002\u0010\u0006\n\u0002\b\u0005\n\u0002\u0010\u0007\n\u0002\b@\b\u0086\b\u0018\u00002\u00020\u0001B\u00e1\u0001\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\b\u0010\u0004\u001a\u0004\u0018\u00010\u0003\u0012\b\u0010\u0005\u001a\u0004\u0018\u00010\u0003\u0012\b\u0010\u0006\u001a\u0004\u0018\u00010\u0003\u0012\b\u0010\u0007\u001a\u0004\u0018\u00010\u0003\u0012\b\u0010\b\u001a\u0004\u0018\u00010\u0003\u0012\b\u0010\t\u001a\u0004\u0018\u00010\u0003\u0012\b\u0010\n\u001a\u0004\u0018\u00010\u0003\u0012\b\b\u0002\u0010\u000b\u001a\u00020\f\u0012\b\u0010\r\u001a\u0004\u0018\u00010\u000e\u0012\b\u0010\u000f\u001a\u0004\u0018\u00010\u0003\u0012\b\u0010\u0010\u001a\u0004\u0018\u00010\u0003\u0012\b\u0010\u0011\u001a\u0004\u0018\u00010\u0012\u0012\b\b\u0002\u0010\u0013\u001a\u00020\f\u0012\b\u0010\u0014\u001a\u0004\u0018\u00010\u0003\u0012\u0006\u0010\u0015\u001a\u00020\u0012\u0012\u0006\u0010\u0016\u001a\u00020\u0012\u0012\u0006\u0010\u0017\u001a\u00020\u0018\u0012\n\b\u0002\u0010\u0019\u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010\u001a\u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010\u001b\u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010\u001c\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\u0002\u0010\u001dJ\t\u0010<\u001a\u00020\u0003H\u00c6\u0003J\u0010\u0010=\u001a\u0004\u0018\u00010\u000eH\u00c6\u0003\u00a2\u0006\u0002\u00103J\u000b\u0010>\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010?\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u0010\u0010@\u001a\u0004\u0018\u00010\u0012H\u00c6\u0003\u00a2\u0006\u0002\u0010/J\t\u0010A\u001a\u00020\fH\u00c6\u0003J\u000b\u0010B\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\t\u0010C\u001a\u00020\u0012H\u00c6\u0003J\t\u0010D\u001a\u00020\u0012H\u00c6\u0003J\t\u0010E\u001a\u00020\u0018H\u00c6\u0003J\u000b\u0010F\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010G\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010H\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010I\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010J\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010K\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010L\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010M\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010N\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010O\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010P\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\t\u0010Q\u001a\u00020\fH\u00c6\u0003J\u008a\u0002\u0010R\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\n\b\u0002\u0010\u0004\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u0005\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u0006\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u0007\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\b\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\t\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\n\u001a\u0004\u0018\u00010\u00032\b\b\u0002\u0010\u000b\u001a\u00020\f2\n\b\u0002\u0010\r\u001a\u0004\u0018\u00010\u000e2\n\b\u0002\u0010\u000f\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u0010\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u0011\u001a\u0004\u0018\u00010\u00122\b\b\u0002\u0010\u0013\u001a\u00020\f2\n\b\u0002\u0010\u0014\u001a\u0004\u0018\u00010\u00032\b\b\u0002\u0010\u0015\u001a\u00020\u00122\b\b\u0002\u0010\u0016\u001a\u00020\u00122\b\b\u0002\u0010\u0017\u001a\u00020\u00182\n\b\u0002\u0010\u0019\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u001a\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u001b\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u001c\u001a\u0004\u0018\u00010\u0003H\u00c6\u0001\u00a2\u0006\u0002\u0010SJ\u0013\u0010T\u001a\u00020\f2\b\u0010U\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010V\u001a\u00020\u000eH\u00d6\u0001J\t\u0010W\u001a\u00020\u0003H\u00d6\u0001R\u0018\u0010\n\u001a\u0004\u0018\u00010\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001e\u0010\u001fR\u0013\u0010\u0004\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b \u0010\u001fR\u0018\u0010\u001a\u001a\u0004\u0018\u00010\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b!\u0010\u001fR\u0018\u0010\u0019\u001a\u0004\u0018\u00010\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\"\u0010\u001fR\u0011\u0010\u000b\u001a\u00020\f\u00a2\u0006\b\n\u0000\u001a\u0004\b#\u0010$R\u0018\u0010\u0007\u001a\u0004\u0018\u00010\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b%\u0010\u001fR\u0018\u0010\u0006\u001a\u0004\u0018\u00010\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b&\u0010\u001fR\u0018\u0010\u001b\u001a\u0004\u0018\u00010\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\'\u0010\u001fR\u0016\u0010\u0017\u001a\u00020\u00188\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b(\u0010)R\u0016\u0010\u0015\u001a\u00020\u00128\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b*\u0010+R\u0016\u0010\u0016\u001a\u00020\u00128\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b,\u0010+R\u0018\u0010\t\u001a\u0004\u0018\u00010\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b-\u0010\u001fR\u001a\u0010\u0011\u001a\u0004\u0018\u00010\u00128\u0006X\u0087\u0004\u00a2\u0006\n\n\u0002\u00100\u001a\u0004\b.\u0010/R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b1\u0010\u001fR\u001a\u0010\r\u001a\u0004\u0018\u00010\u000e8\u0006X\u0087\u0004\u00a2\u0006\n\n\u0002\u00104\u001a\u0004\b2\u00103R\u0018\u0010\u0010\u001a\u0004\u0018\u00010\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b5\u0010\u001fR\u0018\u0010\b\u001a\u0004\u0018\u00010\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b6\u0010\u001fR\u0018\u0010\u000f\u001a\u0004\u0018\u00010\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b7\u0010\u001fR\u0018\u0010\u0005\u001a\u0004\u0018\u00010\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b8\u0010\u001fR\u0018\u0010\u001c\u001a\u0004\u0018\u00010\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b9\u0010\u001fR\u0016\u0010\u0013\u001a\u00020\f8\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b:\u0010$R\u0018\u0010\u0014\u001a\u0004\u0018\u00010\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b;\u0010\u001f\u00a8\u0006X"}, d2 = {"Lcom/smartcbwtf/mobile/network/model/HcfRegistrationRequest;", "", "name", "", "address", "phone", "email", "doctorName", "panNo", "gstNo", "aadharNo", "bedded", "", "numberOfBeds", "", "pcbAuthorizationNo", "otherNotes", "monthlyCharges", "", "termsAccepted", "termsVersion", "gpsLatitude", "gpsLongitude", "gpsAccuracy", "", "agreementStartDate", "agreementEndDate", "facilityId", "registeredByUserId", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZLjava/lang/Integer;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Double;ZLjava/lang/String;DDFLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", "getAadharNo", "()Ljava/lang/String;", "getAddress", "getAgreementEndDate", "getAgreementStartDate", "getBedded", "()Z", "getDoctorName", "getEmail", "getFacilityId", "getGpsAccuracy", "()F", "getGpsLatitude", "()D", "getGpsLongitude", "getGstNo", "getMonthlyCharges", "()Ljava/lang/Double;", "Ljava/lang/Double;", "getName", "getNumberOfBeds", "()Ljava/lang/Integer;", "Ljava/lang/Integer;", "getOtherNotes", "getPanNo", "getPcbAuthorizationNo", "getPhone", "getRegisteredByUserId", "getTermsAccepted", "getTermsVersion", "component1", "component10", "component11", "component12", "component13", "component14", "component15", "component16", "component17", "component18", "component19", "component2", "component20", "component21", "component22", "component3", "component4", "component5", "component6", "component7", "component8", "component9", "copy", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZLjava/lang/Integer;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Double;ZLjava/lang/String;DDFLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Lcom/smartcbwtf/mobile/network/model/HcfRegistrationRequest;", "equals", "other", "hashCode", "toString", "app_debug"})
public final class HcfRegistrationRequest {
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String name = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String address = null;
    @com.google.gson.annotations.SerializedName(value = "contactPhone")
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String phone = null;
    @com.google.gson.annotations.SerializedName(value = "contactEmail")
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String email = null;
    @com.google.gson.annotations.SerializedName(value = "doctorName")
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String doctorName = null;
    @com.google.gson.annotations.SerializedName(value = "panNo")
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String panNo = null;
    @com.google.gson.annotations.SerializedName(value = "gstNo")
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String gstNo = null;
    @com.google.gson.annotations.SerializedName(value = "aadharNo")
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String aadharNo = null;
    private final boolean bedded = false;
    @com.google.gson.annotations.SerializedName(value = "numberOfBeds")
    @org.jetbrains.annotations.Nullable()
    private final java.lang.Integer numberOfBeds = null;
    @com.google.gson.annotations.SerializedName(value = "pcbAuthorizationNo")
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String pcbAuthorizationNo = null;
    @com.google.gson.annotations.SerializedName(value = "otherNotes")
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String otherNotes = null;
    @com.google.gson.annotations.SerializedName(value = "monthlyCharges")
    @org.jetbrains.annotations.Nullable()
    private final java.lang.Double monthlyCharges = null;
    @com.google.gson.annotations.SerializedName(value = "termsAccepted")
    private final boolean termsAccepted = false;
    @com.google.gson.annotations.SerializedName(value = "termsVersion")
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String termsVersion = null;
    @com.google.gson.annotations.SerializedName(value = "registrationGpsLat")
    private final double gpsLatitude = 0.0;
    @com.google.gson.annotations.SerializedName(value = "registrationGpsLon")
    private final double gpsLongitude = 0.0;
    @com.google.gson.annotations.SerializedName(value = "registrationGpsAccuracy")
    private final float gpsAccuracy = 0.0F;
    @com.google.gson.annotations.SerializedName(value = "agreementStartDate")
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String agreementStartDate = null;
    @com.google.gson.annotations.SerializedName(value = "agreementEndDate")
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String agreementEndDate = null;
    @com.google.gson.annotations.SerializedName(value = "facilityId")
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String facilityId = null;
    @com.google.gson.annotations.SerializedName(value = "registeredByUserId")
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String registeredByUserId = null;
    
    public HcfRegistrationRequest(@org.jetbrains.annotations.NotNull()
    java.lang.String name, @org.jetbrains.annotations.Nullable()
    java.lang.String address, @org.jetbrains.annotations.Nullable()
    java.lang.String phone, @org.jetbrains.annotations.Nullable()
    java.lang.String email, @org.jetbrains.annotations.Nullable()
    java.lang.String doctorName, @org.jetbrains.annotations.Nullable()
    java.lang.String panNo, @org.jetbrains.annotations.Nullable()
    java.lang.String gstNo, @org.jetbrains.annotations.Nullable()
    java.lang.String aadharNo, boolean bedded, @org.jetbrains.annotations.Nullable()
    java.lang.Integer numberOfBeds, @org.jetbrains.annotations.Nullable()
    java.lang.String pcbAuthorizationNo, @org.jetbrains.annotations.Nullable()
    java.lang.String otherNotes, @org.jetbrains.annotations.Nullable()
    java.lang.Double monthlyCharges, boolean termsAccepted, @org.jetbrains.annotations.Nullable()
    java.lang.String termsVersion, double gpsLatitude, double gpsLongitude, float gpsAccuracy, @org.jetbrains.annotations.Nullable()
    java.lang.String agreementStartDate, @org.jetbrains.annotations.Nullable()
    java.lang.String agreementEndDate, @org.jetbrains.annotations.Nullable()
    java.lang.String facilityId, @org.jetbrains.annotations.Nullable()
    java.lang.String registeredByUserId) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getName() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getAddress() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getPhone() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getEmail() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getDoctorName() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getPanNo() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getGstNo() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getAadharNo() {
        return null;
    }
    
    public final boolean getBedded() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Integer getNumberOfBeds() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getPcbAuthorizationNo() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getOtherNotes() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Double getMonthlyCharges() {
        return null;
    }
    
    public final boolean getTermsAccepted() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getTermsVersion() {
        return null;
    }
    
    public final double getGpsLatitude() {
        return 0.0;
    }
    
    public final double getGpsLongitude() {
        return 0.0;
    }
    
    public final float getGpsAccuracy() {
        return 0.0F;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getAgreementStartDate() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getAgreementEndDate() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getFacilityId() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getRegisteredByUserId() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component1() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Integer component10() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component11() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component12() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Double component13() {
        return null;
    }
    
    public final boolean component14() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component15() {
        return null;
    }
    
    public final double component16() {
        return 0.0;
    }
    
    public final double component17() {
        return 0.0;
    }
    
    public final float component18() {
        return 0.0F;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component19() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component2() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component20() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component21() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component22() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component3() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component4() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component5() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component6() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component7() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component8() {
        return null;
    }
    
    public final boolean component9() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.smartcbwtf.mobile.network.model.HcfRegistrationRequest copy(@org.jetbrains.annotations.NotNull()
    java.lang.String name, @org.jetbrains.annotations.Nullable()
    java.lang.String address, @org.jetbrains.annotations.Nullable()
    java.lang.String phone, @org.jetbrains.annotations.Nullable()
    java.lang.String email, @org.jetbrains.annotations.Nullable()
    java.lang.String doctorName, @org.jetbrains.annotations.Nullable()
    java.lang.String panNo, @org.jetbrains.annotations.Nullable()
    java.lang.String gstNo, @org.jetbrains.annotations.Nullable()
    java.lang.String aadharNo, boolean bedded, @org.jetbrains.annotations.Nullable()
    java.lang.Integer numberOfBeds, @org.jetbrains.annotations.Nullable()
    java.lang.String pcbAuthorizationNo, @org.jetbrains.annotations.Nullable()
    java.lang.String otherNotes, @org.jetbrains.annotations.Nullable()
    java.lang.Double monthlyCharges, boolean termsAccepted, @org.jetbrains.annotations.Nullable()
    java.lang.String termsVersion, double gpsLatitude, double gpsLongitude, float gpsAccuracy, @org.jetbrains.annotations.Nullable()
    java.lang.String agreementStartDate, @org.jetbrains.annotations.Nullable()
    java.lang.String agreementEndDate, @org.jetbrains.annotations.Nullable()
    java.lang.String facilityId, @org.jetbrains.annotations.Nullable()
    java.lang.String registeredByUserId) {
        return null;
    }
    
    @java.lang.Override()
    public boolean equals(@org.jetbrains.annotations.Nullable()
    java.lang.Object other) {
        return false;
    }
    
    @java.lang.Override()
    public int hashCode() {
        return 0;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public java.lang.String toString() {
        return null;
    }
}