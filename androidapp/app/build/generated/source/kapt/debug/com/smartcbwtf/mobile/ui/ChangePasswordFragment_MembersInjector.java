package com.smartcbwtf.mobile.ui;

import com.smartcbwtf.mobile.repository.AuthRepository;
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
public final class ChangePasswordFragment_MembersInjector implements MembersInjector<ChangePasswordFragment> {
  private final Provider<AuthRepository> authRepositoryProvider;

  public ChangePasswordFragment_MembersInjector(Provider<AuthRepository> authRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
  }

  public static MembersInjector<ChangePasswordFragment> create(
      Provider<AuthRepository> authRepositoryProvider) {
    return new ChangePasswordFragment_MembersInjector(authRepositoryProvider);
  }

  @Override
  public void injectMembers(ChangePasswordFragment instance) {
    injectAuthRepository(instance, authRepositoryProvider.get());
  }

  @InjectedFieldSignature("com.smartcbwtf.mobile.ui.ChangePasswordFragment.authRepository")
  public static void injectAuthRepository(ChangePasswordFragment instance,
      AuthRepository authRepository) {
    instance.authRepository = authRepository;
  }
}
