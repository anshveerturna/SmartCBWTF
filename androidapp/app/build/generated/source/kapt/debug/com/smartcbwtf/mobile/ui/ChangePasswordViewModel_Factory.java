package com.smartcbwtf.mobile.ui;

import com.smartcbwtf.mobile.network.api.ProfileApi;
import com.smartcbwtf.mobile.repository.AuthRepository;
import com.smartcbwtf.mobile.storage.AuthTokenStore;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class ChangePasswordViewModel_Factory implements Factory<ChangePasswordViewModel> {
  private final Provider<ProfileApi> profileApiProvider;

  private final Provider<AuthTokenStore> authTokenStoreProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  public ChangePasswordViewModel_Factory(Provider<ProfileApi> profileApiProvider,
      Provider<AuthTokenStore> authTokenStoreProvider,
      Provider<AuthRepository> authRepositoryProvider) {
    this.profileApiProvider = profileApiProvider;
    this.authTokenStoreProvider = authTokenStoreProvider;
    this.authRepositoryProvider = authRepositoryProvider;
  }

  @Override
  public ChangePasswordViewModel get() {
    return newInstance(profileApiProvider.get(), authTokenStoreProvider.get(), authRepositoryProvider.get());
  }

  public static ChangePasswordViewModel_Factory create(Provider<ProfileApi> profileApiProvider,
      Provider<AuthTokenStore> authTokenStoreProvider,
      Provider<AuthRepository> authRepositoryProvider) {
    return new ChangePasswordViewModel_Factory(profileApiProvider, authTokenStoreProvider, authRepositoryProvider);
  }

  public static ChangePasswordViewModel newInstance(ProfileApi profileApi,
      AuthTokenStore authTokenStore, AuthRepository authRepository) {
    return new ChangePasswordViewModel(profileApi, authTokenStore, authRepository);
  }
}
