package com.smartcbwtf.mobile.ui;

import com.smartcbwtf.mobile.repository.LocationRepository;
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
public final class LocationDisclosureFragment_MembersInjector implements MembersInjector<LocationDisclosureFragment> {
  private final Provider<LocationRepository> locationRepositoryProvider;

  public LocationDisclosureFragment_MembersInjector(
      Provider<LocationRepository> locationRepositoryProvider) {
    this.locationRepositoryProvider = locationRepositoryProvider;
  }

  public static MembersInjector<LocationDisclosureFragment> create(
      Provider<LocationRepository> locationRepositoryProvider) {
    return new LocationDisclosureFragment_MembersInjector(locationRepositoryProvider);
  }

  @Override
  public void injectMembers(LocationDisclosureFragment instance) {
    injectLocationRepository(instance, locationRepositoryProvider.get());
  }

  @InjectedFieldSignature("com.smartcbwtf.mobile.ui.LocationDisclosureFragment.locationRepository")
  public static void injectLocationRepository(LocationDisclosureFragment instance,
      LocationRepository locationRepository) {
    instance.locationRepository = locationRepository;
  }
}
