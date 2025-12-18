package com.smartcbwtf.mobile.database.dao;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000F\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0000\n\u0002\u0010\b\n\u0002\b\u000b\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\t\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\b\bg\u0018\u00002\u00020\u0001J\u0016\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006J\u0018\u0010\u0007\u001a\u0004\u0018\u00010\b2\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006J\u001e\u0010\t\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\b0\u000b0\n2\b\b\u0002\u0010\f\u001a\u00020\rH\'J\u0010\u0010\u000e\u001a\u0004\u0018\u00010\bH\u00a7@\u00a2\u0006\u0002\u0010\u000fJ\u0014\u0010\u0010\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\b0\u000b0\nH\'J\u0014\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\b0\u000bH\u00a7@\u00a2\u0006\u0002\u0010\u000fJ\u0016\u0010\u0012\u001a\u00020\u00032\u0006\u0010\u0013\u001a\u00020\bH\u00a7@\u00a2\u0006\u0002\u0010\u0014J\u001c\u0010\u0015\u001a\u00020\u00032\f\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\b0\u000bH\u00a7@\u00a2\u0006\u0002\u0010\u0017J\u0016\u0010\u0018\u001a\u00020\u00192\u0006\u0010\u001a\u001a\u00020\u001bH\u00a7@\u00a2\u0006\u0002\u0010\u001cJ\u001e\u0010\u001d\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u001e\u001a\u00020\u001fH\u00a7@\u00a2\u0006\u0002\u0010 J&\u0010!\u001a\u00020\u00032\f\u0010\"\u001a\b\u0012\u0004\u0012\u00020\u00050\u000b2\b\b\u0002\u0010#\u001a\u00020\u001bH\u00a7@\u00a2\u0006\u0002\u0010$J\u000e\u0010%\u001a\b\u0012\u0004\u0012\u00020\r0\nH\'J\u0016\u0010&\u001a\u00020\u00032\u0006\u0010\u0013\u001a\u00020\bH\u00a7@\u00a2\u0006\u0002\u0010\u0014\u00a8\u0006\'"}, d2 = {"Lcom/smartcbwtf/mobile/database/dao/AttendanceDao;", "", "deleteById", "", "id", "Ljava/util/UUID;", "(Ljava/util/UUID;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "findById", "Lcom/smartcbwtf/mobile/database/entity/AttendanceEventEntity;", "getHistory", "Lkotlinx/coroutines/flow/Flow;", "", "limit", "", "getLatest", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getPending", "getPendingList", "insert", "event", "(Lcom/smartcbwtf/mobile/database/entity/AttendanceEventEntity;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "insertAll", "events", "(Ljava/util/List;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "isCooldownActive", "", "cooldownStartMs", "", "(JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "markSyncError", "error", "", "(Ljava/util/UUID;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "markSynced", "ids", "syncedAt", "(Ljava/util/List;JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "pendingCount", "update", "app_debug"})
@androidx.room.Dao()
public abstract interface AttendanceDao {
    
    @androidx.room.Insert(onConflict = 1)
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object insert(@org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.database.entity.AttendanceEventEntity event, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Insert(onConflict = 1)
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object insertAll(@org.jetbrains.annotations.NotNull()
    java.util.List<com.smartcbwtf.mobile.database.entity.AttendanceEventEntity> events, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Update()
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object update(@org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.database.entity.AttendanceEventEntity event, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM attendance_events WHERE synced = 0 ORDER BY eventTs ASC")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.smartcbwtf.mobile.database.entity.AttendanceEventEntity>> getPending();
    
    @androidx.room.Query(value = "SELECT * FROM attendance_events WHERE synced = 0 ORDER BY eventTs ASC")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getPendingList(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.smartcbwtf.mobile.database.entity.AttendanceEventEntity>> $completion);
    
    @androidx.room.Query(value = "SELECT COUNT(*) FROM attendance_events WHERE synced = 0")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.lang.Integer> pendingCount();
    
    @androidx.room.Query(value = "SELECT * FROM attendance_events WHERE id = :id")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object findById(@org.jetbrains.annotations.NotNull()
    java.util.UUID id, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.smartcbwtf.mobile.database.entity.AttendanceEventEntity> $completion);
    
    @androidx.room.Query(value = "UPDATE attendance_events SET synced = 1, syncedAt = :syncedAt, syncError = NULL WHERE id IN (:ids)")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object markSynced(@org.jetbrains.annotations.NotNull()
    java.util.List<java.util.UUID> ids, long syncedAt, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "UPDATE attendance_events SET syncError = :error WHERE id = :id")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object markSyncError(@org.jetbrains.annotations.NotNull()
    java.util.UUID id, @org.jetbrains.annotations.NotNull()
    java.lang.String error, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "DELETE FROM attendance_events WHERE id = :id")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object deleteById(@org.jetbrains.annotations.NotNull()
    java.util.UUID id, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    /**
     * Get the most recent attendance event (for cooldown checking).
     * Returns the latest event regardless of sync status.
     */
    @androidx.room.Query(value = "SELECT * FROM attendance_events ORDER BY eventTs DESC LIMIT 1")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getLatest(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.smartcbwtf.mobile.database.entity.AttendanceEventEntity> $completion);
    
    /**
     * Get attendance history for display.
     */
    @androidx.room.Query(value = "SELECT * FROM attendance_events ORDER BY eventTs DESC LIMIT :limit")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.smartcbwtf.mobile.database.entity.AttendanceEventEntity>> getHistory(int limit);
    
    /**
     * Check if cooldown is active (any attendance within cooldown window).
     */
    @androidx.room.Query(value = "SELECT COUNT(*) > 0 FROM attendance_events WHERE eventTs > :cooldownStartMs")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object isCooldownActive(long cooldownStartMs, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion);
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 3, xi = 48)
    public static final class DefaultImpls {
    }
}