package com.smartcbwtf.mobile.ui;

/**
 * Fragment displaying the staff member's assigned route and waypoints.
 */
@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000@\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\b\u0007\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u000e\u001a\u00020\u000fH\u0002J\b\u0010\u0010\u001a\u00020\u000fH\u0016J\u001a\u0010\u0011\u001a\u00020\u000f2\u0006\u0010\u0012\u001a\u00020\u00132\b\u0010\u0014\u001a\u0004\u0018\u00010\u0015H\u0016J\u0010\u0010\u0016\u001a\u00020\u000f2\u0006\u0010\u0017\u001a\u00020\u0018H\u0002J\b\u0010\u0019\u001a\u00020\u000fH\u0002J\b\u0010\u001a\u001a\u00020\u000fH\u0002J\u0010\u0010\u001b\u001a\u00020\u000f2\u0006\u0010\u001c\u001a\u00020\u001dH\u0002R\u0010\u0010\u0003\u001a\u0004\u0018\u00010\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0005\u001a\u00020\u00048BX\u0082\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0006\u0010\u0007R\u001b\u0010\b\u001a\u00020\t8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\f\u0010\r\u001a\u0004\b\n\u0010\u000b\u00a8\u0006\u001e"}, d2 = {"Lcom/smartcbwtf/mobile/ui/MyRouteFragment;", "Landroidx/fragment/app/Fragment;", "()V", "_binding", "Lcom/smartcbwtf/mobile/databinding/FragmentMyRouteBinding;", "binding", "getBinding", "()Lcom/smartcbwtf/mobile/databinding/FragmentMyRouteBinding;", "viewModel", "Lcom/smartcbwtf/mobile/viewmodel/MyRouteViewModel;", "getViewModel", "()Lcom/smartcbwtf/mobile/viewmodel/MyRouteViewModel;", "viewModel$delegate", "Lkotlin/Lazy;", "observeState", "", "onDestroyView", "onViewCreated", "view", "Landroid/view/View;", "savedInstanceState", "Landroid/os/Bundle;", "showError", "message", "", "showLoading", "showNoRoute", "showRoute", "route", "Lcom/smartcbwtf/mobile/network/model/MobileRouteResponse;", "app_debug"})
public final class MyRouteFragment extends androidx.fragment.app.Fragment {
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy viewModel$delegate = null;
    @org.jetbrains.annotations.Nullable()
    private com.smartcbwtf.mobile.databinding.FragmentMyRouteBinding _binding;
    
    public MyRouteFragment() {
        super();
    }
    
    private final com.smartcbwtf.mobile.viewmodel.MyRouteViewModel getViewModel() {
        return null;
    }
    
    private final com.smartcbwtf.mobile.databinding.FragmentMyRouteBinding getBinding() {
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
    
    private final void observeState() {
    }
    
    private final void showLoading() {
    }
    
    private final void showRoute(com.smartcbwtf.mobile.network.model.MobileRouteResponse route) {
    }
    
    private final void showNoRoute() {
    }
    
    private final void showError(java.lang.String message) {
    }
}