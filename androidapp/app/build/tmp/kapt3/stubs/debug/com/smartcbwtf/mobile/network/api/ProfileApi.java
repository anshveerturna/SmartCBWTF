package com.smartcbwtf.mobile.network.api;

/**
 * Profile API for fetching user profile data.
 *
 * IMPORTANT: This API is READ-ONLY by design EXCEPT for password change.
 * Profile data is centrally managed at the backend level.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000$\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\bf\u0018\u00002\u00020\u0001J\u001e\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\b\b\u0001\u0010\u0005\u001a\u00020\u0006H\u00a7@\u00a2\u0006\u0002\u0010\u0007J\u000e\u0010\b\u001a\u00020\tH\u00a7@\u00a2\u0006\u0002\u0010\n\u00a8\u0006\u000b"}, d2 = {"Lcom/smartcbwtf/mobile/network/api/ProfileApi;", "", "changePassword", "Lretrofit2/Response;", "", "request", "Lcom/smartcbwtf/mobile/ui/ChangePasswordRequest;", "(Lcom/smartcbwtf/mobile/ui/ChangePasswordRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getCurrentUser", "Lcom/smartcbwtf/mobile/network/model/UserProfileResponse;", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public abstract interface ProfileApi {
    
    /**
     * Get the current authenticated user's profile.
     * Used for identity confirmation only - not for editing.
     */
    @retrofit2.http.GET(value = "users/me")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getCurrentUser(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.smartcbwtf.mobile.network.model.UserProfileResponse> $completion);
    
    /**
     * Change the current user's password.
     * Required when mustChangePassword flag is set.
     */
    @retrofit2.http.POST(value = "users/me/change-password")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object changePassword(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.ui.ChangePasswordRequest request, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<kotlin.Unit>> $completion);
}