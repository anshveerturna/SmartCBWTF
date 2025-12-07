package com.smartcbwtf.mobile.viewmodel;

import com.smartcbwtf.mobile.repository.HcfRepository;
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
public final class HcfRegistrationViewModel_Factory implements Factory<HcfRegistrationViewModel> {
  private final Provider<HcfRepository> hcfRepositoryProvider;

  private final Provider<LocationHelper> locationHelperProvider;

  public HcfRegistrationViewModel_Factory(Provider<HcfRepository> hcfRepositoryProvider,
      Provider<LocationHelper> locationHelperProvider) {
    this.hcfRepositoryProvider = hcfRepositoryProvider;
    this.locationHelperProvider = locationHelperProvider;
  }

  @Override
  public HcfRegistrationViewModel get() {
    return newInstance(hcfRepositoryProvider.get(), locationHelperProvider.get());
  }

  public static HcfRegistrationViewModel_Factory create(
      Provider<HcfRepository> hcfRepositoryProvider,
      Provider<LocationHelper> locationHelperProvider) {
    return new HcfRegistrationViewModel_Factory(hcfRepositoryProvider, locationHelperProvider);
  }

  public static HcfRegistrationViewModel newInstance(HcfRepository hcfRepository,
      LocationHelper locationHelper) {
    return new HcfRegistrationViewModel(hcfRepository, locationHelper);
  }
}
