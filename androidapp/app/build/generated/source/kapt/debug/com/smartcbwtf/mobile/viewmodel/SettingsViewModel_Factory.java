package com.smartcbwtf.mobile.viewmodel;

import com.smartcbwtf.mobile.bluetooth.ScaleService;
import com.smartcbwtf.mobile.repository.AuthRepository;
import com.smartcbwtf.mobile.repository.BagEventRepository;
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<BagEventRepository> bagEventRepositoryProvider;

  private final Provider<ScaleService> scaleServiceProvider;

  public SettingsViewModel_Factory(Provider<AuthRepository> authRepositoryProvider,
      Provider<BagEventRepository> bagEventRepositoryProvider,
      Provider<ScaleService> scaleServiceProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.bagEventRepositoryProvider = bagEventRepositoryProvider;
    this.scaleServiceProvider = scaleServiceProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(authRepositoryProvider.get(), bagEventRepositoryProvider.get(), scaleServiceProvider.get());
  }

  public static SettingsViewModel_Factory create(Provider<AuthRepository> authRepositoryProvider,
      Provider<BagEventRepository> bagEventRepositoryProvider,
      Provider<ScaleService> scaleServiceProvider) {
    return new SettingsViewModel_Factory(authRepositoryProvider, bagEventRepositoryProvider, scaleServiceProvider);
  }

  public static SettingsViewModel newInstance(AuthRepository authRepository,
      BagEventRepository bagEventRepository, ScaleService scaleService) {
    return new SettingsViewModel(authRepository, bagEventRepository, scaleService);
  }
}
