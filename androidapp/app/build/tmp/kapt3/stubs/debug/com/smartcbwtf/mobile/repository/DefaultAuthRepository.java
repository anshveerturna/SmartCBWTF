package com.smartcbwtf.mobile.repository;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000R\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\b\u0007\u0018\u00002\u00020\u0001B9\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u0012\u0006\u0010\n\u001a\u00020\u000b\u0012\b\b\u0002\u0010\f\u001a\u00020\r\u00a2\u0006\u0002\u0010\u000eJ\u0010\u0010\u000f\u001a\u0004\u0018\u00010\u0010H\u0096@\u00a2\u0006\u0002\u0010\u0011J\u0012\u0010\u0012\u001a\u0004\u0018\u00010\u00102\u0006\u0010\u0013\u001a\u00020\u0010H\u0002J\u0010\u0010\u0014\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00100\u0015H\u0016J\u001e\u0010\u0016\u001a\u00020\u00172\u0006\u0010\u0018\u001a\u00020\u00102\u0006\u0010\u0019\u001a\u00020\u0010H\u0096@\u00a2\u0006\u0002\u0010\u001aJ\u000e\u0010\u001b\u001a\u00020\u001cH\u0096@\u00a2\u0006\u0002\u0010\u0011J\b\u0010\u001d\u001a\u00020\u001eH\u0016R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\rX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001f"}, d2 = {"Lcom/smartcbwtf/mobile/repository/DefaultAuthRepository;", "Lcom/smartcbwtf/mobile/repository/AuthRepository;", "api", "Lcom/smartcbwtf/mobile/network/api/AuthApi;", "tokenStore", "Lcom/smartcbwtf/mobile/storage/AuthTokenStore;", "appConfigStore", "Lcom/smartcbwtf/mobile/storage/AppConfigStore;", "sessionManager", "Lcom/smartcbwtf/mobile/storage/SessionManager;", "networkMonitor", "Lcom/smartcbwtf/mobile/utils/NetworkMonitor;", "ioDispatcher", "Lkotlinx/coroutines/CoroutineDispatcher;", "(Lcom/smartcbwtf/mobile/network/api/AuthApi;Lcom/smartcbwtf/mobile/storage/AuthTokenStore;Lcom/smartcbwtf/mobile/storage/AppConfigStore;Lcom/smartcbwtf/mobile/storage/SessionManager;Lcom/smartcbwtf/mobile/utils/NetworkMonitor;Lkotlinx/coroutines/CoroutineDispatcher;)V", "currentToken", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "extractUserIdFromJwt", "token", "getAuthStateFlow", "Lkotlinx/coroutines/flow/Flow;", "login", "Lcom/smartcbwtf/mobile/network/model/AuthResponse;", "username", "password", "(Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "logout", "", "mustChangePassword", "", "app_debug"})
public final class DefaultAuthRepository implements com.smartcbwtf.mobile.repository.AuthRepository {
    @org.jetbrains.annotations.NotNull()
    private final com.smartcbwtf.mobile.network.api.AuthApi api = null;
    @org.jetbrains.annotations.NotNull()
    private final com.smartcbwtf.mobile.storage.AuthTokenStore tokenStore = null;
    @org.jetbrains.annotations.NotNull()
    private final com.smartcbwtf.mobile.storage.AppConfigStore appConfigStore = null;
    @org.jetbrains.annotations.NotNull()
    private final com.smartcbwtf.mobile.storage.SessionManager sessionManager = null;
    @org.jetbrains.annotations.NotNull()
    private final com.smartcbwtf.mobile.utils.NetworkMonitor networkMonitor = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.CoroutineDispatcher ioDispatcher = null;
    
    @javax.inject.Inject()
    public DefaultAuthRepository(@org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.network.api.AuthApi api, @org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.storage.AuthTokenStore tokenStore, @org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.storage.AppConfigStore appConfigStore, @org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.storage.SessionManager sessionManager, @org.jetbrains.annotations.NotNull()
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
    
    /**
     * Extract user ID (user_id claim) from JWT token
     */
    private final java.lang.String extractUserIdFromJwt(java.lang.String token) {
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