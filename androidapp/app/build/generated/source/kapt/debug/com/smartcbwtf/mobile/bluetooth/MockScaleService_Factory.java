package com.smartcbwtf.mobile.bluetooth;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class MockScaleService_Factory implements Factory<MockScaleService> {
  @Override
  public MockScaleService get() {
    return newInstance();
  }

  public static MockScaleService_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static MockScaleService newInstance() {
    return new MockScaleService();
  }

  private static final class InstanceHolder {
    private static final MockScaleService_Factory INSTANCE = new MockScaleService_Factory();
  }
}
