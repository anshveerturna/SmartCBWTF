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
public final class SyncBagEventsWorker_AssistedFactory_Impl implements SyncBagEventsWorker_AssistedFactory {
  private final SyncBagEventsWorker_Factory delegateFactory;

  SyncBagEventsWorker_AssistedFactory_Impl(SyncBagEventsWorker_Factory delegateFactory) {
    this.delegateFactory = delegateFactory;
  }

  @Override
  public SyncBagEventsWorker create(Context arg0, WorkerParameters arg1) {
    return delegateFactory.get(arg0, arg1);
  }

  public static Provider<SyncBagEventsWorker_AssistedFactory> create(
      SyncBagEventsWorker_Factory delegateFactory) {
    return InstanceFactory.create(new SyncBagEventsWorker_AssistedFactory_Impl(delegateFactory));
  }

  public static dagger.internal.Provider<SyncBagEventsWorker_AssistedFactory> createFactoryProvider(
      SyncBagEventsWorker_Factory delegateFactory) {
    return InstanceFactory.create(new SyncBagEventsWorker_AssistedFactory_Impl(delegateFactory));
  }
}
