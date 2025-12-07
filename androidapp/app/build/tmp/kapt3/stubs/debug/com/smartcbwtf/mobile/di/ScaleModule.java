package com.smartcbwtf.mobile.di;

@dagger.Module()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000 \n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\b\'\u0018\u0000 \t2\u00020\u0001:\u0001\tB\u0005\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006H\'J\u0010\u0010\u0007\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\bH\'\u00a8\u0006\n"}, d2 = {"Lcom/smartcbwtf/mobile/di/ScaleModule;", "", "()V", "bindMockScaleService", "Lcom/smartcbwtf/mobile/bluetooth/ScaleService;", "service", "Lcom/smartcbwtf/mobile/bluetooth/MockScaleService;", "bindRealScaleService", "Lcom/smartcbwtf/mobile/bluetooth/RealBluetoothScaleService;", "Companion", "app_debug"})
@dagger.hilt.InstallIn(value = {dagger.hilt.components.SingletonComponent.class})
public abstract class ScaleModule {
    @org.jetbrains.annotations.NotNull()
    public static final com.smartcbwtf.mobile.di.ScaleModule.Companion Companion = null;
    
    public ScaleModule() {
        super();
    }
    
    @dagger.Binds()
    @javax.inject.Singleton()
    @com.smartcbwtf.mobile.bluetooth.MockScale()
    @org.jetbrains.annotations.NotNull()
    public abstract com.smartcbwtf.mobile.bluetooth.ScaleService bindMockScaleService(@org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.bluetooth.MockScaleService service);
    
    @dagger.Binds()
    @javax.inject.Singleton()
    @com.smartcbwtf.mobile.bluetooth.RealScale()
    @org.jetbrains.annotations.NotNull()
    public abstract com.smartcbwtf.mobile.bluetooth.ScaleService bindRealScaleService(@org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.bluetooth.RealBluetoothScaleService service);
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u001c\u0010\u0003\u001a\u00020\u00042\b\b\u0001\u0010\u0005\u001a\u00020\u00042\b\b\u0001\u0010\u0006\u001a\u00020\u0004H\u0007\u00a8\u0006\u0007"}, d2 = {"Lcom/smartcbwtf/mobile/di/ScaleModule$Companion;", "", "()V", "provideScaleService", "Lcom/smartcbwtf/mobile/bluetooth/ScaleService;", "real", "mock", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @dagger.Provides()
        @javax.inject.Singleton()
        @org.jetbrains.annotations.NotNull()
        public final com.smartcbwtf.mobile.bluetooth.ScaleService provideScaleService(@com.smartcbwtf.mobile.bluetooth.RealScale()
        @org.jetbrains.annotations.NotNull()
        com.smartcbwtf.mobile.bluetooth.ScaleService real, @com.smartcbwtf.mobile.bluetooth.MockScale()
        @org.jetbrains.annotations.NotNull()
        com.smartcbwtf.mobile.bluetooth.ScaleService mock) {
            return null;
        }
    }
}