package com.smartcbwtf.mobile.viewmodel;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000^\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\u0010\u000b\n\u0002\b\n\n\u0002\u0010\u0006\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\f\b\u0007\u0018\u00002\u00020\u0001B\u001f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\u0006\u0010(\u001a\u00020)J\u0006\u0010*\u001a\u00020)J\u0010\u0010+\u001a\u00020\u001b2\u0006\u0010,\u001a\u00020\u000bH\u0002J\u000e\u0010-\u001a\u00020)2\u0006\u0010,\u001a\u00020\u000bJ\u0006\u0010.\u001a\u00020)J\u0006\u0010/\u001a\u00020)J\u000e\u00100\u001a\u00020)H\u0086@\u00a2\u0006\u0002\u00101J\u0016\u00102\u001a\u00020)2\u0006\u00103\u001a\u00020\u000b2\u0006\u00104\u001a\u00020\u000bR\u0016\u0010\t\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u000b0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\f\u001a\b\u0012\u0004\u0012\u00020\r0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u000e\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u000b0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u000f\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u000b0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00110\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00140\u0013\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0015\u0010\u0016R\u0019\u0010\u0017\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u000b0\u0013\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u0016R\u0017\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\u001b0\u001a\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0019\u0010\u001cR\u0017\u0010\u001d\u001a\b\u0012\u0004\u0012\u00020\r0\u0013\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001e\u0010\u0016R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0019\u0010\u001f\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u000b0\u0013\u00a2\u0006\b\n\u0000\u001a\u0004\b \u0010\u0016R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0019\u0010!\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u000b0\u0013\u00a2\u0006\b\n\u0000\u001a\u0004\b\"\u0010\u0016R\u0017\u0010#\u001a\b\u0012\u0004\u0012\u00020\u00110\u0013\u00a2\u0006\b\n\u0000\u001a\u0004\b$\u0010\u0016R\u0019\u0010%\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010&0\u0013\u00a2\u0006\b\n\u0000\u001a\u0004\b\'\u0010\u0016\u00a8\u00065"}, d2 = {"Lcom/smartcbwtf/mobile/viewmodel/ScanWeighViewModel;", "Landroidx/lifecycle/ViewModel;", "scaleService", "Lcom/smartcbwtf/mobile/bluetooth/ScaleService;", "bagEventRepository", "Lcom/smartcbwtf/mobile/repository/BagEventRepository;", "locationHelper", "Lcom/smartcbwtf/mobile/utils/LocationHelper;", "(Lcom/smartcbwtf/mobile/bluetooth/ScaleService;Lcom/smartcbwtf/mobile/repository/BagEventRepository;Lcom/smartcbwtf/mobile/utils/LocationHelper;)V", "_error", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "_location", "Lcom/smartcbwtf/mobile/viewmodel/LocationState;", "_qrError", "_scannedQr", "_submissionState", "Lcom/smartcbwtf/mobile/viewmodel/SubmissionState;", "connectionState", "Lkotlinx/coroutines/flow/StateFlow;", "Lcom/smartcbwtf/mobile/bluetooth/ConnectionState;", "getConnectionState", "()Lkotlinx/coroutines/flow/StateFlow;", "error", "getError", "isSubmitEnabled", "Lkotlinx/coroutines/flow/Flow;", "", "()Lkotlinx/coroutines/flow/Flow;", "location", "getLocation", "qrError", "getQrError", "scannedQr", "getScannedQr", "submissionState", "getSubmissionState", "weight", "", "getWeight", "connectScale", "", "disconnectScale", "isValidQr", "qr", "onQrScanned", "refreshLocation", "resetSubmissionState", "simulateWeight", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "submit", "hcfId", "eventType", "app_debug"})
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
    public final kotlinx.coroutines.flow.Flow<java.lang.Boolean> isSubmitEnabled() {
        return null;
    }
    
    public final void connectScale() {
    }
    
    public final void disconnectScale() {
    }
    
    public final void onQrScanned(@org.jetbrains.annotations.NotNull()
    java.lang.String qr) {
    }
    
    public final void submit(@org.jetbrains.annotations.NotNull()
    java.lang.String hcfId, @org.jetbrains.annotations.NotNull()
    java.lang.String eventType) {
    }
    
    public final void resetSubmissionState() {
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