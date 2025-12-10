package com.smartcbwtf.mobile.viewmodel;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00008\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\b\u0007\u0018\u00002\u00020\u0001B\u001f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\u0006\u0010\u0011\u001a\u00020\u0012R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u000b0\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\rR\u0017\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u000f0\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\r\u00a8\u0006\u0013"}, d2 = {"Lcom/smartcbwtf/mobile/viewmodel/HomeViewModel;", "Landroidx/lifecycle/ViewModel;", "authRepository", "Lcom/smartcbwtf/mobile/repository/AuthRepository;", "bagEventRepository", "Lcom/smartcbwtf/mobile/repository/BagEventRepository;", "workManager", "Landroidx/work/WorkManager;", "(Lcom/smartcbwtf/mobile/repository/AuthRepository;Lcom/smartcbwtf/mobile/repository/BagEventRepository;Landroidx/work/WorkManager;)V", "lastSyncFailed", "Lkotlinx/coroutines/flow/StateFlow;", "", "getLastSyncFailed", "()Lkotlinx/coroutines/flow/StateFlow;", "pendingCount", "", "getPendingCount", "logout", "", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class HomeViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.smartcbwtf.mobile.repository.AuthRepository authRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Integer> pendingCount = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> lastSyncFailed = null;
    
    @javax.inject.Inject()
    public HomeViewModel(@org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.repository.AuthRepository authRepository, @org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.repository.BagEventRepository bagEventRepository, @org.jetbrains.annotations.NotNull()
    androidx.work.WorkManager workManager) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Integer> getPendingCount() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> getLastSyncFailed() {
        return null;
    }
    
    public final void logout() {
    }
}