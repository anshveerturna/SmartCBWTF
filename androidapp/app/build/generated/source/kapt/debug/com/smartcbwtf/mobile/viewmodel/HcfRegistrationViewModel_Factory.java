package com.smartcbwtf.mobile.viewmodel;

import android.content.Context;
import androidx.lifecycle.SavedStateHandle;
import com.smartcbwtf.mobile.repository.HcfRepository;
import com.smartcbwtf.mobile.storage.SessionManager;
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
public final class HcfRegistrationViewModel_Factory implements Factory<HcfRegistrationViewModel> {
  private final Provider<SavedStateHandle> savedStateHandleProvider;

  private final Provider<Context> appContextProvider;

  private final Provider<HcfRepository> hcfRepositoryProvider;

  private final Provider<LocationHelper> locationHelperProvider;

  private final Provider<SessionManager> sessionManagerProvider;

  public HcfRegistrationViewModel_Factory(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<Context> appContextProvider, Provider<HcfRepository> hcfRepositoryProvider,
      Provider<LocationHelper> locationHelperProvider,
      Provider<SessionManager> sessionManagerProvider) {
    this.savedStateHandleProvider = savedStateHandleProvider;
    this.appContextProvider = appContextProvider;
    this.hcfRepositoryProvider = hcfRepositoryProvider;
    this.locationHelperProvider = locationHelperProvider;
    this.sessionManagerProvider = sessionManagerProvider;
  }

  @Override
  public HcfRegistrationViewModel get() {
    return newInstance(savedStateHandleProvider.get(), appContextProvider.get(), hcfRepositoryProvider.get(), locationHelperProvider.get(), sessionManagerProvider.get());
  }

  public static HcfRegistrationViewModel_Factory create(
      Provider<SavedStateHandle> savedStateHandleProvider, Provider<Context> appContextProvider,
      Provider<HcfRepository> hcfRepositoryProvider,
      Provider<LocationHelper> locationHelperProvider,
      Provider<SessionManager> sessionManagerProvider) {
    return new HcfRegistrationViewModel_Factory(savedStateHandleProvider, appContextProvider, hcfRepositoryProvider, locationHelperProvider, sessionManagerProvider);
  }

  public static HcfRegistrationViewModel newInstance(SavedStateHandle savedStateHandle,
      Context appContext, HcfRepository hcfRepository, LocationHelper locationHelper,
      SessionManager sessionManager) {
    return new HcfRegistrationViewModel(savedStateHandle, appContext, hcfRepository, locationHelper, sessionManager);
  }
}
