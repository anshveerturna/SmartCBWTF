package com.smartcbwtf.mobile.di;

import com.smartcbwtf.mobile.network.api.BagEventApi;
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
public final class NetworkModule_ProvideBagEventApiFactory implements Factory<BagEventApi> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideBagEventApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public BagEventApi get() {
    return provideBagEventApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvideBagEventApiFactory create(
      Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideBagEventApiFactory(retrofitProvider);
  }

  public static BagEventApi provideBagEventApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideBagEventApi(retrofit));
  }
}
