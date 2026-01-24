package com.smartcbwtf.mobile.viewmodel;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u00a8\u0001\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u000b\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0006\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u001b\n\u0002\u0010\u0002\n\u0002\b\t\n\u0002\u0018\u0002\n\u0002\b\u0011\n\u0002\u0010\b\n\u0002\b\u000f\b\u0007\u0018\u0000 y2\u00020\u0001:\u0001yB/\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u0012\u0006\u0010\n\u001a\u00020\u000b\u00a2\u0006\u0002\u0010\fJ\u000e\u0010N\u001a\u00020OH\u0082@\u00a2\u0006\u0002\u0010PJ\u0006\u0010Q\u001a\u00020\"J\u001d\u0010R\u001a\u00020\"2\u0006\u0010S\u001a\u00020\u000f2\b\u0010T\u001a\u0004\u0018\u00010,\u00a2\u0006\u0002\u0010UJ\r\u0010V\u001a\u0004\u0018\u00010,\u00a2\u0006\u0002\u0010WJ\u0010\u0010X\u001a\u00020O2\u0006\u0010<\u001a\u00020YH\u0002J\u0006\u0010Z\u001a\u00020OJ\u0006\u0010[\u001a\u00020OJ\u0006\u0010\\\u001a\u00020OJ\u000e\u0010]\u001a\u00020O2\u0006\u0010^\u001a\u000203J\u0006\u0010_\u001a\u00020OJ\r\u0010`\u001a\u0004\u0018\u00010,\u00a2\u0006\u0002\u0010WJ\u0006\u0010a\u001a\u00020OJ\u0010\u0010b\u001a\u00020\"2\u0006\u0010c\u001a\u00020\u000fH\u0002J\u000e\u0010d\u001a\u00020OH\u0082@\u00a2\u0006\u0002\u0010PJ\u000e\u0010e\u001a\u00020O2\u0006\u0010c\u001a\u00020\u000fJ\u0012\u0010f\u001a\u00020\u000f2\b\u0010g\u001a\u0004\u0018\u00010\u000fH\u0002J\u0006\u0010h\u001a\u00020OJ\u000e\u0010i\u001a\u00020O2\u0006\u0010j\u001a\u00020kJ\u0006\u0010l\u001a\u00020OJ\u0006\u0010m\u001a\u00020OJ\u0006\u0010n\u001a\u00020OJ\u000e\u0010o\u001a\u00020O2\u0006\u0010p\u001a\u00020\u0017J\u000e\u0010q\u001a\u00020OH\u0086@\u00a2\u0006\u0002\u0010PJ\u0006\u0010r\u001a\u00020OJ\u0016\u0010s\u001a\u00020O2\u0006\u0010t\u001a\u00020\u000f2\u0006\u0010u\u001a\u00020\u000fJ\u0010\u0010v\u001a\u00020O2\b\b\u0002\u0010u\u001a\u00020\u000fJ\u000e\u0010w\u001a\u00020O2\u0006\u0010x\u001a\u00020\u000fR\u0016\u0010\r\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u000f0\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u0010\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00110\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00130\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00150\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u00170\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u0018\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u000f0\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u0019\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u000f0\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\u001b0\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u001c\u001a\b\u0012\u0004\u0012\u00020\u001d0\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u001e\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u001f0\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010 \u001a\b\u0012\u0004\u0012\u00020\"0!\u00a2\u0006\b\n\u0000\u001a\u0004\b#\u0010$R\u0017\u0010%\u001a\b\u0012\u0004\u0012\u00020\"0!\u00a2\u0006\b\n\u0000\u001a\u0004\b&\u0010$R\u0017\u0010\'\u001a\b\u0012\u0004\u0012\u00020\"0(\u00a2\u0006\b\n\u0000\u001a\u0004\b)\u0010*R\u0012\u0010+\u001a\u0004\u0018\u00010,X\u0082\u000e\u00a2\u0006\u0004\n\u0002\u0010-R\u0017\u0010.\u001a\b\u0012\u0004\u0012\u00020/0(\u00a2\u0006\b\n\u0000\u001a\u0004\b0\u0010*R\u001d\u00101\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u000203020(\u00a2\u0006\b\n\u0000\u001a\u0004\b4\u0010*R\u0019\u00105\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u000f0(\u00a2\u0006\b\n\u0000\u001a\u0004\b6\u0010*R\u0019\u00107\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00110(\u00a2\u0006\b\n\u0000\u001a\u0004\b8\u0010*R\u0017\u00109\u001a\b\u0012\u0004\u0012\u00020\u00130(\u00a2\u0006\b\n\u0000\u001a\u0004\b:\u0010*R\u0017\u0010;\u001a\b\u0012\u0004\u0012\u00020\"0!\u00a2\u0006\b\n\u0000\u001a\u0004\b;\u0010$R\u0017\u0010<\u001a\b\u0012\u0004\u0012\u00020\u00150(\u00a2\u0006\b\n\u0000\u001a\u0004\b=\u0010*R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010>\u001a\b\u0012\u0004\u0012\u00020\u00170(\u00a2\u0006\b\n\u0000\u001a\u0004\b?\u0010*R\u0019\u0010@\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u000f0(\u00a2\u0006\b\n\u0000\u001a\u0004\bA\u0010*R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0019\u0010B\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u000f0(\u00a2\u0006\b\n\u0000\u001a\u0004\bC\u0010*R\u0017\u0010D\u001a\b\u0012\u0004\u0012\u00020\"0!\u00a2\u0006\b\n\u0000\u001a\u0004\bE\u0010$R\u000e\u0010\n\u001a\u00020\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010F\u001a\b\u0012\u0004\u0012\u00020\u001b0(\u00a2\u0006\b\n\u0000\u001a\u0004\bG\u0010*R\u0017\u0010H\u001a\b\u0012\u0004\u0012\u00020\u001d0(\u00a2\u0006\b\n\u0000\u001a\u0004\bI\u0010*R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0019\u0010J\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u001f0(\u00a2\u0006\b\n\u0000\u001a\u0004\bK\u0010*R\u0019\u0010L\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010,0(\u00a2\u0006\b\n\u0000\u001a\u0004\bM\u0010*\u00a8\u0006z"}, d2 = {"Lcom/smartcbwtf/mobile/viewmodel/ScanWeighViewModel;", "Landroidx/lifecycle/ViewModel;", "scaleService", "Lcom/smartcbwtf/mobile/bluetooth/ScaleService;", "bagEventRepository", "Lcom/smartcbwtf/mobile/repository/BagEventRepository;", "locationHelper", "Lcom/smartcbwtf/mobile/utils/LocationHelper;", "verificationApi", "Lcom/smartcbwtf/mobile/network/api/VerificationApi;", "sessionManager", "Lcom/smartcbwtf/mobile/storage/SessionManager;", "(Lcom/smartcbwtf/mobile/bluetooth/ScaleService;Lcom/smartcbwtf/mobile/repository/BagEventRepository;Lcom/smartcbwtf/mobile/utils/LocationHelper;Lcom/smartcbwtf/mobile/network/api/VerificationApi;Lcom/smartcbwtf/mobile/storage/SessionManager;)V", "_error", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "_facilityInfo", "Lcom/smartcbwtf/mobile/model/FacilityInfo;", "_geofenceState", "Lcom/smartcbwtf/mobile/viewmodel/GeofenceState;", "_location", "Lcom/smartcbwtf/mobile/viewmodel/LocationState;", "_operationMode", "Lcom/smartcbwtf/mobile/model/OperationMode;", "_qrError", "_scannedQr", "_sessionState", "Lcom/smartcbwtf/mobile/viewmodel/PickupSessionState;", "_submissionState", "Lcom/smartcbwtf/mobile/viewmodel/SubmissionState;", "_verificationResult", "Lcom/smartcbwtf/mobile/viewmodel/VerificationResult;", "canAddBag", "Lkotlinx/coroutines/flow/Flow;", "", "getCanAddBag", "()Lkotlinx/coroutines/flow/Flow;", "canSubmitAll", "getCanSubmitAll", "canVerify", "Lkotlinx/coroutines/flow/StateFlow;", "getCanVerify", "()Lkotlinx/coroutines/flow/StateFlow;", "capturedWeightForScan", "", "Ljava/lang/Double;", "connectionState", "Lcom/smartcbwtf/mobile/bluetooth/ConnectionState;", "getConnectionState", "discoveredDevices", "", "Landroid/bluetooth/BluetoothDevice;", "getDiscoveredDevices", "error", "getError", "facilityInfo", "getFacilityInfo", "geofenceState", "getGeofenceState", "isSubmitEnabled", "location", "getLocation", "operationMode", "getOperationMode", "qrError", "getQrError", "scannedQr", "getScannedQr", "scannerEnabled", "getScannerEnabled", "sessionState", "getSessionState", "submissionState", "getSubmissionState", "verificationResult", "getVerificationResult", "weight", "getWeight", "acquireGpsForVerification", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "addBag", "addBagWithWeight", "qrCode", "capturedWeight", "(Ljava/lang/String;Ljava/lang/Double;)Z", "captureWeightForScanner", "()Ljava/lang/Double;", "checkGeofence", "Lcom/smartcbwtf/mobile/viewmodel/LocationState$Ready;", "clearSession", "clearVerificationResult", "connectScale", "connectToDevice", "device", "disconnectScale", "getCapturedWeight", "initializeVerificationMode", "isValidQr", "qr", "loadFacilityInfo", "onQrScanned", "parseErrorMessage", "errorBody", "refreshLocation", "removeBag", "index", "", "resetForNextScan", "resetSubmissionState", "retryGpsForVerification", "setOperationMode", "mode", "simulateWeight", "stopScanning", "submit", "hcfId", "eventType", "submitAll", "verifyBag", "deviceId", "Companion", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class ScanWeighViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.smartcbwtf.mobile.bluetooth.ScaleService scaleService = null;
    @org.jetbrains.annotations.NotNull()
    private final com.smartcbwtf.mobile.repository.BagEventRepository bagEventRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.smartcbwtf.mobile.utils.LocationHelper locationHelper = null;
    @org.jetbrains.annotations.NotNull()
    private final com.smartcbwtf.mobile.network.api.VerificationApi verificationApi = null;
    @org.jetbrains.annotations.NotNull()
    private final com.smartcbwtf.mobile.storage.SessionManager sessionManager = null;
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
    private final kotlinx.coroutines.flow.MutableStateFlow<com.smartcbwtf.mobile.model.OperationMode> _operationMode = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.smartcbwtf.mobile.model.OperationMode> operationMode = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.smartcbwtf.mobile.viewmodel.GeofenceState> _geofenceState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.smartcbwtf.mobile.viewmodel.GeofenceState> geofenceState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.smartcbwtf.mobile.model.FacilityInfo> _facilityInfo = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.smartcbwtf.mobile.model.FacilityInfo> facilityInfo = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.smartcbwtf.mobile.viewmodel.VerificationResult> _verificationResult = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.smartcbwtf.mobile.viewmodel.VerificationResult> verificationResult = null;
    @org.jetbrains.annotations.Nullable()
    private java.lang.Double capturedWeightForScan;
    public static final long GPS_TIMEOUT_MS = 8000L;
    public static final float MIN_GPS_ACCURACY_M = 50.0F;
    public static final int DEFAULT_GEOFENCE_RADIUS_M = 200;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.Flow<java.lang.Boolean> canAddBag = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.Flow<java.lang.Boolean> canSubmitAll = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> canVerify = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.Flow<java.lang.Boolean> scannerEnabled = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.Flow<java.lang.Boolean> isSubmitEnabled = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.smartcbwtf.mobile.viewmodel.ScanWeighViewModel.Companion Companion = null;
    
    @javax.inject.Inject()
    public ScanWeighViewModel(@org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.bluetooth.ScaleService scaleService, @org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.repository.BagEventRepository bagEventRepository, @org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.utils.LocationHelper locationHelper, @org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.network.api.VerificationApi verificationApi, @org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.storage.SessionManager sessionManager) {
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
    public final kotlinx.coroutines.flow.StateFlow<com.smartcbwtf.mobile.model.OperationMode> getOperationMode() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.smartcbwtf.mobile.viewmodel.GeofenceState> getGeofenceState() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.smartcbwtf.mobile.model.FacilityInfo> getFacilityInfo() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.smartcbwtf.mobile.viewmodel.VerificationResult> getVerificationResult() {
        return null;
    }
    
    /**
     * Capture current weight before opening QR scanner.
     * Call this right before navigating to scanner fragment.
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Double captureWeightForScanner() {
        return null;
    }
    
    /**
     * Get the weight that was captured before scanning.
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Double getCapturedWeight() {
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
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> getCanVerify() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<java.lang.Boolean> getScannerEnabled() {
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
     * Add bag with pre-captured weight.
     * Used when weight is captured at scan time before scale might disconnect.
     */
    public final boolean addBagWithWeight(@org.jetbrains.annotations.NotNull()
    java.lang.String qrCode, @org.jetbrains.annotations.Nullable()
    java.lang.Double capturedWeight) {
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
    
    /**
     * Remove a bag from the session at the given index.
     * This allows rescanning of that QR code.
     */
    public final void removeBag(int index) {
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
    
    /**
     * Set the operation mode (COLLECTION or VERIFY)
     */
    public final void setOperationMode(@org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.model.OperationMode mode) {
    }
    
    /**
     * Initialize verification mode - fetch facility info and check geofence
     */
    public final void initializeVerificationMode() {
    }
    
    private final java.lang.Object loadFacilityInfo(kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final java.lang.Object acquireGpsForVerification(kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final void checkGeofence(com.smartcbwtf.mobile.viewmodel.LocationState.Ready location) {
    }
    
    /**
     * Retry GPS acquisition for verification
     */
    public final void retryGpsForVerification() {
    }
    
    /**
     * Verify a single bag at CBWTF
     */
    public final void verifyBag(@org.jetbrains.annotations.NotNull()
    java.lang.String deviceId) {
    }
    
    private final java.lang.String parseErrorMessage(java.lang.String errorBody) {
        return null;
    }
    
    public final void clearVerificationResult() {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object simulateWeight(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final boolean isValidQr(java.lang.String qr) {
        return false;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001e\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u0007\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\t"}, d2 = {"Lcom/smartcbwtf/mobile/viewmodel/ScanWeighViewModel$Companion;", "", "()V", "DEFAULT_GEOFENCE_RADIUS_M", "", "GPS_TIMEOUT_MS", "", "MIN_GPS_ACCURACY_M", "", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}