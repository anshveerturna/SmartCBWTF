package com.smartcbwtf.mobile.network.api;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000,\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\bf\u0018\u00002\u00020\u0001J\u0014\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003H\u00a7@\u00a2\u0006\u0002\u0010\u0005J\u001e\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\b\b\u0001\u0010\u0007\u001a\u00020\bH\u00a7@\u00a2\u0006\u0002\u0010\tJ\u001e\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u000b0\u00032\b\b\u0001\u0010\f\u001a\u00020\rH\u00a7@\u00a2\u0006\u0002\u0010\u000e\u00a8\u0006\u000f"}, d2 = {"Lcom/smartcbwtf/mobile/network/api/VerificationApi;", "", "getActiveFacility", "Lretrofit2/Response;", "Lcom/smartcbwtf/mobile/model/FacilityInfo;", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getFacility", "facilityId", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "verifyBag", "Lcom/smartcbwtf/mobile/model/VerifyBagResponse;", "request", "Lcom/smartcbwtf/mobile/model/VerifyBagRequest;", "(Lcom/smartcbwtf/mobile/model/VerifyBagRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public abstract interface VerificationApi {
    
    /**
     * Verify a bag at the CBWTF facility
     * Server performs authoritative geofence check and records verification event
     */
    @retrofit2.http.POST(value = "bags/verify")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object verifyBag(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.model.VerifyBagRequest request, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<com.smartcbwtf.mobile.model.VerifyBagResponse>> $completion);
    
    /**
     * Get facility information including geofence coordinates
     * Used for client-side pre-check before verification
     */
    @retrofit2.http.GET(value = "facilities/{facilityId}")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getFacility(@retrofit2.http.Path(value = "facilityId")
    @org.jetbrains.annotations.NotNull()
    java.lang.String facilityId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<com.smartcbwtf.mobile.model.FacilityInfo>> $completion);
    
    /**
     * Get the default/active facility for the current user
     * Returns the CBWTF facility assigned to the driver
     */
    @retrofit2.http.GET(value = "facilities/active")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getActiveFacility(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<com.smartcbwtf.mobile.model.FacilityInfo>> $completion);
}