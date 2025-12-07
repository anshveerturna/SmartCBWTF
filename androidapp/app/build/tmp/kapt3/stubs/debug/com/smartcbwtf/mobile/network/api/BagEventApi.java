package com.smartcbwtf.mobile.network.api;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001c\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\bf\u0018\u00002\u00020\u0001J\u001e\u0010\u0002\u001a\u00020\u00032\u000e\b\u0001\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0007\u00a8\u0006\b"}, d2 = {"Lcom/smartcbwtf/mobile/network/api/BagEventApi;", "", "sync", "Lcom/smartcbwtf/mobile/network/model/SyncResponse;", "payload", "", "Lcom/smartcbwtf/mobile/network/model/BagEventPayload;", "(Ljava/util/List;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public abstract interface BagEventApi {
    
    @retrofit2.http.POST(value = "bags/sync")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object sync(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    java.util.List<com.smartcbwtf.mobile.network.model.BagEventPayload> payload, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.smartcbwtf.mobile.network.model.SyncResponse> $completion);
}