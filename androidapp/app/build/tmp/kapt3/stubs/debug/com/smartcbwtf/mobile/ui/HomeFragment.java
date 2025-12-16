package com.smartcbwtf.mobile.ui;

@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000P\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\u0010\u0011\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u0007\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u001d\u001a\u00020\u001eH\u0002J\b\u0010\u001f\u001a\u00020\u001eH\u0002J\b\u0010 \u001a\u00020\u001eH\u0002J\b\u0010!\u001a\u00020\u001eH\u0002J\b\u0010\"\u001a\u00020\u001eH\u0016J\u001a\u0010#\u001a\u00020\u001e2\u0006\u0010$\u001a\u00020%2\b\u0010&\u001a\u0004\u0018\u00010\'H\u0016J\b\u0010(\u001a\u00020\u001eH\u0002J\b\u0010)\u001a\u00020\u001eH\u0002R\u0010\u0010\u0003\u001a\u0004\u0018\u00010\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0005\u001a\u00020\u00048BX\u0082\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0006\u0010\u0007R\u001b\u0010\b\u001a\u00020\t8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\f\u0010\r\u001a\u0004\b\n\u0010\u000bR\u001e\u0010\u000e\u001a\u00020\u000f8\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0010\u0010\u0011\"\u0004\b\u0012\u0010\u0013R\u001a\u0010\u0014\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00170\u00160\u0015X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001b\u0010\u0018\u001a\u00020\u00198BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u001c\u0010\r\u001a\u0004\b\u001a\u0010\u001b\u00a8\u0006*"}, d2 = {"Lcom/smartcbwtf/mobile/ui/HomeFragment;", "Landroidx/fragment/app/Fragment;", "()V", "_binding", "Lcom/smartcbwtf/mobile/databinding/FragmentHomeBinding;", "binding", "getBinding", "()Lcom/smartcbwtf/mobile/databinding/FragmentHomeBinding;", "homeViewModel", "Lcom/smartcbwtf/mobile/viewmodel/HomeViewModel;", "getHomeViewModel", "()Lcom/smartcbwtf/mobile/viewmodel/HomeViewModel;", "homeViewModel$delegate", "Lkotlin/Lazy;", "locationHelper", "Lcom/smartcbwtf/mobile/utils/LocationHelper;", "getLocationHelper", "()Lcom/smartcbwtf/mobile/utils/LocationHelper;", "setLocationHelper", "(Lcom/smartcbwtf/mobile/utils/LocationHelper;)V", "locationPermissionLauncher", "Landroidx/activity/result/ActivityResultLauncher;", "", "", "viewModel", "Lcom/smartcbwtf/mobile/viewmodel/AuthViewModel;", "getViewModel", "()Lcom/smartcbwtf/mobile/viewmodel/AuthViewModel;", "viewModel$delegate", "animateEntry", "", "bindStatus", "captureLocationAndNavigate", "checkLocationPermissionAndMarkAttendance", "onDestroyView", "onViewCreated", "view", "Landroid/view/View;", "savedInstanceState", "Landroid/os/Bundle;", "setupActions", "setupProfileMenu", "app_debug"})
public final class HomeFragment extends androidx.fragment.app.Fragment {
    @javax.inject.Inject()
    public com.smartcbwtf.mobile.utils.LocationHelper locationHelper;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy viewModel$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy homeViewModel$delegate = null;
    @org.jetbrains.annotations.Nullable()
    private com.smartcbwtf.mobile.databinding.FragmentHomeBinding _binding;
    @org.jetbrains.annotations.NotNull()
    private final androidx.activity.result.ActivityResultLauncher<java.lang.String[]> locationPermissionLauncher = null;
    
    public HomeFragment() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.smartcbwtf.mobile.utils.LocationHelper getLocationHelper() {
        return null;
    }
    
    public final void setLocationHelper(@org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.utils.LocationHelper p0) {
    }
    
    private final com.smartcbwtf.mobile.viewmodel.AuthViewModel getViewModel() {
        return null;
    }
    
    private final com.smartcbwtf.mobile.viewmodel.HomeViewModel getHomeViewModel() {
        return null;
    }
    
    private final com.smartcbwtf.mobile.databinding.FragmentHomeBinding getBinding() {
        return null;
    }
    
    @java.lang.Override()
    public void onViewCreated(@org.jetbrains.annotations.NotNull()
    android.view.View view, @org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    @java.lang.Override()
    public void onDestroyView() {
    }
    
    private final void setupActions() {
    }
    
    private final void checkLocationPermissionAndMarkAttendance() {
    }
    
    private final void captureLocationAndNavigate() {
    }
    
    private final void setupProfileMenu() {
    }
    
    private final void bindStatus() {
    }
    
    private final void animateEntry() {
    }
}