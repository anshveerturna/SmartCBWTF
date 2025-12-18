package com.smartcbwtf.mobile.storage;

import android.content.SharedPreferences;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class DefaultAuthTokenStore_Factory implements Factory<DefaultAuthTokenStore> {
  private final Provider<SharedPreferences> prefsProvider;

  public DefaultAuthTokenStore_Factory(Provider<SharedPreferences> prefsProvider) {
    this.prefsProvider = prefsProvider;
  }

  @Override
  public DefaultAuthTokenStore get() {
    return newInstance(prefsProvider.get());
  }

  public static DefaultAuthTokenStore_Factory create(Provider<SharedPreferences> prefsProvider) {
    return new DefaultAuthTokenStore_Factory(prefsProvider);
  }

  public static DefaultAuthTokenStore newInstance(SharedPreferences prefs) {
    return new DefaultAuthTokenStore(prefs);
  }
}
