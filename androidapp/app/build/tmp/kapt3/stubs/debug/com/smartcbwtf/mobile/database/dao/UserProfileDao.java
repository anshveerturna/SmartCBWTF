package com.smartcbwtf.mobile.database.dao;

/**
 * DAO for user profile cache operations.
 *
 * DESIGN NOTE: This DAO is intentionally limited to:
 * - Insert/replace (to cache fresh data from backend)
 * - Query (to read cached data for offline use)
 * - Delete (to clear cache on logout)
 *
 * There are NO update operations because profile data
 * is never modified on the client side.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000 \n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\bg\u0018\u00002\u00020\u0001J\u000e\u0010\u0002\u001a\u00020\u0003H\u00a7@\u00a2\u0006\u0002\u0010\u0004J\u0010\u0010\u0005\u001a\u0004\u0018\u00010\u0006H\u00a7@\u00a2\u0006\u0002\u0010\u0004J\u0010\u0010\u0007\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00060\bH\'J\u0016\u0010\t\u001a\u00020\u00032\u0006\u0010\n\u001a\u00020\u0006H\u00a7@\u00a2\u0006\u0002\u0010\u000b\u00a8\u0006\f"}, d2 = {"Lcom/smartcbwtf/mobile/database/dao/UserProfileDao;", "", "clearProfile", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getProfile", "Lcom/smartcbwtf/mobile/database/entity/UserProfileEntity;", "getProfileFlow", "Lkotlinx/coroutines/flow/Flow;", "insertProfile", "profile", "(Lcom/smartcbwtf/mobile/database/entity/UserProfileEntity;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
@androidx.room.Dao()
public abstract interface UserProfileDao {
    
    /**
     * Insert or replace the cached profile.
     * Called after fetching fresh data from backend.
     */
    @androidx.room.Insert(onConflict = 1)
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object insertProfile(@org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.database.entity.UserProfileEntity profile, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    /**
     * Get the cached profile as a Flow for reactive updates.
     */
    @androidx.room.Query(value = "SELECT * FROM user_profile LIMIT 1")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<com.smartcbwtf.mobile.database.entity.UserProfileEntity> getProfileFlow();
    
    /**
     * Get the cached profile (one-shot).
     */
    @androidx.room.Query(value = "SELECT * FROM user_profile LIMIT 1")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getProfile(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.smartcbwtf.mobile.database.entity.UserProfileEntity> $completion);
    
    /**
     * Clear cached profile on logout.
     */
    @androidx.room.Query(value = "DELETE FROM user_profile")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object clearProfile(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
}