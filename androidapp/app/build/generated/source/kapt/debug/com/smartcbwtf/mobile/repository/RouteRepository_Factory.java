package com.smartcbwtf.mobile.repository;

import com.smartcbwtf.mobile.network.api.RouteApi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class RouteRepository_Factory implements Factory<RouteRepository> {
  private final Provider<RouteApi> routeApiProvider;

  public RouteRepository_Factory(Provider<RouteApi> routeApiProvider) {
    this.routeApiProvider = routeApiProvider;
  }

  @Override
  public RouteRepository get() {
    return newInstance(routeApiProvider.get());
  }

  public static RouteRepository_Factory create(Provider<RouteApi> routeApiProvider) {
    return new RouteRepository_Factory(routeApiProvider);
  }

  public static RouteRepository newInstance(RouteApi routeApi) {
    return new RouteRepository(routeApi);
  }
}
