package com.smartcbwtf.mobile.viewmodel;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000h\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\t\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\r\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u0006\n\u0002\b\u0003\b\u0007\u0018\u00002\u00020\u0001B\u001f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\u0006\u0010\u001c\u001a\u00020\u001dJ\u0012\u0010\u001e\u001a\u00020\u001d2\n\b\u0002\u0010\u001f\u001a\u0004\u0018\u00010 J\u0006\u0010!\u001a\u00020\u001dJ\u000e\u0010\"\u001a\u00020\u001d2\u0006\u0010#\u001a\u00020$J\u000e\u0010%\u001a\u00020\u001d2\u0006\u0010&\u001a\u00020$J\u0081\u0001\u0010\'\u001a\u00020\u001d2\u0006\u0010(\u001a\u00020 2\b\u0010)\u001a\u0004\u0018\u00010 2\b\u0010*\u001a\u0004\u0018\u00010 2\b\u0010+\u001a\u0004\u0018\u00010 2\b\u0010,\u001a\u0004\u0018\u00010 2\b\u0010-\u001a\u0004\u0018\u00010 2\b\u0010.\u001a\u0004\u0018\u00010 2\b\u0010/\u001a\u0004\u0018\u00010 2\b\u00100\u001a\u0004\u0018\u00010 2\b\u00101\u001a\u0004\u0018\u0001022\b\u00103\u001a\u0004\u0018\u0001042\b\u00105\u001a\u0004\u0018\u00010 \u00a2\u0006\u0002\u00106R\u0014\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u000b0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\f\u001a\b\u0012\u0004\u0012\u00020\r0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u000f0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00110\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u000b0\u0013\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u0015R\u0017\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\r0\u0013\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u0015R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u000f0\u0013\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0019\u0010\u0015R\u0017\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\u00110\u0013\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001b\u0010\u0015\u00a8\u00067"}, d2 = {"Lcom/smartcbwtf/mobile/viewmodel/HcfRegistrationViewModel;", "Landroidx/lifecycle/ViewModel;", "hcfRepository", "Lcom/smartcbwtf/mobile/repository/HcfRepository;", "locationHelper", "Lcom/smartcbwtf/mobile/utils/LocationHelper;", "sessionManager", "Lcom/smartcbwtf/mobile/storage/SessionManager;", "(Lcom/smartcbwtf/mobile/repository/HcfRepository;Lcom/smartcbwtf/mobile/utils/LocationHelper;Lcom/smartcbwtf/mobile/storage/SessionManager;)V", "_formState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/smartcbwtf/mobile/viewmodel/HcfFormState;", "_gpsState", "Lcom/smartcbwtf/mobile/viewmodel/GpsState;", "_state", "Lcom/smartcbwtf/mobile/viewmodel/RegistrationState;", "_termsState", "Lcom/smartcbwtf/mobile/viewmodel/TermsState;", "formState", "Lkotlinx/coroutines/flow/StateFlow;", "getFormState", "()Lkotlinx/coroutines/flow/StateFlow;", "gpsState", "getGpsState", "state", "getState", "termsState", "getTermsState", "captureGpsLocation", "", "loadTerms", "facilityId", "", "reset", "setBedded", "bedded", "", "setTermsAccepted", "accepted", "submit", "name", "address", "doctorName", "phone", "email", "panNo", "gstNo", "aadharNo", "pcbNo", "beds", "", "monthlyCharges", "", "otherNotes", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Integer;Ljava/lang/Double;Ljava/lang/String;)V", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class HcfRegistrationViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.smartcbwtf.mobile.repository.HcfRepository hcfRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.smartcbwtf.mobile.utils.LocationHelper locationHelper = null;
    @org.jetbrains.annotations.NotNull()
    private final com.smartcbwtf.mobile.storage.SessionManager sessionManager = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.smartcbwtf.mobile.viewmodel.RegistrationState> _state = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.smartcbwtf.mobile.viewmodel.RegistrationState> state = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.smartcbwtf.mobile.viewmodel.HcfFormState> _formState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.smartcbwtf.mobile.viewmodel.HcfFormState> formState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.smartcbwtf.mobile.viewmodel.TermsState> _termsState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.smartcbwtf.mobile.viewmodel.TermsState> termsState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.smartcbwtf.mobile.viewmodel.GpsState> _gpsState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.smartcbwtf.mobile.viewmodel.GpsState> gpsState = null;
    
    @javax.inject.Inject()
    public HcfRegistrationViewModel(@org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.repository.HcfRepository hcfRepository, @org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.utils.LocationHelper locationHelper, @org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.storage.SessionManager sessionManager) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.smartcbwtf.mobile.viewmodel.RegistrationState> getState() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.smartcbwtf.mobile.viewmodel.HcfFormState> getFormState() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.smartcbwtf.mobile.viewmodel.TermsState> getTermsState() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.smartcbwtf.mobile.viewmodel.GpsState> getGpsState() {
        return null;
    }
    
    public final void loadTerms(@org.jetbrains.annotations.Nullable()
    java.lang.String facilityId) {
    }
    
    public final void captureGpsLocation() {
    }
    
    public final void setTermsAccepted(boolean accepted) {
    }
    
    public final void setBedded(boolean bedded) {
    }
    
    public final void submit(@org.jetbrains.annotations.NotNull()
    java.lang.String name, @org.jetbrains.annotations.Nullable()
    java.lang.String address, @org.jetbrains.annotations.Nullable()
    java.lang.String doctorName, @org.jetbrains.annotations.Nullable()
    java.lang.String phone, @org.jetbrains.annotations.Nullable()
    java.lang.String email, @org.jetbrains.annotations.Nullable()
    java.lang.String panNo, @org.jetbrains.annotations.Nullable()
    java.lang.String gstNo, @org.jetbrains.annotations.Nullable()
    java.lang.String aadharNo, @org.jetbrains.annotations.Nullable()
    java.lang.String pcbNo, @org.jetbrains.annotations.Nullable()
    java.lang.Integer beds, @org.jetbrains.annotations.Nullable()
    java.lang.Double monthlyCharges, @org.jetbrains.annotations.Nullable()
    java.lang.String otherNotes) {
    }
    
    public final void reset() {
    }
}