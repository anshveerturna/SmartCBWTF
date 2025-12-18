package com.smartcbwtf.mobile.viewmodel;

import com.smartcbwtf.mobile.bluetooth.ScaleService;
import com.smartcbwtf.mobile.network.api.VerificationApi;
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
public final class ScanWeighViewModel_Factory implements Factory<ScanWeighViewModel> {
  private final Provider<ScaleService> scaleServiceProvider;

  private final Provider<BagEventRepository> bagEventRepositoryProvider;

  private final Provider<LocationHelper> locationHelperProvider;

  private final Provider<VerificationApi> verificationApiProvider;

  private final Provider<SessionManager> sessionManagerProvider;

  public ScanWeighViewModel_Factory(Provider<ScaleService> scaleServiceProvider,
      Provider<BagEventRepository> bagEventRepositoryProvider,
      Provider<LocationHelper> locationHelperProvider,
      Provider<VerificationApi> verificationApiProvider,
      Provider<SessionManager> sessionManagerProvider) {
    this.scaleServiceProvider = scaleServiceProvider;
    this.bagEventRepositoryProvider = bagEventRepositoryProvider;
    this.locationHelperProvider = locationHelperProvider;
    this.verificationApiProvider = verificationApiProvider;
    this.sessionManagerProvider = sessionManagerProvider;
  }

  @Override
  public ScanWeighViewModel get() {
    return newInstance(scaleServiceProvider.get(), bagEventRepositoryProvider.get(), locationHelperProvider.get(), verificationApiProvider.get(), sessionManagerProvider.get());
  }

  public static ScanWeighViewModel_Factory create(Provider<ScaleService> scaleServiceProvider,
      Provider<BagEventRepository> bagEventRepositoryProvider,
      Provider<LocationHelper> locationHelperProvider,
      Provider<VerificationApi> verificationApiProvider,
      Provider<SessionManager> sessionManagerProvider) {
    return new ScanWeighViewModel_Factory(scaleServiceProvider, bagEventRepositoryProvider, locationHelperProvider, verificationApiProvider, sessionManagerProvider);
  }

  public static ScanWeighViewModel newInstance(ScaleService scaleService,
      BagEventRepository bagEventRepository, LocationHelper locationHelper,
      VerificationApi verificationApi, SessionManager sessionManager) {
    return new ScanWeighViewModel(scaleService, bagEventRepository, locationHelper, verificationApi, sessionManager);
  }
}
