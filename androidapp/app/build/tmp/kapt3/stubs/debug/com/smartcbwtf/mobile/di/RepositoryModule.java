package com.smartcbwtf.mobile.di;

@dagger.Module()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000,\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b\'\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006H\'J\u0010\u0010\u0007\u001a\u00020\b2\u0006\u0010\u0005\u001a\u00020\tH\'J\u0010\u0010\n\u001a\u00020\u000b2\u0006\u0010\u0005\u001a\u00020\fH\'\u00a8\u0006\r"}, d2 = {"Lcom/smartcbwtf/mobile/di/RepositoryModule;", "", "()V", "bindAuthRepository", "Lcom/smartcbwtf/mobile/repository/AuthRepository;", "repo", "Lcom/smartcbwtf/mobile/repository/DefaultAuthRepository;", "bindBagEventRepository", "Lcom/smartcbwtf/mobile/repository/BagEventRepository;", "Lcom/smartcbwtf/mobile/repository/DefaultBagEventRepository;", "bindHcfRepository", "Lcom/smartcbwtf/mobile/repository/HcfRepository;", "Lcom/smartcbwtf/mobile/repository/DefaultHcfRepository;", "app_debug"})
@dagger.hilt.InstallIn(value = {dagger.hilt.components.SingletonComponent.class})
public abstract class RepositoryModule {
    
    public RepositoryModule() {
        super();
    }
    
    @dagger.Binds()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public abstract com.smartcbwtf.mobile.repository.AuthRepository bindAuthRepository(@org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.repository.DefaultAuthRepository repo);
    
    @dagger.Binds()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public abstract com.smartcbwtf.mobile.repository.HcfRepository bindHcfRepository(@org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.repository.DefaultHcfRepository repo);
    
    @dagger.Binds()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public abstract com.smartcbwtf.mobile.repository.BagEventRepository bindBagEventRepository(@org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.repository.DefaultBagEventRepository repo);
}