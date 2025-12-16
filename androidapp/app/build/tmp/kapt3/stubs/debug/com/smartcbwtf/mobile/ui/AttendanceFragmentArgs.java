package com.smartcbwtf.mobile.ui;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00008\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0007\n\u0002\b\t\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\b\u0086\b\u0018\u0000 \u00182\u00020\u0001:\u0001\u0018B\u0019\u0012\b\b\u0002\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0005J\t\u0010\t\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\n\u001a\u00020\u0003H\u00c6\u0003J\u001d\u0010\u000b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\f\u001a\u00020\r2\b\u0010\u000e\u001a\u0004\u0018\u00010\u000fH\u00d6\u0003J\t\u0010\u0010\u001a\u00020\u0011H\u00d6\u0001J\u0006\u0010\u0012\u001a\u00020\u0013J\u0006\u0010\u0014\u001a\u00020\u0015J\t\u0010\u0016\u001a\u00020\u0017H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u0007R\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\u0007\u00a8\u0006\u0019"}, d2 = {"Lcom/smartcbwtf/mobile/ui/AttendanceFragmentArgs;", "Landroidx/navigation/NavArgs;", "latitude", "", "longitude", "(FF)V", "getLatitude", "()F", "getLongitude", "component1", "component2", "copy", "equals", "", "other", "", "hashCode", "", "toBundle", "Landroid/os/Bundle;", "toSavedStateHandle", "Landroidx/lifecycle/SavedStateHandle;", "toString", "", "Companion", "app_debug"})
public final class AttendanceFragmentArgs implements androidx.navigation.NavArgs {
    private final float latitude = 0.0F;
    private final float longitude = 0.0F;
    @org.jetbrains.annotations.NotNull()
    public static final com.smartcbwtf.mobile.ui.AttendanceFragmentArgs.Companion Companion = null;
    
    public AttendanceFragmentArgs(float latitude, float longitude) {
        super();
    }
    
    public final float getLatitude() {
        return 0.0F;
    }
    
    public final float getLongitude() {
        return 0.0F;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final android.os.Bundle toBundle() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final androidx.lifecycle.SavedStateHandle toSavedStateHandle() {
        return null;
    }
    
    public AttendanceFragmentArgs() {
        super();
    }
    
    public final float component1() {
        return 0.0F;
    }
    
    public final float component2() {
        return 0.0F;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.smartcbwtf.mobile.ui.AttendanceFragmentArgs copy(float latitude, float longitude) {
        return null;
    }
    
    @java.lang.Override()
    public boolean equals(@org.jetbrains.annotations.Nullable()
    java.lang.Object other) {
        return false;
    }
    
    @kotlin.jvm.JvmStatic()
    @org.jetbrains.annotations.NotNull()
    public static final com.smartcbwtf.mobile.ui.AttendanceFragmentArgs fromBundle(@org.jetbrains.annotations.NotNull()
    android.os.Bundle bundle) {
        return null;
    }
    
    @kotlin.jvm.JvmStatic()
    @org.jetbrains.annotations.NotNull()
    public static final com.smartcbwtf.mobile.ui.AttendanceFragmentArgs fromSavedStateHandle(@org.jetbrains.annotations.NotNull()
    androidx.lifecycle.SavedStateHandle savedStateHandle) {
        return null;
    }
    
    @java.lang.Override()
    public int hashCode() {
        return 0;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public java.lang.String toString() {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000 \n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006H\u0007J\u0010\u0010\u0007\u001a\u00020\u00042\u0006\u0010\b\u001a\u00020\tH\u0007\u00a8\u0006\n"}, d2 = {"Lcom/smartcbwtf/mobile/ui/AttendanceFragmentArgs$Companion;", "", "()V", "fromBundle", "Lcom/smartcbwtf/mobile/ui/AttendanceFragmentArgs;", "bundle", "Landroid/os/Bundle;", "fromSavedStateHandle", "savedStateHandle", "Landroidx/lifecycle/SavedStateHandle;", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @kotlin.jvm.JvmStatic()
        @org.jetbrains.annotations.NotNull()
        public final com.smartcbwtf.mobile.ui.AttendanceFragmentArgs fromBundle(@org.jetbrains.annotations.NotNull()
        android.os.Bundle bundle) {
            return null;
        }
        
        @kotlin.jvm.JvmStatic()
        @org.jetbrains.annotations.NotNull()
        public final com.smartcbwtf.mobile.ui.AttendanceFragmentArgs fromSavedStateHandle(@org.jetbrains.annotations.NotNull()
        androidx.lifecycle.SavedStateHandle savedStateHandle) {
            return null;
        }
    }
}