package com.smartcbwtf.mobile.network.model;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\n\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0010\u0006\n\u0002\b\u0007\n\u0002\u0010\u0007\n\u0002\bO\b\u0086\b\u0018\u00002\u00020\u0001B\u00a1\u0002\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\b\u0010\u0004\u001a\u0004\u0018\u00010\u0003\u0012\b\u0010\u0005\u001a\u0004\u0018\u00010\u0003\u0012\b\u0010\u0006\u001a\u0004\u0018\u00010\u0003\u0012\b\u0010\u0007\u001a\u0004\u0018\u00010\u0003\u0012\b\u0010\b\u001a\u0004\u0018\u00010\u0003\u0012\b\u0010\t\u001a\u0004\u0018\u00010\u0003\u0012\b\u0010\n\u001a\u0004\u0018\u00010\u0003\u0012\b\u0010\u000b\u001a\u0004\u0018\u00010\u0003\u0012\b\u0010\f\u001a\u0004\u0018\u00010\u0003\u0012\b\b\u0002\u0010\r\u001a\u00020\u000e\u0012\b\u0010\u000f\u001a\u0004\u0018\u00010\u0010\u0012\b\u0010\u0011\u001a\u0004\u0018\u00010\u0003\u0012\b\u0010\u0012\u001a\u0004\u0018\u00010\u0013\u0012\u0006\u0010\u0014\u001a\u00020\u0003\u0012\b\u0010\u0015\u001a\u0004\u0018\u00010\u0003\u0012\b\b\u0002\u0010\u0016\u001a\u00020\u000e\u0012\b\u0010\u0017\u001a\u0004\u0018\u00010\u0003\u0012\u0006\u0010\u0018\u001a\u00020\u0013\u0012\u0006\u0010\u0019\u001a\u00020\u0013\u0012\u0006\u0010\u001a\u001a\u00020\u001b\u0012\n\b\u0002\u0010\u001c\u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010\u001d\u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010\u001e\u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010\u001f\u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010 \u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010!\u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010\"\u001a\u0004\u0018\u00010\u0010\u00a2\u0006\u0002\u0010#J\t\u0010H\u001a\u00020\u0003H\u00c6\u0003J\u000b\u0010I\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\t\u0010J\u001a\u00020\u000eH\u00c6\u0003J\u0010\u0010K\u001a\u0004\u0018\u00010\u0010H\u00c6\u0003\u00a2\u0006\u0002\u0010;J\u000b\u0010L\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u0010\u0010M\u001a\u0004\u0018\u00010\u0013H\u00c6\u0003\u00a2\u0006\u0002\u00107J\t\u0010N\u001a\u00020\u0003H\u00c6\u0003J\u000b\u0010O\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\t\u0010P\u001a\u00020\u000eH\u00c6\u0003J\u000b\u0010Q\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\t\u0010R\u001a\u00020\u0013H\u00c6\u0003J\u000b\u0010S\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\t\u0010T\u001a\u00020\u0013H\u00c6\u0003J\t\u0010U\u001a\u00020\u001bH\u00c6\u0003J\u000b\u0010V\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010W\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010X\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010Y\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010Z\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010[\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u0010\u0010\\\u001a\u0004\u0018\u00010\u0010H\u00c6\u0003\u00a2\u0006\u0002\u0010;J\u000b\u0010]\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010^\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010_\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010`\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010a\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010b\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010c\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u00d0\u0002\u0010d\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\n\b\u0002\u0010\u0004\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u0005\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u0006\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u0007\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\b\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\t\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\n\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u000b\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\f\u001a\u0004\u0018\u00010\u00032\b\b\u0002\u0010\r\u001a\u00020\u000e2\n\b\u0002\u0010\u000f\u001a\u0004\u0018\u00010\u00102\n\b\u0002\u0010\u0011\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u0012\u001a\u0004\u0018\u00010\u00132\b\b\u0002\u0010\u0014\u001a\u00020\u00032\n\b\u0002\u0010\u0015\u001a\u0004\u0018\u00010\u00032\b\b\u0002\u0010\u0016\u001a\u00020\u000e2\n\b\u0002\u0010\u0017\u001a\u0004\u0018\u00010\u00032\b\b\u0002\u0010\u0018\u001a\u00020\u00132\b\b\u0002\u0010\u0019\u001a\u00020\u00132\b\b\u0002\u0010\u001a\u001a\u00020\u001b2\n\b\u0002\u0010\u001c\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u001d\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u001e\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u001f\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010 \u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010!\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\"\u001a\u0004\u0018\u00010\u0010H\u00c6\u0001\u00a2\u0006\u0002\u0010eJ\u0013\u0010f\u001a\u00020\u000e2\b\u0010g\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010h\u001a\u00020\u0010H\u00d6\u0001J\t\u0010i\u001a\u00020\u0003H\u00d6\u0001R\u0018\u0010\f\u001a\u0004\u0018\u00010\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b$\u0010%R\u0013\u0010\u0004\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b&\u0010%R\u0018\u0010\u001d\u001a\u0004\u0018\u00010\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\'\u0010%R\u0018\u0010\u001c\u001a\u0004\u0018\u00010\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b(\u0010%R\u0011\u0010\r\u001a\u00020\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b)\u0010*R\u0018\u0010!\u001a\u0004\u0018\u00010\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b+\u0010%R\u0018\u0010\t\u001a\u0004\u0018\u00010\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b,\u0010%R\u0018\u0010\b\u001a\u0004\u0018\u00010\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b-\u0010%R\u0018\u0010\u001e\u001a\u0004\u0018\u00010\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b.\u0010%R\u0016\u0010\u001a\u001a\u00020\u001b8\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b/\u00100R\u0016\u0010\u0018\u001a\u00020\u00138\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b1\u00102R\u0016\u0010\u0019\u001a\u00020\u00138\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b3\u00102R\u0018\u0010\u000b\u001a\u0004\u0018\u00010\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b4\u0010%R\u0018\u0010 \u001a\u0004\u0018\u00010\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b5\u0010%R\u001a\u0010\u0012\u001a\u0004\u0018\u00010\u00138\u0006X\u0087\u0004\u00a2\u0006\n\n\u0002\u00108\u001a\u0004\b6\u00107R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b9\u0010%R\u001a\u0010\u000f\u001a\u0004\u0018\u00010\u00108\u0006X\u0087\u0004\u00a2\u0006\n\n\u0002\u0010<\u001a\u0004\b:\u0010;R\u0018\u0010\u0011\u001a\u0004\u0018\u00010\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b=\u0010%R\u0016\u0010\u0014\u001a\u00020\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b>\u0010%R\u0018\u0010\n\u001a\u0004\u0018\u00010\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b?\u0010%R\u0018\u0010\u0007\u001a\u0004\u0018\u00010\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b@\u0010%R\u0013\u0010\u0005\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\bA\u0010%R\u0018\u0010\u001f\u001a\u0004\u0018\u00010\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\bB\u0010%R\u0018\u0010\u0015\u001a\u0004\u0018\u00010\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\bC\u0010%R\u001a\u0010\"\u001a\u0004\u0018\u00010\u00108\u0006X\u0087\u0004\u00a2\u0006\n\n\u0002\u0010<\u001a\u0004\bD\u0010;R\u0013\u0010\u0006\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\bE\u0010%R\u0016\u0010\u0016\u001a\u00020\u000e8\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\bF\u0010*R\u0018\u0010\u0017\u001a\u0004\u0018\u00010\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\bG\u0010%\u00a8\u0006j"}, d2 = {"Lcom/smartcbwtf/mobile/network/model/HcfRegistrationRequest;", "", "name", "", "address", "pincode", "state", "phone", "email", "doctorName", "panNo", "gstNo", "aadharNo", "bedded", "", "numberOfBeds", "", "otherNotes", "monthlyCharges", "", "ownershipType", "rentAgreementUrl", "termsAccepted", "termsVersion", "gpsLatitude", "gpsLongitude", "gpsAccuracy", "", "agreementStartDate", "agreementEndDate", "facilityId", "registeredByUserId", "hcfType", "city", "seatCount", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZLjava/lang/Integer;Ljava/lang/String;Ljava/lang/Double;Ljava/lang/String;Ljava/lang/String;ZLjava/lang/String;DDFLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Integer;)V", "getAadharNo", "()Ljava/lang/String;", "getAddress", "getAgreementEndDate", "getAgreementStartDate", "getBedded", "()Z", "getCity", "getDoctorName", "getEmail", "getFacilityId", "getGpsAccuracy", "()F", "getGpsLatitude", "()D", "getGpsLongitude", "getGstNo", "getHcfType", "getMonthlyCharges", "()Ljava/lang/Double;", "Ljava/lang/Double;", "getName", "getNumberOfBeds", "()Ljava/lang/Integer;", "Ljava/lang/Integer;", "getOtherNotes", "getOwnershipType", "getPanNo", "getPhone", "getPincode", "getRegisteredByUserId", "getRentAgreementUrl", "getSeatCount", "getState", "getTermsAccepted", "getTermsVersion", "component1", "component10", "component11", "component12", "component13", "component14", "component15", "component16", "component17", "component18", "component19", "component2", "component20", "component21", "component22", "component23", "component24", "component25", "component26", "component27", "component28", "component3", "component4", "component5", "component6", "component7", "component8", "component9", "copy", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZLjava/lang/Integer;Ljava/lang/String;Ljava/lang/Double;Ljava/lang/String;Ljava/lang/String;ZLjava/lang/String;DDFLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Integer;)Lcom/smartcbwtf/mobile/network/model/HcfRegistrationRequest;", "equals", "other", "hashCode", "toString", "app_debug"})
public final class HcfRegistrationRequest {
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String name = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String address = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String pincode = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String state = null;
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
    @com.google.gson.annotations.SerializedName(value = "otherNotes")
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String otherNotes = null;
    @com.google.gson.annotations.SerializedName(value = "monthlyCharges")
    @org.jetbrains.annotations.Nullable()
    private final java.lang.Double monthlyCharges = null;
    @com.google.gson.annotations.SerializedName(value = "ownershipType")
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String ownershipType = null;
    @com.google.gson.annotations.SerializedName(value = "rentAgreementUrl")
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String rentAgreementUrl = null;
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
    @com.google.gson.annotations.SerializedName(value = "hcfType")
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String hcfType = null;
    @com.google.gson.annotations.SerializedName(value = "city")
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String city = null;
    @com.google.gson.annotations.SerializedName(value = "seatCount")
    @org.jetbrains.annotations.Nullable()
    private final java.lang.Integer seatCount = null;
    
