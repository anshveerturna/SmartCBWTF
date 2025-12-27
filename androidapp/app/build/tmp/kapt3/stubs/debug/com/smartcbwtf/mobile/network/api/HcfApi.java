package com.smartcbwtf.mobile.network.api;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000@\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010$\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\bf\u0018\u00002\u00020\u0001J\u0014\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003H\u00a7@\u00a2\u0006\u0002\u0010\u0005J\u001a\u0010\u0006\u001a\u00020\u00072\n\b\u0003\u0010\b\u001a\u0004\u0018\u00010\tH\u00a7@\u00a2\u0006\u0002\u0010\nJ\u0018\u0010\u000b\u001a\u00020\f2\b\b\u0001\u0010\r\u001a\u00020\u000eH\u00a7@\u00a2\u0006\u0002\u0010\u000fJ$\u0010\u0010\u001a\u000e\u0012\u0004\u0012\u00020\t\u0012\u0004\u0012\u00020\t0\u00112\b\b\u0001\u0010\u0012\u001a\u00020\u0013H\u00a7@\u00a2\u0006\u0002\u0010\u0014\u00a8\u0006\u0015"}, d2 = {"Lcom/smartcbwtf/mobile/network/api/HcfApi;", "", "getAll", "", "Lcom/smartcbwtf/mobile/network/model/HcfDto;", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getLatestTerms", "Lcom/smartcbwtf/mobile/network/model/TermsResponse;", "facilityId", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "register", "Lcom/smartcbwtf/mobile/network/model/HcfRegistrationResponse;", "request", "Lcom/smartcbwtf/mobile/network/model/HcfRegistrationRequest;", "(Lcom/smartcbwtf/mobile/network/model/HcfRegistrationRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "uploadRentAgreement", "", "file", "Lokhttp3/MultipartBody$Part;", "(Lokhttp3/MultipartBody$Part;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public abstract interface HcfApi {
    
    @retrofit2.http.GET(value = "hcfs")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getAll(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.smartcbwtf.mobile.network.model.HcfDto>> $completion);
    
    @retrofit2.http.POST(value = "hcfs/register")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object register(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.network.model.HcfRegistrationRequest request, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.smartcbwtf.mobile.network.model.HcfRegistrationResponse> $completion);
    
    @retrofit2.http.GET(value = "terms/latest")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getLatestTerms(@retrofit2.http.Query(value = "facilityId")
    @org.jetbrains.annotations.Nullable()
    java.lang.String facilityId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.smartcbwtf.mobile.network.model.TermsResponse> $completion);
    
    @retrofit2.http.Multipart()
    @retrofit2.http.POST(value = "hcfs/rent-agreement")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object uploadRentAgreement(@retrofit2.http.Part()
    @org.jetbrains.annotations.NotNull()
    okhttp3.MultipartBody.Part file, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.Map<java.lang.String, java.lang.String>> $completion);
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 3, xi = 48)
    public static final class DefaultImpls {
    }
}