package com.smartcbwtf.mobile.di;

import com.smartcbwtf.mobile.network.api.VerificationApi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import retrofit2.Retrofit;

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
public final class NetworkModule_ProvideVerificationApiFactory implements Factory<VerificationApi> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideVerificationApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public VerificationApi get() {
    return provideVerificationApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvideVerificationApiFactory create(
      Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideVerificationApiFactory(retrofitProvider);
  }

  public static VerificationApi provideVerificationApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideVerificationApi(retrofit));
  }
}
