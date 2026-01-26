package com.smartcbwtf.mobile.viewmodel;

import com.smartcbwtf.mobile.bluetooth.ScaleService;
import com.smartcbwtf.mobile.repository.BagEventRepository;
import com.smartcbwtf.mobile.storage.SessionManager;
import com.smartcbwtf.mobile.utils.LocationHelper;
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
public final class VerifyAtPlantViewModel_Factory implements Factory<VerifyAtPlantViewModel> {
  private final Provider<ScaleService> scaleServiceProvider;

  private final Provider<BagEventRepository> bagEventRepositoryProvider;

  private final Provider<LocationHelper> locationHelperProvider;

  private final Provider<SessionManager> sessionManagerProvider;

  public VerifyAtPlantViewModel_Factory(Provider<ScaleService> scaleServiceProvider,
      Provider<BagEventRepository> bagEventRepositoryProvider,
      Provider<LocationHelper> locationHelperProvider,
      Provider<SessionManager> sessionManagerProvider) {
    this.scaleServiceProvider = scaleServiceProvider;
    this.bagEventRepositoryProvider = bagEventRepositoryProvider;
    this.locationHelperProvider = locationHelperProvider;
    this.sessionManagerProvider = sessionManagerProvider;
  }

  @Override
  public VerifyAtPlantViewModel get() {
    return newInstance(scaleServiceProvider.get(), bagEventRepositoryProvider.get(), locationHelperProvider.get(), sessionManagerProvider.get());
  }

  public static VerifyAtPlantViewModel_Factory create(Provider<ScaleService> scaleServiceProvider,
      Provider<BagEventRepository> bagEventRepositoryProvider,
      Provider<LocationHelper> locationHelperProvider,
      Provider<SessionManager> sessionManagerProvider) {
    return new VerifyAtPlantViewModel_Factory(scaleServiceProvider, bagEventRepositoryProvider, locationHelperProvider, sessionManagerProvider);
  }

  public static VerifyAtPlantViewModel newInstance(ScaleService scaleService,
      BagEventRepository bagEventRepository, LocationHelper locationHelper,
      SessionManager sessionManager) {
    return new VerifyAtPlantViewModel(scaleService, bagEventRepository, locationHelper, sessionManager);
  }
}
