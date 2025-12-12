package com.smartcbwtf.mobile.storage;

/**
 * Manages user session data including authentication token, user info, and facility info.
 */
@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00004\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\b\n\u0002\u0010\u0002\n\u0002\b\b\b\u0007\u0018\u0000 \u001f2\u00020\u0001:\u0001\u001fB\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u000e\u0010\u0017\u001a\u00020\u0018H\u0086@\u00a2\u0006\u0002\u0010\u0019J\b\u0010\u001a\u001a\u0004\u0018\u00010\tJ\u0006\u0010\u001b\u001a\u00020\u0007JN\u0010\u001c\u001a\u00020\u00182\u0006\u0010\u001d\u001a\u00020\t2\u0006\u0010\u0011\u001a\u00020\t2\n\b\u0002\u0010\u0013\u001a\u0004\u0018\u00010\t2\n\b\u0002\u0010\u0015\u001a\u0004\u0018\u00010\t2\n\b\u0002\u0010\f\u001a\u0004\u0018\u00010\t2\n\b\u0002\u0010\b\u001a\u0004\u0018\u00010\tH\u0086@\u00a2\u0006\u0002\u0010\u001eR\u0014\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0013\u0010\b\u001a\u0004\u0018\u00010\t8F\u00a2\u0006\u0006\u001a\u0004\b\n\u0010\u000bR\u0013\u0010\f\u001a\u0004\u0018\u00010\t8F\u00a2\u0006\u0006\u001a\u0004\b\r\u0010\u000bR\u0017\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00070\u000f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u0010R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0013\u0010\u0011\u001a\u0004\u0018\u00010\t8F\u00a2\u0006\u0006\u001a\u0004\b\u0012\u0010\u000bR\u0013\u0010\u0013\u001a\u0004\u0018\u00010\t8F\u00a2\u0006\u0006\u001a\u0004\b\u0014\u0010\u000bR\u0013\u0010\u0015\u001a\u0004\u0018\u00010\t8F\u00a2\u0006\u0006\u001a\u0004\b\u0016\u0010\u000b\u00a8\u0006 "}, d2 = {"Lcom/smartcbwtf/mobile/storage/SessionManager;", "", "prefs", "Landroid/content/SharedPreferences;", "(Landroid/content/SharedPreferences;)V", "_isLoggedIn", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "facilityCode", "", "getFacilityCode", "()Ljava/lang/String;", "facilityId", "getFacilityId", "isLoggedIn", "Lkotlinx/coroutines/flow/Flow;", "()Lkotlinx/coroutines/flow/Flow;", "userId", "getUserId", "userName", "getUserName", "userRole", "getUserRole", "clearSession", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getToken", "isUserLoggedIn", "saveSession", "token", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "Companion", "app_debug"})
public final class SessionManager {
    @org.jetbrains.annotations.NotNull()
    private final android.content.SharedPreferences prefs = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Boolean> _isLoggedIn = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.Flow<java.lang.Boolean> isLoggedIn = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_TOKEN = "auth_token";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_USER_ID = "user_id";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_USER_NAME = "user_name";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_USER_ROLE = "user_role";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_FACILITY_ID = "facility_id";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_FACILITY_CODE = "facility_code";
    @org.jetbrains.annotations.NotNull()
    public static final com.smartcbwtf.mobile.storage.SessionManager.Companion Companion = null;
    
    @javax.inject.Inject()
    public SessionManager(@org.jetbrains.annotations.NotNull()
    android.content.SharedPreferences prefs) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<java.lang.Boolean> isLoggedIn() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getUserId() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getFacilityId() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getFacilityCode() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getUserName() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getUserRole() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object saveSession(@org.jetbrains.annotations.NotNull()
    java.lang.String token, @org.jetbrains.annotations.NotNull()
    java.lang.String userId, @org.jetbrains.annotations.Nullable()
    java.lang.String userName, @org.jetbrains.annotations.Nullable()
    java.lang.String userRole, @org.jetbrains.annotations.Nullable()
    java.lang.String facilityId, @org.jetbrains.annotations.Nullable()
    java.lang.String facilityCode, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object clearSession(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getToken() {
        return null;
    }
    
    public final boolean isUserLoggedIn() {
        return false;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0006\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\n"}, d2 = {"Lcom/smartcbwtf/mobile/storage/SessionManager$Companion;", "", "()V", "KEY_FACILITY_CODE", "", "KEY_FACILITY_ID", "KEY_TOKEN", "KEY_USER_ID", "KEY_USER_NAME", "KEY_USER_ROLE", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}