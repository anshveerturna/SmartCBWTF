package com.smartcbwtf.mobile.ui;

@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000N\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u000b\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\b\u0007\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u000e\u001a\u00020\u000fH\u0002J\b\u0010\u0010\u001a\u00020\u0011H\u0002J\b\u0010\u0012\u001a\u00020\u0011H\u0016J\u001a\u0010\u0013\u001a\u00020\u00112\u0006\u0010\u0014\u001a\u00020\u00152\b\u0010\u0016\u001a\u0004\u0018\u00010\u0017H\u0016J\u0010\u0010\u0018\u001a\u00020\u00112\u0006\u0010\u0019\u001a\u00020\u001aH\u0002J\b\u0010\u001b\u001a\u00020\u0011H\u0002J\b\u0010\u001c\u001a\u00020\u0011H\u0002J\b\u0010\u001d\u001a\u00020\u0011H\u0002J\b\u0010\u001e\u001a\u00020\u0011H\u0002J\b\u0010\u001f\u001a\u00020\u0011H\u0002J\u001c\u0010 \u001a\u00020\u00112\b\u0010!\u001a\u0004\u0018\u00010\u001a2\b\u0010\u0019\u001a\u0004\u0018\u00010\u001aH\u0002J\b\u0010\"\u001a\u00020\u0011H\u0002J\b\u0010#\u001a\u00020\u0011H\u0002J\u0010\u0010$\u001a\u00020\u00112\u0006\u0010%\u001a\u00020&H\u0002J\b\u0010\'\u001a\u00020\u0011H\u0002J\u0010\u0010(\u001a\u00020\u00112\u0006\u0010%\u001a\u00020)H\u0002R\u0010\u0010\u0003\u001a\u0004\u0018\u00010\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0005\u001a\u00020\u00048BX\u0082\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0006\u0010\u0007R\u001b\u0010\b\u001a\u00020\t8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\f\u0010\r\u001a\u0004\b\n\u0010\u000b\u00a8\u0006*"}, d2 = {"Lcom/smartcbwtf/mobile/ui/HcfRegistrationFragment;", "Landroidx/fragment/app/Fragment;", "()V", "_binding", "Lcom/smartcbwtf/mobile/databinding/FragmentHcfRegistrationBinding;", "binding", "getBinding", "()Lcom/smartcbwtf/mobile/databinding/FragmentHcfRegistrationBinding;", "viewModel", "Lcom/smartcbwtf/mobile/viewmodel/HcfRegistrationViewModel;", "getViewModel", "()Lcom/smartcbwtf/mobile/viewmodel/HcfRegistrationViewModel;", "viewModel$delegate", "Lkotlin/Lazy;", "canSubmit", "", "observeStates", "", "onDestroyView", "onViewCreated", "view", "Landroid/view/View;", "savedInstanceState", "Landroid/os/Bundle;", "openPdf", "pdfUrl", "", "setupFormFields", "setupGpsCapture", "setupRegisterButton", "setupTermsCard", "setupToolbar", "showSuccessDialog", "agreementNumber", "showTermsDialog", "submitRegistration", "updateGpsUI", "state", "Lcom/smartcbwtf/mobile/viewmodel/GpsState;", "updateRegisterButtonState", "updateTermsUI", "Lcom/smartcbwtf/mobile/viewmodel/TermsState;", "app_debug"})
public final class HcfRegistrationFragment extends androidx.fragment.app.Fragment {
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy viewModel$delegate = null;
    @org.jetbrains.annotations.Nullable()
    private com.smartcbwtf.mobile.databinding.FragmentHcfRegistrationBinding _binding;
    
    public HcfRegistrationFragment() {
        super();
    }
    
    private final com.smartcbwtf.mobile.viewmodel.HcfRegistrationViewModel getViewModel() {
        return null;
    }
    
    private final com.smartcbwtf.mobile.databinding.FragmentHcfRegistrationBinding getBinding() {
        return null;
    }
    
    @java.lang.Override()
    public void onViewCreated(@org.jetbrains.annotations.NotNull()
    android.view.View view, @org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    private final void setupToolbar() {
    }
    
    private final void setupFormFields() {
    }
    
    private final void setupGpsCapture() {
    }
    
    private final void setupTermsCard() {
    }
    
    private final void showTermsDialog() {
    }
    
    private final void setupRegisterButton() {
    }
    
    private final void submitRegistration() {
    }
    
    private final void updateRegisterButtonState() {
    }
    
    private final void observeStates() {
    }
    
    private final void updateGpsUI(com.smartcbwtf.mobile.viewmodel.GpsState state) {
    }
    
    private final void updateTermsUI(com.smartcbwtf.mobile.viewmodel.TermsState state) {
    }
    
    private final boolean canSubmit() {
        return false;
    }
    
    private final void showSuccessDialog(java.lang.String agreementNumber, java.lang.String pdfUrl) {
    }
    
    private final void openPdf(java.lang.String pdfUrl) {
    }
    
    @java.lang.Override()
    public void onDestroyView() {
    }
}