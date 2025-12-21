package com.smartcbwtf.mobile.storage;

/**
 * Secure storage for authentication token and security flags.
 * Uses EncryptedSharedPreferences (injected via Hilt) for secure persistence.
 *
 * SECURITY: mustChangePassword flag is persisted to ensure enforcement
 * survives app restart - users cannot bypass by force-killing the app.
 */
@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0007\b\u0007\u0018\u0000 \u00162\u00020\u0001:\u0001\u0016B\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0006\u0010\n\u001a\u00020\u000bJ\b\u0010\f\u001a\u00020\u0006H\u0016J\u0010\u0010\r\u001a\u0004\u0018\u00010\tH\u0096@\u00a2\u0006\u0002\u0010\u000eJ\u0010\u0010\u000f\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\t0\u0010H\u0016J\u0010\u0010\u0011\u001a\u00020\u000b2\u0006\u0010\u0012\u001a\u00020\u0006H\u0016J\u0018\u0010\u0013\u001a\u00020\u000b2\b\u0010\u0014\u001a\u0004\u0018\u00010\tH\u0096@\u00a2\u0006\u0002\u0010\u0015R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u0007\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\t0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0017"}, d2 = {"Lcom/smartcbwtf/mobile/storage/DefaultAuthTokenStore;", "Lcom/smartcbwtf/mobile/storage/AuthTokenStore;", "prefs", "Landroid/content/SharedPreferences;", "(Landroid/content/SharedPreferences;)V", "_mustChangePassword", "", "_tokenFlow", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "clearAll", "", "getMustChangePassword", "getToken", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getTokenFlow", "Lkotlinx/coroutines/flow/Flow;", "setMustChangePassword", "required", "setToken", "token", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "Companion", "app_debug"})
public final class DefaultAuthTokenStore implements com.smartcbwtf.mobile.storage.AuthTokenStore {
    @org.jetbrains.annotations.NotNull()
    private final android.content.SharedPreferences prefs = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.String> _tokenFlow = null;
    @kotlin.jvm.Volatile()
    private volatile boolean _mustChangePassword;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "AuthTokenStore";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_TOKEN = "auth_token";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_MUST_CHANGE_PASSWORD = "must_change_password";
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
    
    /**
     * Get the mustChangePassword flag.
     * This is read from persistent storage on first access, then cached.
     *
     * SECURITY: Returns true if user must change password before accessing app.
     */
    @java.lang.Override()
    public boolean getMustChangePassword() {
        return false;
    }
    
    /**
     * Set the mustChangePassword flag.
     * Persisted immediately to survive app restart/kill.
     *
     * SECURITY: This MUST be persisted, not just in-memory, to prevent bypass.
     */
    @java.lang.Override()
    public void setMustChangePassword(boolean required) {
    }
    
    /**
     * Clear all auth state (for complete logout).
     */
    public final void clearAll() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0007"}, d2 = {"Lcom/smartcbwtf/mobile/storage/DefaultAuthTokenStore$Companion;", "", "()V", "KEY_MUST_CHANGE_PASSWORD", "", "KEY_TOKEN", "TAG", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}