    public HcfRegistrationRequest(@org.jetbrains.annotations.NotNull()
    java.lang.String name, @org.jetbrains.annotations.Nullable()
    java.lang.String address, @org.jetbrains.annotations.Nullable()
    java.lang.String pincode, @org.jetbrains.annotations.Nullable()
    java.lang.String state, @org.jetbrains.annotations.Nullable()
    java.lang.String phone, @org.jetbrains.annotations.Nullable()
    java.lang.String email, @org.jetbrains.annotations.Nullable()
    java.lang.String doctorName, @org.jetbrains.annotations.Nullable()
    java.lang.String panNo, @org.jetbrains.annotations.Nullable()
    java.lang.String gstNo, @org.jetbrains.annotations.Nullable()
    java.lang.String aadharNo, boolean bedded, @org.jetbrains.annotations.Nullable()
    java.lang.Integer numberOfBeds, @org.jetbrains.annotations.Nullable()
    java.lang.String otherNotes, @org.jetbrains.annotations.Nullable()
    java.lang.Double monthlyCharges, @org.jetbrains.annotations.NotNull()
    java.lang.String ownershipType, @org.jetbrains.annotations.Nullable()
    java.lang.String rentAgreementUrl, boolean termsAccepted, @org.jetbrains.annotations.Nullable()
    java.lang.String termsVersion, double gpsLatitude, double gpsLongitude, float gpsAccuracy, @org.jetbrains.annotations.Nullable()
    java.lang.String agreementStartDate, @org.jetbrains.annotations.Nullable()
    java.lang.String agreementEndDate, @org.jetbrains.annotations.Nullable()
    java.lang.String facilityId, @org.jetbrains.annotations.Nullable()
    java.lang.String registeredByUserId, @org.jetbrains.annotations.Nullable()
    java.lang.String hcfType, @org.jetbrains.annotations.Nullable()
    java.lang.String city, @org.jetbrains.annotations.Nullable()
    java.lang.Integer seatCount) {
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
    public final java.lang.String getPincode() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getState() {
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
    public final java.lang.String getOtherNotes() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Double getMonthlyCharges() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getOwnershipType() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getRentAgreementUrl() {
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
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getHcfType() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getCity() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Integer getSeatCount() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component1() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component10() {
        return null;
    }
    
    public final boolean component11() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Integer component12() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component13() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Double component14() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component15() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component16() {
        return null;
    }
    
    public final boolean component17() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component18() {
        return null;
    }
    
    public final double component19() {
        return 0.0;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component2() {
        return null;
    }
    
    public final double component20() {
        return 0.0;
    }
    
    public final float component21() {
        return 0.0F;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component22() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component23() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component24() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component25() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component26() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component27() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Integer component28() {
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
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component9() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.smartcbwtf.mobile.network.model.HcfRegistrationRequest copy(@org.jetbrains.annotations.NotNull()
    java.lang.String name, @org.jetbrains.annotations.Nullable()
    java.lang.String address, @org.jetbrains.annotations.Nullable()
    java.lang.String pincode, @org.jetbrains.annotations.Nullable()
    java.lang.String state, @org.jetbrains.annotations.Nullable()
    java.lang.String phone, @org.jetbrains.annotations.Nullable()
    java.lang.String email, @org.jetbrains.annotations.Nullable()
    java.lang.String doctorName, @org.jetbrains.annotations.Nullable()
    java.lang.String panNo, @org.jetbrains.annotations.Nullable()
    java.lang.String gstNo, @org.jetbrains.annotations.Nullable()
    java.lang.String aadharNo, boolean bedded, @org.jetbrains.annotations.Nullable()
    java.lang.Integer numberOfBeds, @org.jetbrains.annotations.Nullable()
    java.lang.String otherNotes, @org.jetbrains.annotations.Nullable()
    java.lang.Double monthlyCharges, @org.jetbrains.annotations.NotNull()
    java.lang.String ownershipType, @org.jetbrains.annotations.Nullable()
    java.lang.String rentAgreementUrl, boolean termsAccepted, @org.jetbrains.annotations.Nullable()
    java.lang.String termsVersion, double gpsLatitude, double gpsLongitude, float gpsAccuracy, @org.jetbrains.annotations.Nullable()
    java.lang.String agreementStartDate, @org.jetbrains.annotations.Nullable()
    java.lang.String agreementEndDate, @org.jetbrains.annotations.Nullable()
    java.lang.String facilityId, @org.jetbrains.annotations.Nullable()
    java.lang.String registeredByUserId, @org.jetbrains.annotations.Nullable()
    java.lang.String hcfType, @org.jetbrains.annotations.Nullable()
    java.lang.String city, @org.jetbrains.annotations.Nullable()
    java.lang.Integer seatCount) {
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