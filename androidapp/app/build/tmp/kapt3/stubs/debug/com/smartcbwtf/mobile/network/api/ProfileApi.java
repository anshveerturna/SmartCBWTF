package com.smartcbwtf.mobile.network.api;

/**
 * Profile API for fetching user profile data.
 *
 * IMPORTANT: This API is READ-ONLY by design.
 * Profile data is centrally managed at the backend level.
 * There are intentionally NO mutation endpoints (POST/PUT/PATCH).
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\bf\u0018\u00002\u00020\u0001J\u000e\u0010\u0002\u001a\u00020\u0003H\u00a7@\u00a2\u0006\u0002\u0010\u0004\u00a8\u0006\u0005"}, d2 = {"Lcom/smartcbwtf/mobile/network/api/ProfileApi;", "", "getCurrentUser", "Lcom/smartcbwtf/mobile/network/model/UserProfileResponse;", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public abstract interface ProfileApi {
    
    /**
     * Get the current authenticated user's profile.
     * Used for identity confirmation only - not for editing.
     */
    @retrofit2.http.GET(value = "users/me")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getCurrentUser(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.smartcbwtf.mobile.network.model.UserProfileResponse> $completion);
}