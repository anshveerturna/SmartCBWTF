package com.smartcbwtf.mobile.ui;

import android.content.SharedPreferences;
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

  public PermissionsFragment_MembersInjector(
      Provider<SharedPreferences> sharedPreferencesProvider) {
    this.sharedPreferencesProvider = sharedPreferencesProvider;
  }

  public static MembersInjector<PermissionsFragment> create(
      Provider<SharedPreferences> sharedPreferencesProvider) {
    return new PermissionsFragment_MembersInjector(sharedPreferencesProvider);
  }

  @Override
  public void injectMembers(PermissionsFragment instance) {
    injectSharedPreferences(instance, sharedPreferencesProvider.get());
  }

  @InjectedFieldSignature("com.smartcbwtf.mobile.ui.PermissionsFragment.sharedPreferences")
  public static void injectSharedPreferences(PermissionsFragment instance,
      SharedPreferences sharedPreferences) {
    instance.sharedPreferences = sharedPreferences;
  }
}
