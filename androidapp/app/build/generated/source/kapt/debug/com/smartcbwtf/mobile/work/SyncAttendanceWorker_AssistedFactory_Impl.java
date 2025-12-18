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
public final class SyncAttendanceWorker_AssistedFactory_Impl implements SyncAttendanceWorker_AssistedFactory {
  private final SyncAttendanceWorker_Factory delegateFactory;

  SyncAttendanceWorker_AssistedFactory_Impl(SyncAttendanceWorker_Factory delegateFactory) {
    this.delegateFactory = delegateFactory;
  }

  @Override
  public SyncAttendanceWorker create(Context arg0, WorkerParameters arg1) {
    return delegateFactory.get(arg0, arg1);
  }

  public static Provider<SyncAttendanceWorker_AssistedFactory> create(
      SyncAttendanceWorker_Factory delegateFactory) {
    return InstanceFactory.create(new SyncAttendanceWorker_AssistedFactory_Impl(delegateFactory));
  }

  public static dagger.internal.Provider<SyncAttendanceWorker_AssistedFactory> createFactoryProvider(
      SyncAttendanceWorker_Factory delegateFactory) {
    return InstanceFactory.create(new SyncAttendanceWorker_AssistedFactory_Impl(delegateFactory));
  }
}
