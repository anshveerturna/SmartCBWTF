package com.smartcbwtf.mobile.di;

import com.smartcbwtf.mobile.network.api.HcfApi;
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
public final class NetworkModule_ProvideHcfApiFactory implements Factory<HcfApi> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideHcfApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public HcfApi get() {
    return provideHcfApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvideHcfApiFactory create(Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideHcfApiFactory(retrofitProvider);
  }

  public static HcfApi provideHcfApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideHcfApi(retrofit));
  }
}
