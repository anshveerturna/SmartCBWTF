package com.smartcbwtf.mobile.viewmodel;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000~\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u000b\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u000b\n\u0002\b\u000f\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u0006\n\u0002\b\u0005\b\u0007\u0018\u0000 E2\u00020\u0001:\u0001EB1\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0001\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u0012\u0006\u0010\n\u001a\u00020\u000b\u00a2\u0006\u0002\u0010\fJ\u0006\u0010$\u001a\u00020%J\u0010\u0010&\u001a\u00020\'2\u0006\u0010(\u001a\u00020)H\u0002J\u0012\u0010*\u001a\u00020%2\n\b\u0002\u0010+\u001a\u0004\u0018\u00010\'J\u0006\u0010,\u001a\u00020%J\u000e\u0010-\u001a\u00020%2\u0006\u0010.\u001a\u00020/J\u000e\u00100\u001a\u00020%2\u0006\u00101\u001a\u00020\'J\u000e\u00102\u001a\u00020%2\u0006\u00103\u001a\u00020/J\u008b\u0001\u00104\u001a\u00020%2\u0006\u00105\u001a\u00020\'2\b\u00106\u001a\u0004\u0018\u00010\'2\b\u00107\u001a\u0004\u0018\u00010\'2\b\u0010 \u001a\u0004\u0018\u00010\'2\b\u00108\u001a\u0004\u0018\u00010\'2\b\u00109\u001a\u0004\u0018\u00010\'2\b\u0010:\u001a\u0004\u0018\u00010\'2\b\u0010;\u001a\u0004\u0018\u00010\'2\b\u0010<\u001a\u0004\u0018\u00010\'2\b\u0010=\u001a\u0004\u0018\u00010\'2\b\u0010>\u001a\u0004\u0018\u00010?2\b\u0010@\u001a\u0004\u0018\u00010A2\b\u0010B\u001a\u0004\u0018\u00010\'\u00a2\u0006\u0002\u0010CJ\u000e\u0010D\u001a\u00020%2\u0006\u0010(\u001a\u00020)R\u0014\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u000f0\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00110\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00130\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00150\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u00170\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u000f0\u0019\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001a\u0010\u001bR\u0017\u0010\u001c\u001a\b\u0012\u0004\u0012\u00020\u00110\u0019\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001d\u0010\u001bR\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u001e\u001a\b\u0012\u0004\u0012\u00020\u00130\u0019\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001f\u0010\u001bR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010 \u001a\b\u0012\u0004\u0012\u00020\u00150\u0019\u00a2\u0006\b\n\u0000\u001a\u0004\b!\u0010\u001bR\u0017\u0010\"\u001a\b\u0012\u0004\u0012\u00020\u00170\u0019\u00a2\u0006\b\n\u0000\u001a\u0004\b#\u0010\u001b\u00a8\u0006F"}, d2 = {"Lcom/smartcbwtf/mobile/viewmodel/HcfRegistrationViewModel;", "Landroidx/lifecycle/ViewModel;", "savedStateHandle", "Landroidx/lifecycle/SavedStateHandle;", "appContext", "Landroid/content/Context;", "hcfRepository", "Lcom/smartcbwtf/mobile/repository/HcfRepository;", "locationHelper", "Lcom/smartcbwtf/mobile/utils/LocationHelper;", "sessionManager", "Lcom/smartcbwtf/mobile/storage/SessionManager;", "(Landroidx/lifecycle/SavedStateHandle;Landroid/content/Context;Lcom/smartcbwtf/mobile/repository/HcfRepository;Lcom/smartcbwtf/mobile/utils/LocationHelper;Lcom/smartcbwtf/mobile/storage/SessionManager;)V", "_formState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/smartcbwtf/mobile/viewmodel/HcfFormState;", "_gpsState", "Lcom/smartcbwtf/mobile/viewmodel/GpsState;", "_rentAgreementState", "Lcom/smartcbwtf/mobile/viewmodel/RentAgreementState;", "_state", "Lcom/smartcbwtf/mobile/viewmodel/RegistrationState;", "_termsState", "Lcom/smartcbwtf/mobile/viewmodel/TermsState;", "formState", "Lkotlinx/coroutines/flow/StateFlow;", "getFormState", "()Lkotlinx/coroutines/flow/StateFlow;", "gpsState", "getGpsState", "rentAgreementState", "getRentAgreementState", "state", "getState", "termsState", "getTermsState", "captureGpsLocation", "", "getFileName", "", "uri", "Landroid/net/Uri;", "loadTerms", "facilityId", "reset", "setBedded", "bedded", "", "setOwnershipType", "type", "setTermsAccepted", "accepted", "submit", "name", "address", "pincode", "doctorName", "phone", "email", "panNo", "gstNo", "aadharNo", "beds", "", "monthlyCharges", "", "otherNotes", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Integer;Ljava/lang/Double;Ljava/lang/String;)V", "uploadRentAgreement", "Companion", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class HcfRegistrationViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final androidx.lifecycle.SavedStateHandle savedStateHandle = null;
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context appContext = null;
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
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.smartcbwtf.mobile.viewmodel.RentAgreementState> _rentAgreementState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.smartcbwtf.mobile.viewmodel.RentAgreementState> rentAgreementState = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_RENT_AGREEMENT_URL = "rent_agreement_url";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_RENT_AGREEMENT_NAME = "rent_agreement_name";
    @org.jetbrains.annotations.NotNull()
    public static final com.smartcbwtf.mobile.viewmodel.HcfRegistrationViewModel.Companion Companion = null;
    
    @javax.inject.Inject()
    public HcfRegistrationViewModel(@org.jetbrains.annotations.NotNull()
    androidx.lifecycle.SavedStateHandle savedStateHandle, @dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context appContext, @org.jetbrains.annotations.NotNull()
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
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.smartcbwtf.mobile.viewmodel.RentAgreementState> getRentAgreementState() {
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
    
    public final void setOwnershipType(@org.jetbrains.annotations.NotNull()
    java.lang.String type) {
    }
    
    public final void uploadRentAgreement(@org.jetbrains.annotations.NotNull()
    android.net.Uri uri) {
    }
    
    private final java.lang.String getFileName(android.net.Uri uri) {
        return null;
    }
    
    public final void submit(@org.jetbrains.annotations.NotNull()
    java.lang.String name, @org.jetbrains.annotations.Nullable()
    java.lang.String address, @org.jetbrains.annotations.Nullable()
    java.lang.String pincode, @org.jetbrains.annotations.Nullable()
    java.lang.String state, @org.jetbrains.annotations.Nullable()
    java.lang.String doctorName, @org.jetbrains.annotations.Nullable()
    java.lang.String phone, @org.jetbrains.annotations.Nullable()
    java.lang.String email, @org.jetbrains.annotations.Nullable()
    java.lang.String panNo, @org.jetbrains.annotations.Nullable()
    java.lang.String gstNo, @org.jetbrains.annotations.Nullable()
    java.lang.String aadharNo, @org.jetbrains.annotations.Nullable()
    java.lang.Integer beds, @org.jetbrains.annotations.Nullable()
    java.lang.Double monthlyCharges, @org.jetbrains.annotations.Nullable()
    java.lang.String otherNotes) {
    }
    
    public final void reset() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0006"}, d2 = {"Lcom/smartcbwtf/mobile/viewmodel/HcfRegistrationViewModel$Companion;", "", "()V", "KEY_RENT_AGREEMENT_NAME", "", "KEY_RENT_AGREEMENT_URL", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}