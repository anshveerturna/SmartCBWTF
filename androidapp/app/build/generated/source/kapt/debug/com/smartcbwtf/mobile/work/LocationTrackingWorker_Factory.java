package com.smartcbwtf.mobile.work;

import android.content.Context;
import androidx.work.WorkerParameters;
import com.smartcbwtf.mobile.repository.LocationRepository;
import com.smartcbwtf.mobile.storage.AppConfigStore;
import com.smartcbwtf.mobile.storage.AuthTokenStore;
import com.smartcbwtf.mobile.utils.LocationHelper;
import dagger.internal.DaggerGenerated;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class LocationTrackingWorker_Factory {
  private final Provider<LocationRepository> locationRepositoryProvider;

  private final Provider<LocationHelper> locationHelperProvider;

  private final Provider<AuthTokenStore> authTokenStoreProvider;

  private final Provider<AppConfigStore> appConfigStoreProvider;

  public LocationTrackingWorker_Factory(Provider<LocationRepository> locationRepositoryProvider,
      Provider<LocationHelper> locationHelperProvider,
      Provider<AuthTokenStore> authTokenStoreProvider,
      Provider<AppConfigStore> appConfigStoreProvider) {
    this.locationRepositoryProvider = locationRepositoryProvider;
    this.locationHelperProvider = locationHelperProvider;
    this.authTokenStoreProvider = authTokenStoreProvider;
    this.appConfigStoreProvider = appConfigStoreProvider;
  }

  public LocationTrackingWorker get(Context appContext, WorkerParameters params) {
    return newInstance(appContext, params, locationRepositoryProvider.get(), locationHelperProvider.get(), authTokenStoreProvider.get(), appConfigStoreProvider.get());
  }

  public static LocationTrackingWorker_Factory create(
      Provider<LocationRepository> locationRepositoryProvider,
      Provider<LocationHelper> locationHelperProvider,
      Provider<AuthTokenStore> authTokenStoreProvider,
      Provider<AppConfigStore> appConfigStoreProvider) {
    return new LocationTrackingWorker_Factory(locationRepositoryProvider, locationHelperProvider, authTokenStoreProvider, appConfigStoreProvider);
  }

  public static LocationTrackingWorker newInstance(Context appContext, WorkerParameters params,
      LocationRepository locationRepository, LocationHelper locationHelper,
      AuthTokenStore authTokenStore, AppConfigStore appConfigStore) {
    return new LocationTrackingWorker(appContext, params, locationRepository, locationHelper, authTokenStore, appConfigStore);
  }
}
