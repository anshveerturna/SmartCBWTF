package com.smartcbwtf.mobile.network.api;

/**
 * API for fetching app configuration from backend.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0016\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\bf\u0018\u00002\u00020\u0001J\u0014\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003H\u00a7@\u00a2\u0006\u0002\u0010\u0005\u00a8\u0006\u0006"}, d2 = {"Lcom/smartcbwtf/mobile/network/api/ConfigApi;", "", "getMobileConfig", "Lretrofit2/Response;", "Lcom/smartcbwtf/mobile/network/api/MobileConfigResponse;", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public abstract interface ConfigApi {
    
    @retrofit2.http.GET(value = "/api/config/mobile")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getMobileConfig(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<com.smartcbwtf.mobile.network.api.MobileConfigResponse>> $completion);
}