package com.smartcbwtf.mobile.viewmodel;

import com.smartcbwtf.mobile.repository.RouteRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class MyRouteViewModel_Factory implements Factory<MyRouteViewModel> {
  private final Provider<RouteRepository> routeRepositoryProvider;

  public MyRouteViewModel_Factory(Provider<RouteRepository> routeRepositoryProvider) {
    this.routeRepositoryProvider = routeRepositoryProvider;
  }

  @Override
  public MyRouteViewModel get() {
    return newInstance(routeRepositoryProvider.get());
  }

  public static MyRouteViewModel_Factory create(Provider<RouteRepository> routeRepositoryProvider) {
    return new MyRouteViewModel_Factory(routeRepositoryProvider);
  }

  public static MyRouteViewModel newInstance(RouteRepository routeRepository) {
    return new MyRouteViewModel(routeRepository);
  }
}
