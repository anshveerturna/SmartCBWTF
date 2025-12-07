package com.smartcbwtf.mobile.viewmodel;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b6\u0018\u00002\u00020\u0001:\u0003\u0003\u0004\u0005B\u0007\b\u0004\u00a2\u0006\u0002\u0010\u0002\u0082\u0001\u0003\u0006\u0007\b\u00a8\u0006\t"}, d2 = {"Lcom/smartcbwtf/mobile/viewmodel/AuthState;", "", "()V", "Authenticated", "Loading", "Unauthenticated", "Lcom/smartcbwtf/mobile/viewmodel/AuthState$Authenticated;", "Lcom/smartcbwtf/mobile/viewmodel/AuthState$Loading;", "Lcom/smartcbwtf/mobile/viewmodel/AuthState$Unauthenticated;", "app_debug"})
public abstract class AuthState {
    
    private AuthState() {
        super();
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/smartcbwtf/mobile/viewmodel/AuthState$Authenticated;", "Lcom/smartcbwtf/mobile/viewmodel/AuthState;", "()V", "app_debug"})
    public static final class Authenticated extends com.smartcbwtf.mobile.viewmodel.AuthState {
        @org.jetbrains.annotations.NotNull()
        public static final com.smartcbwtf.mobile.viewmodel.AuthState.Authenticated INSTANCE = null;
        
        private Authenticated() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/smartcbwtf/mobile/viewmodel/AuthState$Loading;", "Lcom/smartcbwtf/mobile/viewmodel/AuthState;", "()V", "app_debug"})
    public static final class Loading extends com.smartcbwtf.mobile.viewmodel.AuthState {
        @org.jetbrains.annotations.NotNull()
        public static final com.smartcbwtf.mobile.viewmodel.AuthState.Loading INSTANCE = null;
        
        private Loading() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/smartcbwtf/mobile/viewmodel/AuthState$Unauthenticated;", "Lcom/smartcbwtf/mobile/viewmodel/AuthState;", "()V", "app_debug"})
    public static final class Unauthenticated extends com.smartcbwtf.mobile.viewmodel.AuthState {
        @org.jetbrains.annotations.NotNull()
        public static final com.smartcbwtf.mobile.viewmodel.AuthState.Unauthenticated INSTANCE = null;
        
        private Unauthenticated() {
        }
    }
}