package com.smartcbwtf.mobile.viewmodel;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0016\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b6\u0018\u00002\u00020\u0001:\u0002\u0003\u0004B\u0007\b\u0004\u00a2\u0006\u0002\u0010\u0002\u0082\u0001\u0002\u0005\u0006\u00a8\u0006\u0007"}, d2 = {"Lcom/smartcbwtf/mobile/viewmodel/AuthEvent;", "", "()V", "NavigateToChangePassword", "NavigateToHome", "Lcom/smartcbwtf/mobile/viewmodel/AuthEvent$NavigateToChangePassword;", "Lcom/smartcbwtf/mobile/viewmodel/AuthEvent$NavigateToHome;", "app_debug"})
public abstract class AuthEvent {
    
    private AuthEvent() {
        super();
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/smartcbwtf/mobile/viewmodel/AuthEvent$NavigateToChangePassword;", "Lcom/smartcbwtf/mobile/viewmodel/AuthEvent;", "()V", "app_debug"})
    public static final class NavigateToChangePassword extends com.smartcbwtf.mobile.viewmodel.AuthEvent {
        @org.jetbrains.annotations.NotNull()
        public static final com.smartcbwtf.mobile.viewmodel.AuthEvent.NavigateToChangePassword INSTANCE = null;
        
        private NavigateToChangePassword() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/smartcbwtf/mobile/viewmodel/AuthEvent$NavigateToHome;", "Lcom/smartcbwtf/mobile/viewmodel/AuthEvent;", "()V", "app_debug"})
    public static final class NavigateToHome extends com.smartcbwtf.mobile.viewmodel.AuthEvent {
        @org.jetbrains.annotations.NotNull()
        public static final com.smartcbwtf.mobile.viewmodel.AuthEvent.NavigateToHome INSTANCE = null;
        
        private NavigateToHome() {
        }
    }
}