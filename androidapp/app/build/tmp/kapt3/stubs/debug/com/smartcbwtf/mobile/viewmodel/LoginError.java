package com.smartcbwtf.mobile.viewmodel;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b6\u0018\u00002\u00020\u0001:\u0005\u0003\u0004\u0005\u0006\u0007B\u0007\b\u0004\u00a2\u0006\u0002\u0010\u0002\u0082\u0001\u0005\b\t\n\u000b\f\u00a8\u0006\r"}, d2 = {"Lcom/smartcbwtf/mobile/viewmodel/LoginError;", "", "()V", "EmptyPassword", "EmptyUsername", "InvalidCredentials", "NetworkError", "UnknownError", "Lcom/smartcbwtf/mobile/viewmodel/LoginError$EmptyPassword;", "Lcom/smartcbwtf/mobile/viewmodel/LoginError$EmptyUsername;", "Lcom/smartcbwtf/mobile/viewmodel/LoginError$InvalidCredentials;", "Lcom/smartcbwtf/mobile/viewmodel/LoginError$NetworkError;", "Lcom/smartcbwtf/mobile/viewmodel/LoginError$UnknownError;", "app_debug"})
public abstract class LoginError {
    
    private LoginError() {
        super();
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/smartcbwtf/mobile/viewmodel/LoginError$EmptyPassword;", "Lcom/smartcbwtf/mobile/viewmodel/LoginError;", "()V", "app_debug"})
    public static final class EmptyPassword extends com.smartcbwtf.mobile.viewmodel.LoginError {
        @org.jetbrains.annotations.NotNull()
        public static final com.smartcbwtf.mobile.viewmodel.LoginError.EmptyPassword INSTANCE = null;
        
        private EmptyPassword() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/smartcbwtf/mobile/viewmodel/LoginError$EmptyUsername;", "Lcom/smartcbwtf/mobile/viewmodel/LoginError;", "()V", "app_debug"})
    public static final class EmptyUsername extends com.smartcbwtf.mobile.viewmodel.LoginError {
        @org.jetbrains.annotations.NotNull()
        public static final com.smartcbwtf.mobile.viewmodel.LoginError.EmptyUsername INSTANCE = null;
        
        private EmptyUsername() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/smartcbwtf/mobile/viewmodel/LoginError$InvalidCredentials;", "Lcom/smartcbwtf/mobile/viewmodel/LoginError;", "()V", "app_debug"})
    public static final class InvalidCredentials extends com.smartcbwtf.mobile.viewmodel.LoginError {
        @org.jetbrains.annotations.NotNull()
        public static final com.smartcbwtf.mobile.viewmodel.LoginError.InvalidCredentials INSTANCE = null;
        
        private InvalidCredentials() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/smartcbwtf/mobile/viewmodel/LoginError$NetworkError;", "Lcom/smartcbwtf/mobile/viewmodel/LoginError;", "()V", "app_debug"})
    public static final class NetworkError extends com.smartcbwtf.mobile.viewmodel.LoginError {
        @org.jetbrains.annotations.NotNull()
        public static final com.smartcbwtf.mobile.viewmodel.LoginError.NetworkError INSTANCE = null;
        
        private NetworkError() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/smartcbwtf/mobile/viewmodel/LoginError$UnknownError;", "Lcom/smartcbwtf/mobile/viewmodel/LoginError;", "()V", "app_debug"})
    public static final class UnknownError extends com.smartcbwtf.mobile.viewmodel.LoginError {
        @org.jetbrains.annotations.NotNull()
        public static final com.smartcbwtf.mobile.viewmodel.LoginError.UnknownError INSTANCE = null;
        
        private UnknownError() {
        }
    }
}