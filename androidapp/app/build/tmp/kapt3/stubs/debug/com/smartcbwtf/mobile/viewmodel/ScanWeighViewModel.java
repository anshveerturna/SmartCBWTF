package com.smartcbwtf.mobile.viewmodel;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000p\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u000b\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u000f\n\u0002\u0010\u0006\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0012\b\u0007\u0018\u00002\u00020\u0001B\u001f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\u0006\u00104\u001a\u00020\u0016J\u0006\u00105\u001a\u000206J\u0006\u00107\u001a\u000206J\u000e\u00108\u001a\u0002062\u0006\u00109\u001a\u00020\"J\u0006\u0010:\u001a\u000206J\u0010\u0010;\u001a\u00020\u00162\u0006\u0010<\u001a\u00020\u000bH\u0002J\u000e\u0010=\u001a\u0002062\u0006\u0010<\u001a\u00020\u000bJ\u0006\u0010>\u001a\u000206J\u0006\u0010?\u001a\u000206J\u0006\u0010@\u001a\u000206J\u000e\u0010A\u001a\u000206H\u0086@\u00a2\u0006\u0002\u0010BJ\u0006\u0010C\u001a\u000206J\u0016\u0010D\u001a\u0002062\u0006\u0010E\u001a\u00020\u000b2\u0006\u0010F\u001a\u00020\u000bJ\u0010\u0010G\u001a\u0002062\b\b\u0002\u0010F\u001a\u00020\u000bR\u0016\u0010\t\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u000b0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\f\u001a\b\u0012\u0004\u0012\u00020\r0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u000e\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u000b0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u000f\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u000b0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00110\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00130\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00160\u0015\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u0018R\u0017\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\u00160\u0015\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001a\u0010\u0018R\u0017\u0010\u001b\u001a\b\u0012\u0004\u0012\u00020\u001d0\u001c\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001e\u0010\u001fR\u001d\u0010 \u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\"0!0\u001c\u00a2\u0006\b\n\u0000\u001a\u0004\b#\u0010\u001fR\u0019\u0010$\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u000b0\u001c\u00a2\u0006\b\n\u0000\u001a\u0004\b%\u0010\u001fR\u0017\u0010&\u001a\b\u0012\u0004\u0012\u00020\u00160\u0015\u00a2\u0006\b\n\u0000\u001a\u0004\b&\u0010\u0018R\u0017\u0010\'\u001a\b\u0012\u0004\u0012\u00020\r0\u001c\u00a2\u0006\b\n\u0000\u001a\u0004\b(\u0010\u001fR\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0019\u0010)\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u000b0\u001c\u00a2\u0006\b\n\u0000\u001a\u0004\b*\u0010\u001fR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0019\u0010+\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u000b0\u001c\u00a2\u0006\b\n\u0000\u001a\u0004\b,\u0010\u001fR\u0017\u0010-\u001a\b\u0012\u0004\u0012\u00020\u00110\u001c\u00a2\u0006\b\n\u0000\u001a\u0004\b.\u0010\u001fR\u0017\u0010/\u001a\b\u0012\u0004\u0012\u00020\u00130\u001c\u00a2\u0006\b\n\u0000\u001a\u0004\b0\u0010\u001fR\u0019\u00101\u001a\n\u0012\u0006\u0012\u0004\u0018\u0001020\u001c\u00a2\u0006\b\n\u0000\u001a\u0004\b3\u0010\u001f\u00a8\u0006H"}, d2 = {"Lcom/smartcbwtf/mobile/viewmodel/ScanWeighViewModel;", "Landroidx/lifecycle/ViewModel;", "scaleService", "Lcom/smartcbwtf/mobile/bluetooth/ScaleService;", "bagEventRepository", "Lcom/smartcbwtf/mobile/repository/BagEventRepository;", "locationHelper", "Lcom/smartcbwtf/mobile/utils/LocationHelper;", "(Lcom/smartcbwtf/mobile/bluetooth/ScaleService;Lcom/smartcbwtf/mobile/repository/BagEventRepository;Lcom/smartcbwtf/mobile/utils/LocationHelper;)V", "_error", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "_location", "Lcom/smartcbwtf/mobile/viewmodel/LocationState;", "_qrError", "_scannedQr", "_sessionState", "Lcom/smartcbwtf/mobile/viewmodel/PickupSessionState;", "_submissionState", "Lcom/smartcbwtf/mobile/viewmodel/SubmissionState;", "canAddBag", "Lkotlinx/coroutines/flow/Flow;", "", "getCanAddBag", "()Lkotlinx/coroutines/flow/Flow;", "canSubmitAll", "getCanSubmitAll", "connectionState", "Lkotlinx/coroutines/flow/StateFlow;", "Lcom/smartcbwtf/mobile/bluetooth/ConnectionState;", "getConnectionState", "()Lkotlinx/coroutines/flow/StateFlow;", "discoveredDevices", "", "Landroid/bluetooth/BluetoothDevice;", "getDiscoveredDevices", "error", "getError", "isSubmitEnabled", "location", "getLocation", "qrError", "getQrError", "scannedQr", "getScannedQr", "sessionState", "getSessionState", "submissionState", "getSubmissionState", "weight", "", "getWeight", "addBag", "clearSession", "", "connectScale", "connectToDevice", "device", "disconnectScale", "isValidQr", "qr", "onQrScanned", "refreshLocation", "resetForNextScan", "resetSubmissionState", "simulateWeight", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "stopScanning", "submit", "hcfId", "eventType", "submitAll", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class ScanWeighViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.smartcbwtf.mobile.bluetooth.ScaleService scaleService = null;
    @org.jetbrains.annotations.NotNull()
    private final com.smartcbwtf.mobile.repository.BagEventRepository bagEventRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.smartcbwtf.mobile.utils.LocationHelper locationHelper = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Double> weight = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.smartcbwtf.mobile.bluetooth.ConnectionState> connectionState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<android.bluetooth.BluetoothDevice>> discoveredDevices = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.String> _scannedQr = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.String> scannedQr = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.String> _qrError = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.String> qrError = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.smartcbwtf.mobile.viewmodel.SubmissionState> _submissionState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.smartcbwtf.mobile.viewmodel.SubmissionState> submissionState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.String> _error = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.String> error = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.smartcbwtf.mobile.viewmodel.LocationState> _location = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.smartcbwtf.mobile.viewmodel.LocationState> location = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.smartcbwtf.mobile.viewmodel.PickupSessionState> _sessionState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.smartcbwtf.mobile.viewmodel.PickupSessionState> sessionState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.Flow<java.lang.Boolean> canAddBag = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.Flow<java.lang.Boolean> canSubmitAll = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.Flow<java.lang.Boolean> isSubmitEnabled = null;
    
    @javax.inject.Inject()
    public ScanWeighViewModel(@org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.bluetooth.ScaleService scaleService, @org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.repository.BagEventRepository bagEventRepository, @org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.utils.LocationHelper locationHelper) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Double> getWeight() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.smartcbwtf.mobile.bluetooth.ConnectionState> getConnectionState() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.List<android.bluetooth.BluetoothDevice>> getDiscoveredDevices() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.String> getScannedQr() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.String> getQrError() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.smartcbwtf.mobile.viewmodel.SubmissionState> getSubmissionState() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.String> getError() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.smartcbwtf.mobile.viewmodel.LocationState> getLocation() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.smartcbwtf.mobile.viewmodel.PickupSessionState> getSessionState() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<java.lang.Boolean> getCanAddBag() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<java.lang.Boolean> getCanSubmitAll() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<java.lang.Boolean> isSubmitEnabled() {
        return null;
    }
    
    public final void connectScale() {
    }
    
    public final void connectToDevice(@org.jetbrains.annotations.NotNull()
    android.bluetooth.BluetoothDevice device) {
    }
    
    public final void stopScanning() {
    }
    
    public final void disconnectScale() {
    }
    
    public final void onQrScanned(@org.jetbrains.annotations.NotNull()
    java.lang.String qr) {
    }
    
    /**
     * Add current bag to the session list
     */
    public final boolean addBag() {
        return false;
    }
    
    /**
     * Submit all bags in the session to the repository
     */
    public final void submitAll(@org.jetbrains.annotations.NotNull()
    java.lang.String eventType) {
    }
    
    /**
     * Clear the current session
     */
    public final void clearSession() {
    }
    
    public final void submit(@org.jetbrains.annotations.NotNull()
    java.lang.String hcfId, @org.jetbrains.annotations.NotNull()
    java.lang.String eventType) {
    }
    
    public final void resetSubmissionState() {
    }
    
    public final void resetForNextScan() {
    }
    
    public final void refreshLocation() {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object simulateWeight(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final boolean isValidQr(java.lang.String qr) {
        return false;
    }
}