package com.smartcbwtf.mobile.repository;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000V\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u0007\u0018\u00002\u00020\u0001B)\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\b\b\u0002\u0010\b\u001a\u00020\t\u00a2\u0006\u0002\u0010\nJ\u0014\u0010\u000b\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u000e0\r0\fH\u0016J\u0018\u0010\u000f\u001a\u00020\u00102\b\u0010\u0011\u001a\u0004\u0018\u00010\u0012H\u0096@\u00a2\u0006\u0002\u0010\u0013J\u000e\u0010\u0014\u001a\u00020\u0015H\u0096@\u00a2\u0006\u0002\u0010\u0016J\u0016\u0010\u0017\u001a\u00020\u00182\u0006\u0010\u0019\u001a\u00020\u001aH\u0096@\u00a2\u0006\u0002\u0010\u001bR\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001c"}, d2 = {"Lcom/smartcbwtf/mobile/repository/DefaultHcfRepository;", "Lcom/smartcbwtf/mobile/repository/HcfRepository;", "dao", "Lcom/smartcbwtf/mobile/database/dao/HcfDao;", "api", "Lcom/smartcbwtf/mobile/network/api/HcfApi;", "networkMonitor", "Lcom/smartcbwtf/mobile/utils/NetworkMonitor;", "ioDispatcher", "Lkotlinx/coroutines/CoroutineDispatcher;", "(Lcom/smartcbwtf/mobile/database/dao/HcfDao;Lcom/smartcbwtf/mobile/network/api/HcfApi;Lcom/smartcbwtf/mobile/utils/NetworkMonitor;Lkotlinx/coroutines/CoroutineDispatcher;)V", "getAll", "Lkotlinx/coroutines/flow/Flow;", "", "Lcom/smartcbwtf/mobile/database/entity/HcfEntity;", "getLatestTerms", "Lcom/smartcbwtf/mobile/network/model/TermsResponse;", "facilityId", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "refresh", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "register", "Lcom/smartcbwtf/mobile/network/model/HcfRegistrationResponse;", "request", "Lcom/smartcbwtf/mobile/network/model/HcfRegistrationRequest;", "(Lcom/smartcbwtf/mobile/network/model/HcfRegistrationRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public final class DefaultHcfRepository implements com.smartcbwtf.mobile.repository.HcfRepository {
    @org.jetbrains.annotations.NotNull()
    private final com.smartcbwtf.mobile.database.dao.HcfDao dao = null;
    @org.jetbrains.annotations.NotNull()
    private final com.smartcbwtf.mobile.network.api.HcfApi api = null;
    @org.jetbrains.annotations.NotNull()
    private final com.smartcbwtf.mobile.utils.NetworkMonitor networkMonitor = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.CoroutineDispatcher ioDispatcher = null;
    
    @javax.inject.Inject()
    public DefaultHcfRepository(@org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.database.dao.HcfDao dao, @org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.network.api.HcfApi api, @org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.utils.NetworkMonitor networkMonitor, @org.jetbrains.annotations.NotNull()
    kotlinx.coroutines.CoroutineDispatcher ioDispatcher) {
        super();
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public kotlinx.coroutines.flow.Flow<java.util.List<com.smartcbwtf.mobile.database.entity.HcfEntity>> getAll() {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object refresh(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object register(@org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.network.model.HcfRegistrationRequest request, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.smartcbwtf.mobile.network.model.HcfRegistrationResponse> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object getLatestTerms(@org.jetbrains.annotations.Nullable()
    java.lang.String facilityId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.smartcbwtf.mobile.network.model.TermsResponse> $completion) {
        return null;
    }
}