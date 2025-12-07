package com.smartcbwtf.mobile.viewmodel;

import com.smartcbwtf.mobile.repository.AuthRepository;
import com.smartcbwtf.mobile.repository.BagEventRepository;
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
public final class HomeViewModel_Factory implements Factory<HomeViewModel> {
  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<BagEventRepository> bagEventRepositoryProvider;

  public HomeViewModel_Factory(Provider<AuthRepository> authRepositoryProvider,
      Provider<BagEventRepository> bagEventRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.bagEventRepositoryProvider = bagEventRepositoryProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(authRepositoryProvider.get(), bagEventRepositoryProvider.get());
  }

  public static HomeViewModel_Factory create(Provider<AuthRepository> authRepositoryProvider,
      Provider<BagEventRepository> bagEventRepositoryProvider) {
    return new HomeViewModel_Factory(authRepositoryProvider, bagEventRepositoryProvider);
  }

  public static HomeViewModel newInstance(AuthRepository authRepository,
      BagEventRepository bagEventRepository) {
    return new HomeViewModel(authRepository, bagEventRepository);
  }
}
