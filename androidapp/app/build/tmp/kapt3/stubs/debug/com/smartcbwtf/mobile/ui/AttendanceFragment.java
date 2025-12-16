package com.smartcbwtf.mobile.ui;

@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000b\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\b\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\t\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u0007\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u0016\u001a\u00020\u00172\u0006\u0010\u0018\u001a\u00020\u0019H\u0002J\b\u0010\u001a\u001a\u00020\u0017H\u0002J\b\u0010\u001b\u001a\u00020\u0017H\u0016J\u001a\u0010\u001c\u001a\u00020\u00172\u0006\u0010\u001d\u001a\u00020\u001e2\b\u0010\u001f\u001a\u0004\u0018\u00010 H\u0016J\b\u0010!\u001a\u00020\u0017H\u0002J\b\u0010\"\u001a\u00020\u0017H\u0002J\b\u0010#\u001a\u00020\u0017H\u0002J\u0010\u0010$\u001a\u00020\u00172\u0006\u0010%\u001a\u00020&H\u0002J\u0010\u0010\'\u001a\u00020\u00172\u0006\u0010(\u001a\u00020)H\u0002J\u0010\u0010*\u001a\u00020\u00172\u0006\u0010+\u001a\u00020,H\u0002J\u0010\u0010-\u001a\u00020\u00172\u0006\u0010+\u001a\u00020.H\u0002J\b\u0010/\u001a\u00020\u0017H\u0002R\u0010\u0010\u0003\u001a\u0004\u0018\u00010\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001b\u0010\u0005\u001a\u00020\u00068BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\t\u0010\n\u001a\u0004\b\u0007\u0010\bR\u0014\u0010\u000b\u001a\u00020\u00048BX\u0082\u0004\u00a2\u0006\u0006\u001a\u0004\b\f\u0010\rR\u000e\u0010\u000e\u001a\u00020\u000fX\u0082.\u00a2\u0006\u0002\n\u0000R\u001b\u0010\u0010\u001a\u00020\u00118BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0014\u0010\u0015\u001a\u0004\b\u0012\u0010\u0013\u00a8\u00060"}, d2 = {"Lcom/smartcbwtf/mobile/ui/AttendanceFragment;", "Landroidx/fragment/app/Fragment;", "()V", "_binding", "Lcom/smartcbwtf/mobile/databinding/FragmentAttendanceBinding;", "args", "Lcom/smartcbwtf/mobile/ui/AttendanceFragmentArgs;", "getArgs", "()Lcom/smartcbwtf/mobile/ui/AttendanceFragmentArgs;", "args$delegate", "Landroidx/navigation/NavArgsLazy;", "binding", "getBinding", "()Lcom/smartcbwtf/mobile/databinding/FragmentAttendanceBinding;", "hcfAdapter", "Lcom/smartcbwtf/mobile/ui/adapter/HcfSelectionAdapter;", "viewModel", "Lcom/smartcbwtf/mobile/viewmodel/AttendanceViewModel;", "getViewModel", "()Lcom/smartcbwtf/mobile/viewmodel/AttendanceViewModel;", "viewModel$delegate", "Lkotlin/Lazy;", "handleAttendanceResult", "", "result", "Lcom/smartcbwtf/mobile/viewmodel/AttendanceResult;", "observeViewModel", "onDestroyView", "onViewCreated", "view", "Landroid/view/View;", "savedInstanceState", "Landroid/os/Bundle;", "setupRecyclerView", "setupViews", "showConfirmationDialog", "showSuccessDialog", "hcfName", "", "updateCooldownUI", "remainingMs", "", "updateHcfSearchUI", "state", "Lcom/smartcbwtf/mobile/viewmodel/HcfSearchState;", "updateLocationUI", "Lcom/smartcbwtf/mobile/viewmodel/AttendanceLocationState;", "updateMarkButtonState", "app_debug"})
public final class AttendanceFragment extends androidx.fragment.app.Fragment {
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy viewModel$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.navigation.NavArgsLazy args$delegate = null;
    @org.jetbrains.annotations.Nullable()
    private com.smartcbwtf.mobile.databinding.FragmentAttendanceBinding _binding;
    private com.smartcbwtf.mobile.ui.adapter.HcfSelectionAdapter hcfAdapter;
    
    public AttendanceFragment() {
        super();
    }
    
    private final com.smartcbwtf.mobile.viewmodel.AttendanceViewModel getViewModel() {
        return null;
    }
    
    private final com.smartcbwtf.mobile.ui.AttendanceFragmentArgs getArgs() {
        return null;
    }
    
    private final com.smartcbwtf.mobile.databinding.FragmentAttendanceBinding getBinding() {
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
    
    private final void setupViews() {
    }
    
    private final void setupRecyclerView() {
    }
    
    private final void observeViewModel() {
    }
    
    private final void updateLocationUI(com.smartcbwtf.mobile.viewmodel.AttendanceLocationState state) {
    }
    
    private final void updateHcfSearchUI(com.smartcbwtf.mobile.viewmodel.HcfSearchState state) {
    }
    
    private final void updateMarkButtonState() {
    }
    
    private final void updateCooldownUI(long remainingMs) {
    }
    
    private final void handleAttendanceResult(com.smartcbwtf.mobile.viewmodel.AttendanceResult result) {
    }
    
    private final void showConfirmationDialog() {
    }
    
    private final void showSuccessDialog(java.lang.String hcfName) {
    }
}