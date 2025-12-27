package com.smartcbwtf.mobile.work;

import androidx.hilt.work.WorkerAssistedFactory;
import androidx.work.ListenableWorker;
import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.codegen.OriginatingElement;
import dagger.hilt.components.SingletonComponent;
import dagger.multibindings.IntoMap;
import dagger.multibindings.StringKey;
import javax.annotation.processing.Generated;

@Generated("androidx.hilt.AndroidXHiltProcessor")
@Module
@InstallIn(SingletonComponent.class)
@OriginatingElement(
    topLevelClass = LocationTrackingWorker.class
)
public interface LocationTrackingWorker_HiltModule {
  @Binds
  @IntoMap
  @StringKey("com.smartcbwtf.mobile.work.LocationTrackingWorker")
  WorkerAssistedFactory<? extends ListenableWorker> bind(
      LocationTrackingWorker_AssistedFactory factory);
}
