package com.smartcbwtf.mobile.repository;

/**
 * Repository for attendance events with offline-first queue.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00008\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\t\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0004\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0004\bf\u0018\u00002\u00020\u0001J\u0016\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0003H\u00a6@\u00a2\u0006\u0002\u0010\u0005J\u001e\u0010\u0006\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\t0\b0\u00072\b\b\u0002\u0010\n\u001a\u00020\u000bH&J\u0010\u0010\f\u001a\u0004\u0018\u00010\tH\u00a6@\u00a2\u0006\u0002\u0010\rJ\u0014\u0010\u000e\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\t0\b0\u0007H&J\u0016\u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0004\u001a\u00020\u0003H\u00a6@\u00a2\u0006\u0002\u0010\u0005J\u000e\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u000b0\u0007H&J\u0016\u0010\u0012\u001a\u00020\u00132\u0006\u0010\u0014\u001a\u00020\tH\u00a6@\u00a2\u0006\u0002\u0010\u0015J\u000e\u0010\u0016\u001a\u00020\u0013H\u00a6@\u00a2\u0006\u0002\u0010\r\u00a8\u0006\u0017"}, d2 = {"Lcom/smartcbwtf/mobile/repository/AttendanceRepository;", "", "getCooldownRemainingMs", "", "cooldownDurationMs", "(JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getHistory", "Lkotlinx/coroutines/flow/Flow;", "", "Lcom/smartcbwtf/mobile/database/entity/AttendanceEventEntity;", "limit", "", "getLatest", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getPending", "isCooldownActive", "", "pendingCount", "record", "", "event", "(Lcom/smartcbwtf/mobile/database/entity/AttendanceEventEntity;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "syncPending", "app_debug"})
public abstract interface AttendanceRepository {
    
    /**
     * Record an attendance event locally.
     * Event will be synced to backend via WorkManager.
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object record(@org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.database.entity.AttendanceEventEntity event, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    /**
     * Get all pending (unsynced) attendance events.
     */
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.smartcbwtf.mobile.database.entity.AttendanceEventEntity>> getPending();
    
    /**
     * Get count of pending events.
     */
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.lang.Integer> pendingCount();
    
    /**
     * Sync all pending events to backend.
     * Called by WorkManager.
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object syncPending(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    /**
     * Get the most recent attendance event.
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getLatest(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.smartcbwtf.mobile.database.entity.AttendanceEventEntity> $completion);
    
    /**
     * Check if cooldown is active.
     * @param cooldownDurationMs Duration of cooldown in milliseconds
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object isCooldownActive(long cooldownDurationMs, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion);
    
    /**
     * Get cooldown remaining time in milliseconds.
     * Returns 0 if no cooldown is active.
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getCooldownRemainingMs(long cooldownDurationMs, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Long> $completion);
    
    /**
     * Get attendance history.
     */
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.smartcbwtf.mobile.database.entity.AttendanceEventEntity>> getHistory(int limit);
    
    /**
     * Repository for attendance events with offline-first queue.
     */
    @kotlin.Metadata(mv = {1, 9, 0}, k = 3, xi = 48)
    public static final class DefaultImpls {
    }
}