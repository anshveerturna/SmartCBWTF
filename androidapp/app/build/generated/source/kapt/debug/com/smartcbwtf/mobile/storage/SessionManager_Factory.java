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
public final class SessionManager_Factory implements Factory<SessionManager> {
  private final Provider<SharedPreferences> prefsProvider;

  public SessionManager_Factory(Provider<SharedPreferences> prefsProvider) {
    this.prefsProvider = prefsProvider;
  }

  @Override
  public SessionManager get() {
    return newInstance(prefsProvider.get());
  }

  public static SessionManager_Factory create(Provider<SharedPreferences> prefsProvider) {
    return new SessionManager_Factory(prefsProvider);
  }

  public static SessionManager newInstance(SharedPreferences prefs) {
    return new SessionManager(prefs);
  }
}
