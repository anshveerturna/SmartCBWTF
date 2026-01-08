package com.smartcbwtf.mobile.viewmodel;

/**
 * ViewModel for My Route screen.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000,\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0002\b\u0007\u0018\u00002\u00020\u0001:\u0001\u000eB\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0006\u0010\f\u001a\u00020\rR\u0014\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00070\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000b\u00a8\u0006\u000f"}, d2 = {"Lcom/smartcbwtf/mobile/viewmodel/MyRouteViewModel;", "Landroidx/lifecycle/ViewModel;", "routeRepository", "Lcom/smartcbwtf/mobile/repository/RouteRepository;", "(Lcom/smartcbwtf/mobile/repository/RouteRepository;)V", "_uiState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/smartcbwtf/mobile/viewmodel/MyRouteViewModel$UiState;", "uiState", "Lkotlinx/coroutines/flow/StateFlow;", "getUiState", "()Lkotlinx/coroutines/flow/StateFlow;", "loadRoute", "", "UiState", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class MyRouteViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.smartcbwtf.mobile.repository.RouteRepository routeRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.smartcbwtf.mobile.viewmodel.MyRouteViewModel.UiState> _uiState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.smartcbwtf.mobile.viewmodel.MyRouteViewModel.UiState> uiState = null;
    
    @javax.inject.Inject()
    public MyRouteViewModel(@org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.repository.RouteRepository routeRepository) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.smartcbwtf.mobile.viewmodel.MyRouteViewModel.UiState> getUiState() {
        return null;
    }
    
    public final void loadRoute() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001e\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b6\u0018\u00002\u00020\u0001:\u0004\u0003\u0004\u0005\u0006B\u0007\b\u0004\u00a2\u0006\u0002\u0010\u0002\u0082\u0001\u0004\u0007\b\t\n\u00a8\u0006\u000b"}, d2 = {"Lcom/smartcbwtf/mobile/viewmodel/MyRouteViewModel$UiState;", "", "()V", "Error", "Loading", "NoRouteAssigned", "Success", "Lcom/smartcbwtf/mobile/viewmodel/MyRouteViewModel$UiState$Error;", "Lcom/smartcbwtf/mobile/viewmodel/MyRouteViewModel$UiState$Loading;", "Lcom/smartcbwtf/mobile/viewmodel/MyRouteViewModel$UiState$NoRouteAssigned;", "Lcom/smartcbwtf/mobile/viewmodel/MyRouteViewModel$UiState$Success;", "app_debug"})
    public static abstract class UiState {
        
        private UiState() {
            super();
        }
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\t\u0010\u0007\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\t\u001a\u00020\n2\b\u0010\u000b\u001a\u0004\u0018\u00010\fH\u00d6\u0003J\t\u0010\r\u001a\u00020\u000eH\u00d6\u0001J\t\u0010\u000f\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0010"}, d2 = {"Lcom/smartcbwtf/mobile/viewmodel/MyRouteViewModel$UiState$Error;", "Lcom/smartcbwtf/mobile/viewmodel/MyRouteViewModel$UiState;", "message", "", "(Ljava/lang/String;)V", "getMessage", "()Ljava/lang/String;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "app_debug"})
        public static final class Error extends com.smartcbwtf.mobile.viewmodel.MyRouteViewModel.UiState {
            @org.jetbrains.annotations.NotNull()
            private final java.lang.String message = null;
            
            public Error(@org.jetbrains.annotations.NotNull()
            java.lang.String message) {
            }
            
            @org.jetbrains.annotations.NotNull()
            public final java.lang.String getMessage() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final java.lang.String component1() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final com.smartcbwtf.mobile.viewmodel.MyRouteViewModel.UiState.Error copy(@org.jetbrains.annotations.NotNull()
            java.lang.String message) {
                return null;
            }
            
            @java.lang.Override()
            public boolean equals(@org.jetbrains.annotations.Nullable()
            java.lang.Object other) {
                return false;
            }
            
            @java.lang.Override()
            public int hashCode() {
                return 0;
            }
            
            @java.lang.Override()
            @org.jetbrains.annotations.NotNull()
            public java.lang.String toString() {
                return null;
            }
        }
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/smartcbwtf/mobile/viewmodel/MyRouteViewModel$UiState$Loading;", "Lcom/smartcbwtf/mobile/viewmodel/MyRouteViewModel$UiState;", "()V", "app_debug"})
        public static final class Loading extends com.smartcbwtf.mobile.viewmodel.MyRouteViewModel.UiState {
            @org.jetbrains.annotations.NotNull()
            public static final com.smartcbwtf.mobile.viewmodel.MyRouteViewModel.UiState.Loading INSTANCE = null;
            
            private Loading() {
            }
        }
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/smartcbwtf/mobile/viewmodel/MyRouteViewModel$UiState$NoRouteAssigned;", "Lcom/smartcbwtf/mobile/viewmodel/MyRouteViewModel$UiState;", "()V", "app_debug"})
        public static final class NoRouteAssigned extends com.smartcbwtf.mobile.viewmodel.MyRouteViewModel.UiState {
            @org.jetbrains.annotations.NotNull()
            public static final com.smartcbwtf.mobile.viewmodel.MyRouteViewModel.UiState.NoRouteAssigned INSTANCE = null;
            
            private NoRouteAssigned() {
            }
        }
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\t\u0010\u0007\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\t\u001a\u00020\n2\b\u0010\u000b\u001a\u0004\u0018\u00010\fH\u00d6\u0003J\t\u0010\r\u001a\u00020\u000eH\u00d6\u0001J\t\u0010\u000f\u001a\u00020\u0010H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0011"}, d2 = {"Lcom/smartcbwtf/mobile/viewmodel/MyRouteViewModel$UiState$Success;", "Lcom/smartcbwtf/mobile/viewmodel/MyRouteViewModel$UiState;", "route", "Lcom/smartcbwtf/mobile/network/model/MobileRouteResponse;", "(Lcom/smartcbwtf/mobile/network/model/MobileRouteResponse;)V", "getRoute", "()Lcom/smartcbwtf/mobile/network/model/MobileRouteResponse;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "", "app_debug"})
        public static final class Success extends com.smartcbwtf.mobile.viewmodel.MyRouteViewModel.UiState {
            @org.jetbrains.annotations.NotNull()
            private final com.smartcbwtf.mobile.network.model.MobileRouteResponse route = null;
            
            public Success(@org.jetbrains.annotations.NotNull()
            com.smartcbwtf.mobile.network.model.MobileRouteResponse route) {
            }
            
            @org.jetbrains.annotations.NotNull()
            public final com.smartcbwtf.mobile.network.model.MobileRouteResponse getRoute() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final com.smartcbwtf.mobile.network.model.MobileRouteResponse component1() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final com.smartcbwtf.mobile.viewmodel.MyRouteViewModel.UiState.Success copy(@org.jetbrains.annotations.NotNull()
            com.smartcbwtf.mobile.network.model.MobileRouteResponse route) {
                return null;
            }
            
            @java.lang.Override()
            public boolean equals(@org.jetbrains.annotations.Nullable()
            java.lang.Object other) {
                return false;
            }
            
            @java.lang.Override()
            public int hashCode() {
                return 0;
            }
            
            @java.lang.Override()
            @org.jetbrains.annotations.NotNull()
            public java.lang.String toString() {
                return null;
            }
        }
    }
}