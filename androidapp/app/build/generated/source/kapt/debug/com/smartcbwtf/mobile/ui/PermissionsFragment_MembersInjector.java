package com.smartcbwtf.mobile.ui;

import android.content.SharedPreferences;
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
public final class PermissionsFragment_MembersInjector implements MembersInjector<PermissionsFragment> {
  private final Provider<SharedPreferences> sharedPreferencesProvider;

  private final Provider<LocationRepository> locationRepositoryProvider;

  public PermissionsFragment_MembersInjector(Provider<SharedPreferences> sharedPreferencesProvider,
      Provider<LocationRepository> locationRepositoryProvider) {
    this.sharedPreferencesProvider = sharedPreferencesProvider;
    this.locationRepositoryProvider = locationRepositoryProvider;
  }

  public static MembersInjector<PermissionsFragment> create(
      Provider<SharedPreferences> sharedPreferencesProvider,
      Provider<LocationRepository> locationRepositoryProvider) {
    return new PermissionsFragment_MembersInjector(sharedPreferencesProvider, locationRepositoryProvider);
  }

  @Override
  public void injectMembers(PermissionsFragment instance) {
    injectSharedPreferences(instance, sharedPreferencesProvider.get());
    injectLocationRepository(instance, locationRepositoryProvider.get());
  }

  @InjectedFieldSignature("com.smartcbwtf.mobile.ui.PermissionsFragment.sharedPreferences")
  public static void injectSharedPreferences(PermissionsFragment instance,
      SharedPreferences sharedPreferences) {
    instance.sharedPreferences = sharedPreferences;
  }

  @InjectedFieldSignature("com.smartcbwtf.mobile.ui.PermissionsFragment.locationRepository")
  public static void injectLocationRepository(PermissionsFragment instance,
      LocationRepository locationRepository) {
    instance.locationRepository = locationRepository;
  }
}
