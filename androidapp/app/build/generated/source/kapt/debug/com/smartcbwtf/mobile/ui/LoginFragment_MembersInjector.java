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
public final class LoginFragment_MembersInjector implements MembersInjector<LoginFragment> {
  private final Provider<SharedPreferences> sharedPreferencesProvider;

  private final Provider<LocationRepository> locationRepositoryProvider;

  public LoginFragment_MembersInjector(Provider<SharedPreferences> sharedPreferencesProvider,
      Provider<LocationRepository> locationRepositoryProvider) {
    this.sharedPreferencesProvider = sharedPreferencesProvider;
    this.locationRepositoryProvider = locationRepositoryProvider;
  }

  public static MembersInjector<LoginFragment> create(
      Provider<SharedPreferences> sharedPreferencesProvider,
      Provider<LocationRepository> locationRepositoryProvider) {
    return new LoginFragment_MembersInjector(sharedPreferencesProvider, locationRepositoryProvider);
  }

  @Override
  public void injectMembers(LoginFragment instance) {
    injectSharedPreferences(instance, sharedPreferencesProvider.get());
    injectLocationRepository(instance, locationRepositoryProvider.get());
  }

  @InjectedFieldSignature("com.smartcbwtf.mobile.ui.LoginFragment.sharedPreferences")
  public static void injectSharedPreferences(LoginFragment instance,
      SharedPreferences sharedPreferences) {
    instance.sharedPreferences = sharedPreferences;
  }

  @InjectedFieldSignature("com.smartcbwtf.mobile.ui.LoginFragment.locationRepository")
  public static void injectLocationRepository(LoginFragment instance,
      LocationRepository locationRepository) {
    instance.locationRepository = locationRepository;
  }
}
