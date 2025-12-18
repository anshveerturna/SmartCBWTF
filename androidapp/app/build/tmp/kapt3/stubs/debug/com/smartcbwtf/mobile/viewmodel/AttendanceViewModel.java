package com.smartcbwtf.mobile.viewmodel;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000z\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0010\u000b\n\u0002\b\u0004\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u0006\n\u0002\b\r\b\u0007\u0018\u0000 <2\u00020\u0001:\u0001<B)\b\u0007\u0012\b\b\u0001\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u00a2\u0006\u0002\u0010\nJ\u0006\u0010(\u001a\u00020)J\u000e\u0010*\u001a\u00020\u00132\u0006\u0010+\u001a\u00020\u000fJ\b\u0010,\u001a\u00020\u0013H\u0002J\u0016\u0010-\u001a\u00020.2\u0006\u0010/\u001a\u0002002\u0006\u00101\u001a\u000200J\u0006\u00102\u001a\u00020.J\u0006\u00103\u001a\u00020.J\u0006\u00104\u001a\u00020.J\b\u00105\u001a\u00020.H\u0002J\u0010\u00106\u001a\u00020.2\u0006\u00107\u001a\u00020!H\u0002J\u000e\u00108\u001a\u00020.2\u0006\u00109\u001a\u00020\u0017J\u0012\u0010:\u001a\u00020.2\b\b\u0002\u0010;\u001a\u00020\u000fH\u0002R\u0014\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\r0\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u000f0\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00110\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u0012\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00130\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00150\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u0016\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00170\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\r0\u0019\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001a\u0010\u001bR\u0010\u0010\u001c\u001a\u0004\u0018\u00010\u001dX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u001e\u001a\b\u0012\u0004\u0012\u00020\u000f0\u0019\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001f\u0010\u001bR\u0010\u0010 \u001a\u0004\u0018\u00010!X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\"\u001a\b\u0012\u0004\u0012\u00020\u00110\u0019\u00a2\u0006\b\n\u0000\u001a\u0004\b#\u0010\u001bR\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010$\u001a\b\u0012\u0004\u0012\u00020\u00150\u0019\u00a2\u0006\b\n\u0000\u001a\u0004\b%\u0010\u001bR\u0019\u0010&\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00170\u0019\u00a2\u0006\b\n\u0000\u001a\u0004\b\'\u0010\u001b\u00a8\u0006="}, d2 = {"Lcom/smartcbwtf/mobile/viewmodel/AttendanceViewModel;", "Landroidx/lifecycle/ViewModel;", "appContext", "Landroid/content/Context;", "locationHelper", "Lcom/smartcbwtf/mobile/utils/LocationHelper;", "hcfRepository", "Lcom/smartcbwtf/mobile/repository/HcfRepository;", "attendanceRepository", "Lcom/smartcbwtf/mobile/repository/AttendanceRepository;", "(Landroid/content/Context;Lcom/smartcbwtf/mobile/utils/LocationHelper;Lcom/smartcbwtf/mobile/repository/HcfRepository;Lcom/smartcbwtf/mobile/repository/AttendanceRepository;)V", "_attendanceResult", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/smartcbwtf/mobile/viewmodel/AttendanceResult;", "_cooldownRemainingMs", "", "_hcfSearchState", "Lcom/smartcbwtf/mobile/viewmodel/HcfSearchState;", "_lastMarkedHcfId", "", "_locationState", "Lcom/smartcbwtf/mobile/viewmodel/AttendanceLocationState;", "_selectedHcf", "Lcom/smartcbwtf/mobile/viewmodel/NearbyHcf;", "attendanceResult", "Lkotlinx/coroutines/flow/StateFlow;", "getAttendanceResult", "()Lkotlinx/coroutines/flow/StateFlow;", "cooldownJob", "Lkotlinx/coroutines/Job;", "cooldownRemainingMs", "getCooldownRemainingMs", "currentLocation", "Landroid/location/Location;", "hcfSearchState", "getHcfSearchState", "locationState", "getLocationState", "selectedHcf", "getSelectedHcf", "canMarkAttendance", "", "formatCooldownTime", "ms", "getDeviceId", "initWithLocation", "", "latitude", "", "longitude", "markAttendance", "refreshLocation", "resetAttendanceResult", "scheduleSyncWork", "searchNearbyHcfs", "location", "selectHcf", "nearbyHcf", "startCooldownTimer", "durationMs", "Companion", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class AttendanceViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context appContext = null;
    @org.jetbrains.annotations.NotNull()
    private final com.smartcbwtf.mobile.utils.LocationHelper locationHelper = null;
    @org.jetbrains.annotations.NotNull()
    private final com.smartcbwtf.mobile.repository.HcfRepository hcfRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.smartcbwtf.mobile.repository.AttendanceRepository attendanceRepository = null;
    public static final double GEOFENCE_RADIUS_METERS = 50.0;
    public static final long COOLDOWN_DURATION_MS = 300000L;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.smartcbwtf.mobile.viewmodel.AttendanceLocationState> _locationState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.smartcbwtf.mobile.viewmodel.AttendanceLocationState> locationState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.smartcbwtf.mobile.viewmodel.HcfSearchState> _hcfSearchState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.smartcbwtf.mobile.viewmodel.HcfSearchState> hcfSearchState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.smartcbwtf.mobile.viewmodel.NearbyHcf> _selectedHcf = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.smartcbwtf.mobile.viewmodel.NearbyHcf> selectedHcf = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.smartcbwtf.mobile.viewmodel.AttendanceResult> _attendanceResult = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.smartcbwtf.mobile.viewmodel.AttendanceResult> attendanceResult = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Long> _cooldownRemainingMs = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Long> cooldownRemainingMs = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.String> _lastMarkedHcfId = null;
    @org.jetbrains.annotations.Nullable()
    private kotlinx.coroutines.Job cooldownJob;
    @org.jetbrains.annotations.Nullable()
    private android.location.Location currentLocation;
    @org.jetbrains.annotations.NotNull()
    public static final com.smartcbwtf.mobile.viewmodel.AttendanceViewModel.Companion Companion = null;
    
    @javax.inject.Inject()
    public AttendanceViewModel(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context appContext, @org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.utils.LocationHelper locationHelper, @org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.repository.HcfRepository hcfRepository, @org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.repository.AttendanceRepository attendanceRepository) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.smartcbwtf.mobile.viewmodel.AttendanceLocationState> getLocationState() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.smartcbwtf.mobile.viewmodel.HcfSearchState> getHcfSearchState() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.smartcbwtf.mobile.viewmodel.NearbyHcf> getSelectedHcf() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.smartcbwtf.mobile.viewmodel.AttendanceResult> getAttendanceResult() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Long> getCooldownRemainingMs() {
        return null;
    }
    
    public final void initWithLocation(double latitude, double longitude) {
    }
    
    public final void refreshLocation() {
    }
    
    private final void searchNearbyHcfs(android.location.Location location) {
    }
    
    public final void selectHcf(@org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.viewmodel.NearbyHcf nearbyHcf) {
    }
    
    public final boolean canMarkAttendance() {
        return false;
    }
    
    public final void markAttendance() {
    }
    
    private final void scheduleSyncWork() {
    }
    
    private final void startCooldownTimer(long durationMs) {
    }
    
    public final void resetAttendanceResult() {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String formatCooldownTime(long ms) {
        return null;
    }
    
    @kotlin.Suppress(names = {"HardwareIds"})
    private final java.lang.String getDeviceId() {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u0006\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0007"}, d2 = {"Lcom/smartcbwtf/mobile/viewmodel/AttendanceViewModel$Companion;", "", "()V", "COOLDOWN_DURATION_MS", "", "GEOFENCE_RADIUS_METERS", "", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}