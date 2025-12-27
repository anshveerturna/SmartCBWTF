package com.smartcbwtf.mobile.work;

import android.content.Context;
import androidx.work.WorkerParameters;
import dagger.internal.DaggerGenerated;
import dagger.internal.InstanceFactory;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class LocationTrackingWorker_AssistedFactory_Impl implements LocationTrackingWorker_AssistedFactory {
  private final LocationTrackingWorker_Factory delegateFactory;

  LocationTrackingWorker_AssistedFactory_Impl(LocationTrackingWorker_Factory delegateFactory) {
    this.delegateFactory = delegateFactory;
  }

  @Override
  public LocationTrackingWorker create(Context arg0, WorkerParameters arg1) {
    return delegateFactory.get(arg0, arg1);
  }

  public static Provider<LocationTrackingWorker_AssistedFactory> create(
      LocationTrackingWorker_Factory delegateFactory) {
    return InstanceFactory.create(new LocationTrackingWorker_AssistedFactory_Impl(delegateFactory));
  }

  public static dagger.internal.Provider<LocationTrackingWorker_AssistedFactory> createFactoryProvider(
      LocationTrackingWorker_Factory delegateFactory) {
    return InstanceFactory.create(new LocationTrackingWorker_AssistedFactory_Impl(delegateFactory));
  }
}
