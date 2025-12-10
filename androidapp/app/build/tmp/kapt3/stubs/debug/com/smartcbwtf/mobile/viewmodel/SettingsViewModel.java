package com.smartcbwtf.mobile.viewmodel;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\u0010\b\n\u0002\b\b\b\u0007\u0018\u00002\u00020\u0001B\u001f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bR\u0014\u0010\t\u001a\u00020\nX\u0086D\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\fR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0011\u0010\r\u001a\u00020\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\fR\u0017\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00110\u0010\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u0013R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0019\u0010\u0014\u001a\n \u0015*\u0004\u0018\u00010\n0\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0016\u0010\fR\u0017\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\n0\u0010\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u0013\u00a8\u0006\u0019"}, d2 = {"Lcom/smartcbwtf/mobile/viewmodel/SettingsViewModel;", "Landroidx/lifecycle/ViewModel;", "authRepository", "Lcom/smartcbwtf/mobile/repository/AuthRepository;", "bagEventRepository", "Lcom/smartcbwtf/mobile/repository/BagEventRepository;", "scaleService", "Lcom/smartcbwtf/mobile/bluetooth/ScaleService;", "(Lcom/smartcbwtf/mobile/repository/AuthRepository;Lcom/smartcbwtf/mobile/repository/BagEventRepository;Lcom/smartcbwtf/mobile/bluetooth/ScaleService;)V", "appVersion", "", "getAppVersion", "()Ljava/lang/String;", "baseUrl", "getBaseUrl", "pendingCount", "Lkotlinx/coroutines/flow/StateFlow;", "", "getPendingCount", "()Lkotlinx/coroutines/flow/StateFlow;", "scaleServiceType", "kotlin.jvm.PlatformType", "getScaleServiceType", "username", "getUsername", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class SettingsViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.smartcbwtf.mobile.repository.AuthRepository authRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.smartcbwtf.mobile.bluetooth.ScaleService scaleService = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String appVersion = "1.0 (1)";
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String baseUrl = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.String> username = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Integer> pendingCount = null;
    private final java.lang.String scaleServiceType = null;
    
    @javax.inject.Inject()
    public SettingsViewModel(@org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.repository.AuthRepository authRepository, @org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.repository.BagEventRepository bagEventRepository, @org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.bluetooth.ScaleService scaleService) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getAppVersion() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getBaseUrl() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.String> getUsername() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Integer> getPendingCount() {
        return null;
    }
    
    public final java.lang.String getScaleServiceType() {
        return null;
    }
}