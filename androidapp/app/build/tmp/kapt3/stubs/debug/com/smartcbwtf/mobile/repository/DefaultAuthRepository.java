package com.smartcbwtf.mobile.repository;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000L\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\b\u0007\u0018\u00002\u00020\u0001B1\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u0012\b\b\u0002\u0010\n\u001a\u00020\u000b\u00a2\u0006\u0002\u0010\fJ\u0010\u0010\r\u001a\u0004\u0018\u00010\u000eH\u0096@\u00a2\u0006\u0002\u0010\u000fJ\u0010\u0010\u0010\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u000e0\u0011H\u0016J\u001e\u0010\u0012\u001a\u00020\u00132\u0006\u0010\u0014\u001a\u00020\u000e2\u0006\u0010\u0015\u001a\u00020\u000eH\u0096@\u00a2\u0006\u0002\u0010\u0016J\u000e\u0010\u0017\u001a\u00020\u0018H\u0096@\u00a2\u0006\u0002\u0010\u000fJ\b\u0010\u0019\u001a\u00020\u001aH\u0016R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001b"}, d2 = {"Lcom/smartcbwtf/mobile/repository/DefaultAuthRepository;", "Lcom/smartcbwtf/mobile/repository/AuthRepository;", "api", "Lcom/smartcbwtf/mobile/network/api/AuthApi;", "tokenStore", "Lcom/smartcbwtf/mobile/storage/AuthTokenStore;", "appConfigStore", "Lcom/smartcbwtf/mobile/storage/AppConfigStore;", "networkMonitor", "Lcom/smartcbwtf/mobile/utils/NetworkMonitor;", "ioDispatcher", "Lkotlinx/coroutines/CoroutineDispatcher;", "(Lcom/smartcbwtf/mobile/network/api/AuthApi;Lcom/smartcbwtf/mobile/storage/AuthTokenStore;Lcom/smartcbwtf/mobile/storage/AppConfigStore;Lcom/smartcbwtf/mobile/utils/NetworkMonitor;Lkotlinx/coroutines/CoroutineDispatcher;)V", "currentToken", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getAuthStateFlow", "Lkotlinx/coroutines/flow/Flow;", "login", "Lcom/smartcbwtf/mobile/network/model/AuthResponse;", "username", "password", "(Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "logout", "", "mustChangePassword", "", "app_debug"})
public final class DefaultAuthRepository implements com.smartcbwtf.mobile.repository.AuthRepository {
    @org.jetbrains.annotations.NotNull()
    private final com.smartcbwtf.mobile.network.api.AuthApi api = null;
    @org.jetbrains.annotations.NotNull()
    private final com.smartcbwtf.mobile.storage.AuthTokenStore tokenStore = null;
    @org.jetbrains.annotations.NotNull()
    private final com.smartcbwtf.mobile.storage.AppConfigStore appConfigStore = null;
    @org.jetbrains.annotations.NotNull()
    private final com.smartcbwtf.mobile.utils.NetworkMonitor networkMonitor = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.CoroutineDispatcher ioDispatcher = null;
    
    @javax.inject.Inject()
    public DefaultAuthRepository(@org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.network.api.AuthApi api, @org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.storage.AuthTokenStore tokenStore, @org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.storage.AppConfigStore appConfigStore, @org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.utils.NetworkMonitor networkMonitor, @org.jetbrains.annotations.NotNull()
    kotlinx.coroutines.CoroutineDispatcher ioDispatcher) {
        super();
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object login(@org.jetbrains.annotations.NotNull()
    java.lang.String username, @org.jetbrains.annotations.NotNull()
    java.lang.String password, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.smartcbwtf.mobile.network.model.AuthResponse> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object logout(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object currentToken(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.String> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public kotlinx.coroutines.flow.Flow<java.lang.String> getAuthStateFlow() {
        return null;
    }
    
    @java.lang.Override()
    public boolean mustChangePassword() {
        return false;
    }
}