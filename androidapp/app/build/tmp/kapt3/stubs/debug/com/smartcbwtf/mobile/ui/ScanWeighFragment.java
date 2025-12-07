package com.smartcbwtf.mobile.ui;

@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000V\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\b\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0011\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u0007\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010!\u001a\u00020\"H\u0016J\u001a\u0010#\u001a\u00020\"2\u0006\u0010$\u001a\u00020%2\b\u0010&\u001a\u0004\u0018\u00010\'H\u0016J\b\u0010(\u001a\u00020\"H\u0002R\u0010\u0010\u0003\u001a\u0004\u0018\u00010\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001b\u0010\u0005\u001a\u00020\u00068BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\t\u0010\n\u001a\u0004\b\u0007\u0010\bR\u0014\u0010\u000b\u001a\u00020\u00048BX\u0082\u0004\u00a2\u0006\u0006\u001a\u0004\b\f\u0010\rR\u001e\u0010\u000e\u001a\u00020\u000f8\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0010\u0010\u0011\"\u0004\b\u0012\u0010\u0013R\u001c\u0010\u0014\u001a\u0010\u0012\f\u0012\n \u0017*\u0004\u0018\u00010\u00160\u00160\u0015X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0018\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u001a0\u00190\u0015X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001b\u0010\u001b\u001a\u00020\u001c8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u001f\u0010 \u001a\u0004\b\u001d\u0010\u001e\u00a8\u0006)"}, d2 = {"Lcom/smartcbwtf/mobile/ui/ScanWeighFragment;", "Landroidx/fragment/app/Fragment;", "()V", "_binding", "Lcom/smartcbwtf/mobile/databinding/FragmentScanWeighBinding;", "args", "Lcom/smartcbwtf/mobile/ui/ScanWeighFragmentArgs;", "getArgs", "()Lcom/smartcbwtf/mobile/ui/ScanWeighFragmentArgs;", "args$delegate", "Landroidx/navigation/NavArgsLazy;", "binding", "getBinding", "()Lcom/smartcbwtf/mobile/databinding/FragmentScanWeighBinding;", "permissionHelper", "Lcom/smartcbwtf/mobile/utils/PermissionHelper;", "getPermissionHelper", "()Lcom/smartcbwtf/mobile/utils/PermissionHelper;", "setPermissionHelper", "(Lcom/smartcbwtf/mobile/utils/PermissionHelper;)V", "qrScannerLauncher", "Landroidx/activity/result/ActivityResultLauncher;", "Lcom/journeyapps/barcodescanner/ScanOptions;", "kotlin.jvm.PlatformType", "requestPermissionLauncher", "", "", "viewModel", "Lcom/smartcbwtf/mobile/viewmodel/ScanWeighViewModel;", "getViewModel", "()Lcom/smartcbwtf/mobile/viewmodel/ScanWeighViewModel;", "viewModel$delegate", "Lkotlin/Lazy;", "onDestroyView", "", "onViewCreated", "view", "Landroid/view/View;", "savedInstanceState", "Landroid/os/Bundle;", "updateSubmitButton", "app_debug"})
public final class ScanWeighFragment extends androidx.fragment.app.Fragment {
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy viewModel$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.navigation.NavArgsLazy args$delegate = null;
    @org.jetbrains.annotations.Nullable()
    private com.smartcbwtf.mobile.databinding.FragmentScanWeighBinding _binding;
    @javax.inject.Inject()
    public com.smartcbwtf.mobile.utils.PermissionHelper permissionHelper;
    @org.jetbrains.annotations.NotNull()
    private final androidx.activity.result.ActivityResultLauncher<java.lang.String[]> requestPermissionLauncher = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.activity.result.ActivityResultLauncher<com.journeyapps.barcodescanner.ScanOptions> qrScannerLauncher = null;
    
    public ScanWeighFragment() {
        super();
    }
    
    private final com.smartcbwtf.mobile.viewmodel.ScanWeighViewModel getViewModel() {
        return null;
    }
    
    private final com.smartcbwtf.mobile.ui.ScanWeighFragmentArgs getArgs() {
        return null;
    }
    
    private final com.smartcbwtf.mobile.databinding.FragmentScanWeighBinding getBinding() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.smartcbwtf.mobile.utils.PermissionHelper getPermissionHelper() {
        return null;
    }
    
    public final void setPermissionHelper(@org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.utils.PermissionHelper p0) {
    }
    
    @java.lang.Override()
    public void onViewCreated(@org.jetbrains.annotations.NotNull()
    android.view.View view, @org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    private final void updateSubmitButton() {
    }
    
    @java.lang.Override()
    public void onDestroyView() {
    }
}