package com.smartcbwtf.mobile.repository;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000B\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\b\b\u0007\u0018\u00002\u00020\u0001B!\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\b\b\u0002\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\u0014\u0010\t\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\f0\u000b0\nH\u0016J\u001c\u0010\r\u001a\u00020\u000e2\f\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00100\u000bH\u0096@\u00a2\u0006\u0002\u0010\u0011J\u000e\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00130\nH\u0016J\u0016\u0010\u0014\u001a\u00020\u000e2\u0006\u0010\u0015\u001a\u00020\fH\u0096@\u00a2\u0006\u0002\u0010\u0016J\u001c\u0010\u0017\u001a\u00020\u000e2\f\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\f0\u000bH\u0096@\u00a2\u0006\u0002\u0010\u0011J\u000e\u0010\u0019\u001a\u00020\u000eH\u0096@\u00a2\u0006\u0002\u0010\u001aR\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001b"}, d2 = {"Lcom/smartcbwtf/mobile/repository/DefaultBagEventRepository;", "Lcom/smartcbwtf/mobile/repository/BagEventRepository;", "dao", "Lcom/smartcbwtf/mobile/database/dao/BagEventDao;", "api", "Lcom/smartcbwtf/mobile/network/api/BagEventApi;", "ioDispatcher", "Lkotlinx/coroutines/CoroutineDispatcher;", "(Lcom/smartcbwtf/mobile/database/dao/BagEventDao;Lcom/smartcbwtf/mobile/network/api/BagEventApi;Lkotlinx/coroutines/CoroutineDispatcher;)V", "getPending", "Lkotlinx/coroutines/flow/Flow;", "", "Lcom/smartcbwtf/mobile/model/BagEvent;", "markSynced", "", "ids", "Ljava/util/UUID;", "(Ljava/util/List;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "pendingCount", "", "record", "event", "(Lcom/smartcbwtf/mobile/model/BagEvent;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "recordBatch", "events", "syncPending", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public final class DefaultBagEventRepository implements com.smartcbwtf.mobile.repository.BagEventRepository {
    @org.jetbrains.annotations.NotNull()
    private final com.smartcbwtf.mobile.database.dao.BagEventDao dao = null;
    @org.jetbrains.annotations.NotNull()
    private final com.smartcbwtf.mobile.network.api.BagEventApi api = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.CoroutineDispatcher ioDispatcher = null;
    
    @javax.inject.Inject()
    public DefaultBagEventRepository(@org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.database.dao.BagEventDao dao, @org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.network.api.BagEventApi api, @org.jetbrains.annotations.NotNull()
    kotlinx.coroutines.CoroutineDispatcher ioDispatcher) {
        super();
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object record(@org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.model.BagEvent event, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object recordBatch(@org.jetbrains.annotations.NotNull()
    java.util.List<com.smartcbwtf.mobile.model.BagEvent> events, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public kotlinx.coroutines.flow.Flow<java.util.List<com.smartcbwtf.mobile.model.BagEvent>> getPending() {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public kotlinx.coroutines.flow.Flow<java.lang.Integer> pendingCount() {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object markSynced(@org.jetbrains.annotations.NotNull()
    java.util.List<java.util.UUID> ids, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object syncPending(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
}