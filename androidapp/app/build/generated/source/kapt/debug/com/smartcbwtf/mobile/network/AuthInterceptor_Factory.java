package com.smartcbwtf.mobile.network;

import com.smartcbwtf.mobile.storage.AuthTokenStore;
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
public final class AuthInterceptor_Factory implements Factory<AuthInterceptor> {
  private final Provider<AuthTokenStore> tokenStoreProvider;

  public AuthInterceptor_Factory(Provider<AuthTokenStore> tokenStoreProvider) {
    this.tokenStoreProvider = tokenStoreProvider;
  }

  @Override
  public AuthInterceptor get() {
    return newInstance(tokenStoreProvider.get());
  }

  public static AuthInterceptor_Factory create(Provider<AuthTokenStore> tokenStoreProvider) {
    return new AuthInterceptor_Factory(tokenStoreProvider);
  }

  public static AuthInterceptor newInstance(AuthTokenStore tokenStore) {
    return new AuthInterceptor(tokenStore);
  }
}
