package com.smartcbwtf.mobile.service;

import com.smartcbwtf.mobile.repository.LocationRepository;
import com.smartcbwtf.mobile.storage.AppConfigStore;
import com.smartcbwtf.mobile.storage.AuthTokenStore;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class ForegroundLocationService_MembersInjector implements MembersInjector<ForegroundLocationService> {
  private final Provider<LocationRepository> locationRepositoryProvider;

  private final Provider<AuthTokenStore> authTokenStoreProvider;

  private final Provider<AppConfigStore> appConfigStoreProvider;

  public ForegroundLocationService_MembersInjector(
      Provider<LocationRepository> locationRepositoryProvider,
      Provider<AuthTokenStore> authTokenStoreProvider,
      Provider<AppConfigStore> appConfigStoreProvider) {
    this.locationRepositoryProvider = locationRepositoryProvider;
    this.authTokenStoreProvider = authTokenStoreProvider;
    this.appConfigStoreProvider = appConfigStoreProvider;
  }

  public static MembersInjector<ForegroundLocationService> create(
      Provider<LocationRepository> locationRepositoryProvider,
      Provider<AuthTokenStore> authTokenStoreProvider,
      Provider<AppConfigStore> appConfigStoreProvider) {
    return new ForegroundLocationService_MembersInjector(locationRepositoryProvider, authTokenStoreProvider, appConfigStoreProvider);
  }

  @Override
  public void injectMembers(ForegroundLocationService instance) {
    injectLocationRepository(instance, locationRepositoryProvider.get());
    injectAuthTokenStore(instance, authTokenStoreProvider.get());
    injectAppConfigStore(instance, appConfigStoreProvider.get());
  }

  @InjectedFieldSignature("com.smartcbwtf.mobile.service.ForegroundLocationService.locationRepository")
  public static void injectLocationRepository(ForegroundLocationService instance,
      LocationRepository locationRepository) {
    instance.locationRepository = locationRepository;
  }

  @InjectedFieldSignature("com.smartcbwtf.mobile.service.ForegroundLocationService.authTokenStore")
  public static void injectAuthTokenStore(ForegroundLocationService instance,
      AuthTokenStore authTokenStore) {
    instance.authTokenStore = authTokenStore;
  }

  @InjectedFieldSignature("com.smartcbwtf.mobile.service.ForegroundLocationService.appConfigStore")
  public static void injectAppConfigStore(ForegroundLocationService instance,
      AppConfigStore appConfigStore) {
    instance.appConfigStore = appConfigStore;
  }
}
