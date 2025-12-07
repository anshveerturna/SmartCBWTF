package com.smartcbwtf.mobile.di;

import com.smartcbwtf.mobile.bluetooth.ScaleService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata({
    "com.smartcbwtf.mobile.bluetooth.RealScale",
    "com.smartcbwtf.mobile.bluetooth.MockScale"
})
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
public final class ScaleModule_Companion_ProvideScaleServiceFactory implements Factory<ScaleService> {
  private final Provider<ScaleService> realProvider;

  private final Provider<ScaleService> mockProvider;

  public ScaleModule_Companion_ProvideScaleServiceFactory(Provider<ScaleService> realProvider,
      Provider<ScaleService> mockProvider) {
    this.realProvider = realProvider;
    this.mockProvider = mockProvider;
  }

  @Override
  public ScaleService get() {
    return provideScaleService(realProvider.get(), mockProvider.get());
  }

  public static ScaleModule_Companion_ProvideScaleServiceFactory create(
      Provider<ScaleService> realProvider, Provider<ScaleService> mockProvider) {
    return new ScaleModule_Companion_ProvideScaleServiceFactory(realProvider, mockProvider);
  }

  public static ScaleService provideScaleService(ScaleService real, ScaleService mock) {
    return Preconditions.checkNotNullFromProvides(ScaleModule.Companion.provideScaleService(real, mock));
  }
}
