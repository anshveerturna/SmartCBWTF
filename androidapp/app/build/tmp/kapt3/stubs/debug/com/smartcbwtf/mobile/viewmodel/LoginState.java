package com.smartcbwtf.mobile.viewmodel;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b6\u0018\u00002\u00020\u0001:\u0005\u0003\u0004\u0005\u0006\u0007B\u0007\b\u0004\u00a2\u0006\u0002\u0010\u0002\u0082\u0001\u0005\b\t\n\u000b\f\u00a8\u0006\r"}, d2 = {"Lcom/smartcbwtf/mobile/viewmodel/LoginState;", "", "()V", "Error", "Idle", "Loading", "Success", "ValidationFailed", "Lcom/smartcbwtf/mobile/viewmodel/LoginState$Error;", "Lcom/smartcbwtf/mobile/viewmodel/LoginState$Idle;", "Lcom/smartcbwtf/mobile/viewmodel/LoginState$Loading;", "Lcom/smartcbwtf/mobile/viewmodel/LoginState$Success;", "Lcom/smartcbwtf/mobile/viewmodel/LoginState$ValidationFailed;", "app_debug"})
public abstract class LoginState {
    
    private LoginState() {
        super();
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\t\u0010\u0007\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\t\u001a\u00020\n2\b\u0010\u000b\u001a\u0004\u0018\u00010\fH\u00d6\u0003J\t\u0010\r\u001a\u00020\u000eH\u00d6\u0001J\t\u0010\u000f\u001a\u00020\u0010H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0011"}, d2 = {"Lcom/smartcbwtf/mobile/viewmodel/LoginState$Error;", "Lcom/smartcbwtf/mobile/viewmodel/LoginState;", "error", "Lcom/smartcbwtf/mobile/viewmodel/LoginError;", "(Lcom/smartcbwtf/mobile/viewmodel/LoginError;)V", "getError", "()Lcom/smartcbwtf/mobile/viewmodel/LoginError;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "", "app_debug"})
    public static final class Error extends com.smartcbwtf.mobile.viewmodel.LoginState {
        @org.jetbrains.annotations.NotNull()
        private final com.smartcbwtf.mobile.viewmodel.LoginError error = null;
        
        public Error(@org.jetbrains.annotations.NotNull()
        com.smartcbwtf.mobile.viewmodel.LoginError error) {
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.smartcbwtf.mobile.viewmodel.LoginError getError() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.smartcbwtf.mobile.viewmodel.LoginError component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.smartcbwtf.mobile.viewmodel.LoginState.Error copy(@org.jetbrains.annotations.NotNull()
        com.smartcbwtf.mobile.viewmodel.LoginError error) {
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
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/smartcbwtf/mobile/viewmodel/LoginState$Idle;", "Lcom/smartcbwtf/mobile/viewmodel/LoginState;", "()V", "app_debug"})
    public static final class Idle extends com.smartcbwtf.mobile.viewmodel.LoginState {
        @org.jetbrains.annotations.NotNull()
        public static final com.smartcbwtf.mobile.viewmodel.LoginState.Idle INSTANCE = null;
        
        private Idle() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/smartcbwtf/mobile/viewmodel/LoginState$Loading;", "Lcom/smartcbwtf/mobile/viewmodel/LoginState;", "()V", "app_debug"})
    public static final class Loading extends com.smartcbwtf.mobile.viewmodel.LoginState {
        @org.jetbrains.annotations.NotNull()
        public static final com.smartcbwtf.mobile.viewmodel.LoginState.Loading INSTANCE = null;
        
        private Loading() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/smartcbwtf/mobile/viewmodel/LoginState$Success;", "Lcom/smartcbwtf/mobile/viewmodel/LoginState;", "()V", "app_debug"})
    public static final class Success extends com.smartcbwtf.mobile.viewmodel.LoginState {
        @org.jetbrains.annotations.NotNull()
        public static final com.smartcbwtf.mobile.viewmodel.LoginState.Success INSTANCE = null;
        
        private Success() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000.\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\"\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\u00020\u0001B\u0013\u0012\f\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003\u00a2\u0006\u0002\u0010\u0005J\u000f\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003H\u00c6\u0003J\u0019\u0010\t\u001a\u00020\u00002\u000e\b\u0002\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003H\u00c6\u0001J\u0013\u0010\n\u001a\u00020\u000b2\b\u0010\f\u001a\u0004\u0018\u00010\rH\u00d6\u0003J\t\u0010\u000e\u001a\u00020\u000fH\u00d6\u0001J\t\u0010\u0010\u001a\u00020\u0011H\u00d6\u0001R\u0017\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u0007\u00a8\u0006\u0012"}, d2 = {"Lcom/smartcbwtf/mobile/viewmodel/LoginState$ValidationFailed;", "Lcom/smartcbwtf/mobile/viewmodel/LoginState;", "errors", "", "Lcom/smartcbwtf/mobile/viewmodel/LoginError;", "(Ljava/util/Set;)V", "getErrors", "()Ljava/util/Set;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "", "app_debug"})
    public static final class ValidationFailed extends com.smartcbwtf.mobile.viewmodel.LoginState {
        @org.jetbrains.annotations.NotNull()
        private final java.util.Set<com.smartcbwtf.mobile.viewmodel.LoginError> errors = null;
        
        public ValidationFailed(@org.jetbrains.annotations.NotNull()
        java.util.Set<? extends com.smartcbwtf.mobile.viewmodel.LoginError> errors) {
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.Set<com.smartcbwtf.mobile.viewmodel.LoginError> getErrors() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.Set<com.smartcbwtf.mobile.viewmodel.LoginError> component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.smartcbwtf.mobile.viewmodel.LoginState.ValidationFailed copy(@org.jetbrains.annotations.NotNull()
        java.util.Set<? extends com.smartcbwtf.mobile.viewmodel.LoginError> errors) {
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