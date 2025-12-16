package com.smartcbwtf.mobile.ui;

import com.smartcbwtf.mobile.utils.LocationHelper;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class HomeFragment_MembersInjector implements MembersInjector<HomeFragment> {
  private final Provider<LocationHelper> locationHelperProvider;

  public HomeFragment_MembersInjector(Provider<LocationHelper> locationHelperProvider) {
    this.locationHelperProvider = locationHelperProvider;
  }

  public static MembersInjector<HomeFragment> create(
      Provider<LocationHelper> locationHelperProvider) {
    return new HomeFragment_MembersInjector(locationHelperProvider);
  }

  @Override
  public void injectMembers(HomeFragment instance) {
    injectLocationHelper(instance, locationHelperProvider.get());
  }

  @InjectedFieldSignature("com.smartcbwtf.mobile.ui.HomeFragment.locationHelper")
  public static void injectLocationHelper(HomeFragment instance, LocationHelper locationHelper) {
    instance.locationHelper = locationHelper;
  }
}
