package com.smartcbwtf.mobile.network.model;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\b\u0015\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001BQ\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\u0006\u0010\u0005\u001a\u00020\u0003\u0012\b\u0010\u0006\u001a\u0004\u0018\u00010\u0003\u0012\b\u0010\u0007\u001a\u0004\u0018\u00010\u0003\u0012\b\u0010\b\u001a\u0004\u0018\u00010\u0003\u0012\b\u0010\t\u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010\n\u001a\u0004\u0018\u00010\u000b\u00a2\u0006\u0002\u0010\fJ\t\u0010\u0017\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0018\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0019\u001a\u00020\u0003H\u00c6\u0003J\u000b\u0010\u001a\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010\u001b\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010\u001c\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010\u001d\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010\u001e\u001a\u0004\u0018\u00010\u000bH\u00c6\u0003Jc\u0010\u001f\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u00032\n\b\u0002\u0010\u0006\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u0007\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\b\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\t\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\n\u001a\u0004\u0018\u00010\u000bH\u00c6\u0001J\u0013\u0010 \u001a\u00020!2\b\u0010\"\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010#\u001a\u00020$H\u00d6\u0001J\t\u0010%\u001a\u00020\u0003H\u00d6\u0001R\u0018\u0010\u0006\u001a\u0004\u0018\u00010\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000eR\u0018\u0010\u0007\u001a\u0004\u0018\u00010\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\u000eR\u0016\u0010\u0005\u001a\u00020\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u000eR\u0016\u0010\u0004\u001a\u00020\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u000eR\u0013\u0010\t\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u000eR\u0018\u0010\b\u001a\u0004\u0018\u00010\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u000eR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u000eR\u0018\u0010\n\u001a\u0004\u0018\u00010\u000b8\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0015\u0010\u0016\u00a8\u0006&"}, d2 = {"Lcom/smartcbwtf/mobile/network/model/HcfRegistrationResponse;", "", "status", "", "hcfId", "hcfCode", "agreementId", "agreementNumber", "pdfUrl", "message", "templateUsed", "Lcom/smartcbwtf/mobile/network/model/TemplateInfo;", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lcom/smartcbwtf/mobile/network/model/TemplateInfo;)V", "getAgreementId", "()Ljava/lang/String;", "getAgreementNumber", "getHcfCode", "getHcfId", "getMessage", "getPdfUrl", "getStatus", "getTemplateUsed", "()Lcom/smartcbwtf/mobile/network/model/TemplateInfo;", "component1", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "copy", "equals", "", "other", "hashCode", "", "toString", "app_debug"})
public final class HcfRegistrationResponse {
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String status = null;
    @com.google.gson.annotations.SerializedName(value = "hcfId")
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String hcfId = null;
    @com.google.gson.annotations.SerializedName(value = "hcfCode")
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String hcfCode = null;
    @com.google.gson.annotations.SerializedName(value = "agreementId")
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String agreementId = null;
    @com.google.gson.annotations.SerializedName(value = "agreementNumber")
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String agreementNumber = null;
    @com.google.gson.annotations.SerializedName(value = "pdfUrl")
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String pdfUrl = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String message = null;
    @com.google.gson.annotations.SerializedName(value = "templateUsed")
    @org.jetbrains.annotations.Nullable()
    private final com.smartcbwtf.mobile.network.model.TemplateInfo templateUsed = null;
    
    public HcfRegistrationResponse(@org.jetbrains.annotations.NotNull()
    java.lang.String status, @org.jetbrains.annotations.NotNull()
    java.lang.String hcfId, @org.jetbrains.annotations.NotNull()
    java.lang.String hcfCode, @org.jetbrains.annotations.Nullable()
    java.lang.String agreementId, @org.jetbrains.annotations.Nullable()
    java.lang.String agreementNumber, @org.jetbrains.annotations.Nullable()
    java.lang.String pdfUrl, @org.jetbrains.annotations.Nullable()
    java.lang.String message, @org.jetbrains.annotations.Nullable()
    com.smartcbwtf.mobile.network.model.TemplateInfo templateUsed) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getStatus() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getHcfId() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getHcfCode() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getAgreementId() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getAgreementNumber() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getPdfUrl() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getMessage() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.smartcbwtf.mobile.network.model.TemplateInfo getTemplateUsed() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component1() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component2() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
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
    public final com.smartcbwtf.mobile.network.model.TemplateInfo component8() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.smartcbwtf.mobile.network.model.HcfRegistrationResponse copy(@org.jetbrains.annotations.NotNull()
    java.lang.String status, @org.jetbrains.annotations.NotNull()
    java.lang.String hcfId, @org.jetbrains.annotations.NotNull()
    java.lang.String hcfCode, @org.jetbrains.annotations.Nullable()
    java.lang.String agreementId, @org.jetbrains.annotations.Nullable()
    java.lang.String agreementNumber, @org.jetbrains.annotations.Nullable()
    java.lang.String pdfUrl, @org.jetbrains.annotations.Nullable()
    java.lang.String message, @org.jetbrains.annotations.Nullable()
    com.smartcbwtf.mobile.network.model.TemplateInfo templateUsed) {
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