package com.smartcbwtf.mobile.viewmodel;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000@\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0004\b\u0007\u0018\u00002\u00020\u0001B\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u000e\u0010\u0010\u001a\u00020\u00112\u0006\u0010\u0012\u001a\u00020\u0013J\u0016\u0010\u0014\u001a\u00020\u00112\u0006\u0010\u0015\u001a\u00020\u00162\u0006\u0010\u0017\u001a\u00020\u0016J\u0006\u0010\u0018\u001a\u00020\u0011J\b\u0010\u0019\u001a\u00020\u0011H\u0002R\u0014\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\b\u001a\b\u0012\u0004\u0012\u00020\t0\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u00070\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\rR\u0017\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\t0\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\r\u00a8\u0006\u001a"}, d2 = {"Lcom/smartcbwtf/mobile/viewmodel/AuthViewModel;", "Landroidx/lifecycle/ViewModel;", "authRepository", "Lcom/smartcbwtf/mobile/repository/AuthRepository;", "(Lcom/smartcbwtf/mobile/repository/AuthRepository;)V", "_authState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/smartcbwtf/mobile/viewmodel/AuthState;", "_loginState", "Lcom/smartcbwtf/mobile/viewmodel/LoginState;", "authState", "Lkotlinx/coroutines/flow/StateFlow;", "getAuthState", "()Lkotlinx/coroutines/flow/StateFlow;", "loginState", "getLoginState", "clearError", "", "error", "Lcom/smartcbwtf/mobile/viewmodel/LoginError;", "login", "username", "", "password", "logout", "observeAuth", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class AuthViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.smartcbwtf.mobile.repository.AuthRepository authRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.smartcbwtf.mobile.viewmodel.LoginState> _loginState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.smartcbwtf.mobile.viewmodel.LoginState> loginState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.smartcbwtf.mobile.viewmodel.AuthState> _authState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.smartcbwtf.mobile.viewmodel.AuthState> authState = null;
    
    @javax.inject.Inject()
    public AuthViewModel(@org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.repository.AuthRepository authRepository) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.smartcbwtf.mobile.viewmodel.LoginState> getLoginState() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.smartcbwtf.mobile.viewmodel.AuthState> getAuthState() {
        return null;
    }
    
    private final void observeAuth() {
    }
    
    public final void login(@org.jetbrains.annotations.NotNull()
    java.lang.String username, @org.jetbrains.annotations.NotNull()
    java.lang.String password) {
    }
    
    public final void logout() {
    }
    
    public final void clearError(@org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.viewmodel.LoginError error) {
    }
}