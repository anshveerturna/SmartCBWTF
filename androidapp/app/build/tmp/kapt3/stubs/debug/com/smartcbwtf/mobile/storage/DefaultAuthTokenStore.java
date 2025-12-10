package com.smartcbwtf.mobile.storage;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000,\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0004\b\u0007\u0018\u0000 \u00102\u00020\u0001:\u0001\u0010B\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0010\u0010\b\u001a\u0004\u0018\u00010\u0007H\u0096@\u00a2\u0006\u0002\u0010\tJ\u0010\u0010\n\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00070\u000bH\u0016J\u0018\u0010\f\u001a\u00020\r2\b\u0010\u000e\u001a\u0004\u0018\u00010\u0007H\u0096@\u00a2\u0006\u0002\u0010\u000fR\u0016\u0010\u0005\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0011"}, d2 = {"Lcom/smartcbwtf/mobile/storage/DefaultAuthTokenStore;", "Lcom/smartcbwtf/mobile/storage/AuthTokenStore;", "prefs", "Landroid/content/SharedPreferences;", "(Landroid/content/SharedPreferences;)V", "_tokenFlow", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "getToken", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getTokenFlow", "Lkotlinx/coroutines/flow/Flow;", "setToken", "", "token", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "Companion", "app_debug"})
public final class DefaultAuthTokenStore implements com.smartcbwtf.mobile.storage.AuthTokenStore {
    @org.jetbrains.annotations.NotNull()
    private final android.content.SharedPreferences prefs = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.String> _tokenFlow = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_TOKEN = "auth_token";
    @org.jetbrains.annotations.NotNull()
    public static final com.smartcbwtf.mobile.storage.DefaultAuthTokenStore.Companion Companion = null;
    
    @javax.inject.Inject()
    public DefaultAuthTokenStore(@org.jetbrains.annotations.NotNull()
    android.content.SharedPreferences prefs) {
        super();
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object getToken(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.String> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object setToken(@org.jetbrains.annotations.Nullable()
    java.lang.String token, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public kotlinx.coroutines.flow.Flow<java.lang.String> getTokenFlow() {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0005"}, d2 = {"Lcom/smartcbwtf/mobile/storage/DefaultAuthTokenStore$Companion;", "", "()V", "KEY_TOKEN", "", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}