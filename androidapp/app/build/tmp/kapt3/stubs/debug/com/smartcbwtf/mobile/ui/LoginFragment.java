package com.smartcbwtf.mobile.ui;

@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000J\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0004\b\u0007\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u001a\u001a\u00020\u001b2\u0006\u0010\u001c\u001a\u00020\u001dH\u0002J\u0010\u0010\u001e\u001a\u00020\u001b2\u0006\u0010\u001f\u001a\u00020 H\u0002J\b\u0010!\u001a\u00020\u001bH\u0002J\b\u0010\"\u001a\u00020\u001bH\u0016J\u001a\u0010#\u001a\u00020\u001b2\u0006\u0010\u001f\u001a\u00020 2\b\u0010$\u001a\u0004\u0018\u00010%H\u0016J\u0010\u0010&\u001a\u00020\u001b2\u0006\u0010\'\u001a\u00020 H\u0002J\b\u0010(\u001a\u00020\u001bH\u0002R\u0010\u0010\u0003\u001a\u0004\u0018\u00010\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0005\u001a\u00020\u00048BX\u0082\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0006\u0010\u0007R\u001e\u0010\b\u001a\u00020\t8\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\n\u0010\u000b\"\u0004\b\f\u0010\rR\u001e\u0010\u000e\u001a\u00020\u000f8\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0010\u0010\u0011\"\u0004\b\u0012\u0010\u0013R\u001b\u0010\u0014\u001a\u00020\u00158BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0018\u0010\u0019\u001a\u0004\b\u0016\u0010\u0017\u00a8\u0006)"}, d2 = {"Lcom/smartcbwtf/mobile/ui/LoginFragment;", "Landroidx/fragment/app/Fragment;", "()V", "_binding", "Lcom/smartcbwtf/mobile/databinding/FragmentLoginBinding;", "binding", "getBinding", "()Lcom/smartcbwtf/mobile/databinding/FragmentLoginBinding;", "locationRepository", "Lcom/smartcbwtf/mobile/repository/LocationRepository;", "getLocationRepository", "()Lcom/smartcbwtf/mobile/repository/LocationRepository;", "setLocationRepository", "(Lcom/smartcbwtf/mobile/repository/LocationRepository;)V", "sharedPreferences", "Landroid/content/SharedPreferences;", "getSharedPreferences", "()Landroid/content/SharedPreferences;", "setSharedPreferences", "(Landroid/content/SharedPreferences;)V", "viewModel", "Lcom/smartcbwtf/mobile/viewmodel/AuthViewModel;", "getViewModel", "()Lcom/smartcbwtf/mobile/viewmodel/AuthViewModel;", "viewModel$delegate", "Lkotlin/Lazy;", "handleLoginState", "", "state", "Lcom/smartcbwtf/mobile/viewmodel/LoginState;", "hideKeyboard", "view", "Landroid/view/View;", "observeViewModel", "onDestroyView", "onViewCreated", "savedInstanceState", "Landroid/os/Bundle;", "setupHideKeyboardOnTouch", "root", "setupListeners", "app_debug"})
public final class LoginFragment extends androidx.fragment.app.Fragment {
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy viewModel$delegate = null;
    @org.jetbrains.annotations.Nullable()
    private com.smartcbwtf.mobile.databinding.FragmentLoginBinding _binding;
    @javax.inject.Inject()
    public android.content.SharedPreferences sharedPreferences;
    @javax.inject.Inject()
    public com.smartcbwtf.mobile.repository.LocationRepository locationRepository;
    
    public LoginFragment() {
        super();
    }
    
    private final com.smartcbwtf.mobile.viewmodel.AuthViewModel getViewModel() {
        return null;
    }
    
    private final com.smartcbwtf.mobile.databinding.FragmentLoginBinding getBinding() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final android.content.SharedPreferences getSharedPreferences() {
        return null;
    }
    
    public final void setSharedPreferences(@org.jetbrains.annotations.NotNull()
    android.content.SharedPreferences p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.smartcbwtf.mobile.repository.LocationRepository getLocationRepository() {
        return null;
    }
    
    public final void setLocationRepository(@org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.repository.LocationRepository p0) {
    }
    
    @java.lang.Override()
    public void onViewCreated(@org.jetbrains.annotations.NotNull()
    android.view.View view, @org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    private final void setupHideKeyboardOnTouch(android.view.View root) {
    }
    
    private final void hideKeyboard(android.view.View view) {
    }
    
    private final void setupListeners() {
    }
    
    private final void observeViewModel() {
    }
    
    private final void handleLoginState(com.smartcbwtf.mobile.viewmodel.LoginState state) {
    }
    
    @java.lang.Override()
    public void onDestroyView() {
    }
}