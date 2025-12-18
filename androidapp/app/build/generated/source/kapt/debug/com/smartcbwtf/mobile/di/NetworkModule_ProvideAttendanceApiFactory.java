package com.smartcbwtf.mobile.di;

import com.smartcbwtf.mobile.network.api.AttendanceApi;
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
public final class NetworkModule_ProvideAttendanceApiFactory implements Factory<AttendanceApi> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideAttendanceApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public AttendanceApi get() {
    return provideAttendanceApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvideAttendanceApiFactory create(
      Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideAttendanceApiFactory(retrofitProvider);
  }

  public static AttendanceApi provideAttendanceApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideAttendanceApi(retrofit));
  }
}
