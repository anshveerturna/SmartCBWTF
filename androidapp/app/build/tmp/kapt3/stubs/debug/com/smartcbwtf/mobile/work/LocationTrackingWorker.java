package com.smartcbwtf.mobile.work;

/**
 * WorkManager safety net for location tracking.
 *
 * SECURITY INVARIANTS (same as ForegroundLocationService):
 * - NEVER runs without user consent
 * - Only runs for DRIVER / PLANT_OPERATOR roles
 * - Respects backend GPS config
 *
 * Runs every 15 minutes to:
 * - Restart ForegroundLocationService if killed
 * - Sync backup location
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000@\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u0007\u0018\u0000 \u00142\u00020\u0001:\u0001\u0014B;\b\u0007\u0012\b\b\u0001\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0001\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u0012\u0006\u0010\n\u001a\u00020\u000b\u0012\u0006\u0010\f\u001a\u00020\r\u00a2\u0006\u0002\u0010\u000eJ\u000e\u0010\u000f\u001a\u00020\u0010H\u0082@\u00a2\u0006\u0002\u0010\u0011J\u000e\u0010\u0012\u001a\u00020\u0013H\u0096@\u00a2\u0006\u0002\u0010\u0011R\u000e\u0010\f\u001a\u00020\rX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0015"}, d2 = {"Lcom/smartcbwtf/mobile/work/LocationTrackingWorker;", "Landroidx/work/CoroutineWorker;", "appContext", "Landroid/content/Context;", "params", "Landroidx/work/WorkerParameters;", "locationRepository", "Lcom/smartcbwtf/mobile/repository/LocationRepository;", "locationHelper", "Lcom/smartcbwtf/mobile/utils/LocationHelper;", "authTokenStore", "Lcom/smartcbwtf/mobile/storage/AuthTokenStore;", "appConfigStore", "Lcom/smartcbwtf/mobile/storage/AppConfigStore;", "(Landroid/content/Context;Landroidx/work/WorkerParameters;Lcom/smartcbwtf/mobile/repository/LocationRepository;Lcom/smartcbwtf/mobile/utils/LocationHelper;Lcom/smartcbwtf/mobile/storage/AuthTokenStore;Lcom/smartcbwtf/mobile/storage/AppConfigStore;)V", "canStartTracking", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "doWork", "Landroidx/work/ListenableWorker$Result;", "Companion", "app_debug"})
@androidx.hilt.work.HiltWorker()
public final class LocationTrackingWorker extends androidx.work.CoroutineWorker {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context appContext = null;
    @org.jetbrains.annotations.NotNull()
    private final com.smartcbwtf.mobile.repository.LocationRepository locationRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.smartcbwtf.mobile.utils.LocationHelper locationHelper = null;
    @org.jetbrains.annotations.NotNull()
    private final com.smartcbwtf.mobile.storage.AuthTokenStore authTokenStore = null;
    @org.jetbrains.annotations.NotNull()
    private final com.smartcbwtf.mobile.storage.AppConfigStore appConfigStore = null;
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String TAG = "LocationTrackingWorker";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String WORK_NAME = "location_tracking";
    @org.jetbrains.annotations.NotNull()
    private static final java.util.Set<java.lang.String> ALLOWED_ROLES = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.smartcbwtf.mobile.work.LocationTrackingWorker.Companion Companion = null;
    
    @dagger.assisted.AssistedInject()
    public LocationTrackingWorker(@dagger.assisted.Assisted()
    @org.jetbrains.annotations.NotNull()
    android.content.Context appContext, @dagger.assisted.Assisted()
    @org.jetbrains.annotations.NotNull()
    androidx.work.WorkerParameters params, @org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.repository.LocationRepository locationRepository, @org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.utils.LocationHelper locationHelper, @org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.storage.AuthTokenStore authTokenStore, @org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.storage.AppConfigStore appConfigStore) {
        super(null, null);
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object doWork(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super androidx.work.ListenableWorker.Result> $completion) {
        return null;
    }
    
    /**
     * Centralized guard: ALL conditions must be met before tracking.
     *
     * Required conditions:
     * 1. User is logged in (token present)
     * 2. User has granted location consent
     * 3. GPS is enabled in backend config
     * 4. User role is DRIVER or PLANT_OPERATOR
     */
    private final java.lang.Object canStartTracking(kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\"\n\u0002\u0010\u000e\n\u0002\b\u0003\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u0014\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0005X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0005X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\b"}, d2 = {"Lcom/smartcbwtf/mobile/work/LocationTrackingWorker$Companion;", "", "()V", "ALLOWED_ROLES", "", "", "TAG", "WORK_NAME", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}