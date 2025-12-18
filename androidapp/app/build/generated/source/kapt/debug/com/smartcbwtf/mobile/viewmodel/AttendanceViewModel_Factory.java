package com.smartcbwtf.mobile.viewmodel;

import android.content.Context;
import com.smartcbwtf.mobile.repository.AttendanceRepository;
import com.smartcbwtf.mobile.repository.HcfRepository;
import com.smartcbwtf.mobile.utils.LocationHelper;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class AttendanceViewModel_Factory implements Factory<AttendanceViewModel> {
  private final Provider<Context> appContextProvider;

  private final Provider<LocationHelper> locationHelperProvider;

  private final Provider<HcfRepository> hcfRepositoryProvider;

  private final Provider<AttendanceRepository> attendanceRepositoryProvider;

  public AttendanceViewModel_Factory(Provider<Context> appContextProvider,
      Provider<LocationHelper> locationHelperProvider,
      Provider<HcfRepository> hcfRepositoryProvider,
      Provider<AttendanceRepository> attendanceRepositoryProvider) {
    this.appContextProvider = appContextProvider;
    this.locationHelperProvider = locationHelperProvider;
    this.hcfRepositoryProvider = hcfRepositoryProvider;
    this.attendanceRepositoryProvider = attendanceRepositoryProvider;
  }

  @Override
  public AttendanceViewModel get() {
    return newInstance(appContextProvider.get(), locationHelperProvider.get(), hcfRepositoryProvider.get(), attendanceRepositoryProvider.get());
  }

  public static AttendanceViewModel_Factory create(Provider<Context> appContextProvider,
      Provider<LocationHelper> locationHelperProvider,
      Provider<HcfRepository> hcfRepositoryProvider,
      Provider<AttendanceRepository> attendanceRepositoryProvider) {
    return new AttendanceViewModel_Factory(appContextProvider, locationHelperProvider, hcfRepositoryProvider, attendanceRepositoryProvider);
  }

  public static AttendanceViewModel newInstance(Context appContext, LocationHelper locationHelper,
      HcfRepository hcfRepository, AttendanceRepository attendanceRepository) {
    return new AttendanceViewModel(appContext, locationHelper, hcfRepository, attendanceRepository);
  }
}
