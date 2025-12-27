package com.smartcbwtf.mobile.repository;

import android.content.Context;
import com.smartcbwtf.mobile.network.api.LocationApi;
import com.smartcbwtf.mobile.storage.AuthTokenStore;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class DefaultLocationRepository_Factory implements Factory<DefaultLocationRepository> {
  private final Provider<Context> contextProvider;

  private final Provider<LocationApi> locationApiProvider;

  private final Provider<AuthTokenStore> authTokenStoreProvider;

  public DefaultLocationRepository_Factory(Provider<Context> contextProvider,
      Provider<LocationApi> locationApiProvider, Provider<AuthTokenStore> authTokenStoreProvider) {
    this.contextProvider = contextProvider;
    this.locationApiProvider = locationApiProvider;
    this.authTokenStoreProvider = authTokenStoreProvider;
  }

  @Override
  public DefaultLocationRepository get() {
    return newInstance(contextProvider.get(), locationApiProvider.get(), authTokenStoreProvider.get());
  }

  public static DefaultLocationRepository_Factory create(Provider<Context> contextProvider,
      Provider<LocationApi> locationApiProvider, Provider<AuthTokenStore> authTokenStoreProvider) {
    return new DefaultLocationRepository_Factory(contextProvider, locationApiProvider, authTokenStoreProvider);
  }

  public static DefaultLocationRepository newInstance(Context context, LocationApi locationApi,
      AuthTokenStore authTokenStore) {
    return new DefaultLocationRepository(context, locationApi, authTokenStore);
  }
}
