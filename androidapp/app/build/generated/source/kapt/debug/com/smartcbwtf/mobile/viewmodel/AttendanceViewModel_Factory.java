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
public final class AttendanceViewModel_Factory implements Factory<AttendanceViewModel> {
  private final Provider<LocationHelper> locationHelperProvider;

  private final Provider<HcfRepository> hcfRepositoryProvider;

  public AttendanceViewModel_Factory(Provider<LocationHelper> locationHelperProvider,
      Provider<HcfRepository> hcfRepositoryProvider) {
    this.locationHelperProvider = locationHelperProvider;
    this.hcfRepositoryProvider = hcfRepositoryProvider;
  }

  @Override
  public AttendanceViewModel get() {
    return newInstance(locationHelperProvider.get(), hcfRepositoryProvider.get());
  }

  public static AttendanceViewModel_Factory create(Provider<LocationHelper> locationHelperProvider,
      Provider<HcfRepository> hcfRepositoryProvider) {
    return new AttendanceViewModel_Factory(locationHelperProvider, hcfRepositoryProvider);
  }

  public static AttendanceViewModel newInstance(LocationHelper locationHelper,
      HcfRepository hcfRepository) {
    return new AttendanceViewModel(locationHelper, hcfRepository);
  }
}
