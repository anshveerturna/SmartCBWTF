package com.smartcbwtf.mobile.repository;

/**
 * Repository for user profile data.
 *
 * DESIGN PRINCIPLES:
 * 1. Backend is the SINGLE SOURCE OF TRUTH
 * 2. Room cache is for OFFLINE READING ONLY
 * 3. NO local modifications to profile data
 *
 * This repository intentionally provides NO methods to update profile data.
 * All profile changes must happen at the backend database level.
 */
@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000,\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u0007\u0018\u00002\u00020\u0001B\u0017\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u000e\u0010\u0007\u001a\u00020\bH\u0086@\u00a2\u0006\u0002\u0010\tJ\u000e\u0010\n\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\f0\u000bJ\u000e\u0010\r\u001a\u00020\fH\u0086@\u00a2\u0006\u0002\u0010\tR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u000e"}, d2 = {"Lcom/smartcbwtf/mobile/repository/ProfileRepository;", "", "profileApi", "Lcom/smartcbwtf/mobile/network/api/ProfileApi;", "userProfileDao", "Lcom/smartcbwtf/mobile/database/dao/UserProfileDao;", "(Lcom/smartcbwtf/mobile/network/api/ProfileApi;Lcom/smartcbwtf/mobile/database/dao/UserProfileDao;)V", "clearCache", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getCachedProfileFlow", "Lkotlinx/coroutines/flow/Flow;", "Lcom/smartcbwtf/mobile/network/model/UserProfileResponse;", "getCurrentUserProfile", "app_debug"})
public final class ProfileRepository {
    @org.jetbrains.annotations.NotNull()
    private final com.smartcbwtf.mobile.network.api.ProfileApi profileApi = null;
    @org.jetbrains.annotations.NotNull()
    private final com.smartcbwtf.mobile.database.dao.UserProfileDao userProfileDao = null;
    
    @javax.inject.Inject()
    public ProfileRepository(@org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.network.api.ProfileApi profileApi, @org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.database.dao.UserProfileDao userProfileDao) {
        super();
    }
    
    /**
     * Get the current user's profile.
     *
     * Strategy:
     * - Online: Fetch from API, cache to Room, return fresh data
     * - Offline: Return cached data if available
     *
     * @return UserProfileResponse or null if unavailable
     * @throws Exception on network error with no cache
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getCurrentUserProfile(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.smartcbwtf.mobile.network.model.UserProfileResponse> $completion) {
        return null;
    }
    
    /**
     * Get cached profile as a Flow for reactive updates.
     * Useful for observing offline data.
     */
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<com.smartcbwtf.mobile.network.model.UserProfileResponse> getCachedProfileFlow() {
        return null;
    }
    
    /**
     * Clear cached profile on logout.
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object clearCache(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
}