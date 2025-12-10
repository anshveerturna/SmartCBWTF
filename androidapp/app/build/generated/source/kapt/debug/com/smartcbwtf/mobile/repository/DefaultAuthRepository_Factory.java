package com.smartcbwtf.mobile.repository;

import com.smartcbwtf.mobile.network.api.AuthApi;
import com.smartcbwtf.mobile.storage.AuthTokenStore;
import com.smartcbwtf.mobile.utils.NetworkMonitor;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import kotlinx.coroutines.CoroutineDispatcher;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class DefaultAuthRepository_Factory implements Factory<DefaultAuthRepository> {
  private final Provider<AuthApi> apiProvider;

  private final Provider<AuthTokenStore> tokenStoreProvider;

  private final Provider<NetworkMonitor> networkMonitorProvider;

  private final Provider<CoroutineDispatcher> ioDispatcherProvider;

  public DefaultAuthRepository_Factory(Provider<AuthApi> apiProvider,
      Provider<AuthTokenStore> tokenStoreProvider, Provider<NetworkMonitor> networkMonitorProvider,
      Provider<CoroutineDispatcher> ioDispatcherProvider) {
    this.apiProvider = apiProvider;
    this.tokenStoreProvider = tokenStoreProvider;
    this.networkMonitorProvider = networkMonitorProvider;
    this.ioDispatcherProvider = ioDispatcherProvider;
  }

  @Override
  public DefaultAuthRepository get() {
    return newInstance(apiProvider.get(), tokenStoreProvider.get(), networkMonitorProvider.get(), ioDispatcherProvider.get());
  }

  public static DefaultAuthRepository_Factory create(Provider<AuthApi> apiProvider,
      Provider<AuthTokenStore> tokenStoreProvider, Provider<NetworkMonitor> networkMonitorProvider,
      Provider<CoroutineDispatcher> ioDispatcherProvider) {
    return new DefaultAuthRepository_Factory(apiProvider, tokenStoreProvider, networkMonitorProvider, ioDispatcherProvider);
  }

  public static DefaultAuthRepository newInstance(AuthApi api, AuthTokenStore tokenStore,
      NetworkMonitor networkMonitor, CoroutineDispatcher ioDispatcher) {
    return new DefaultAuthRepository(api, tokenStore, networkMonitor, ioDispatcher);
  }
}
