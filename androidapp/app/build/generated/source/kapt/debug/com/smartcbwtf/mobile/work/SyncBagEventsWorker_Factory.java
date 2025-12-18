package com.smartcbwtf.mobile.work;

import android.content.Context;
import androidx.work.WorkerParameters;
import com.smartcbwtf.mobile.repository.BagEventRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class SyncBagEventsWorker_Factory {
  private final Provider<BagEventRepository> repositoryProvider;

  public SyncBagEventsWorker_Factory(Provider<BagEventRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  public SyncBagEventsWorker get(Context appContext, WorkerParameters params) {
    return newInstance(appContext, params, repositoryProvider.get());
  }

  public static SyncBagEventsWorker_Factory create(
      Provider<BagEventRepository> repositoryProvider) {
    return new SyncBagEventsWorker_Factory(repositoryProvider);
  }

  public static SyncBagEventsWorker newInstance(Context appContext, WorkerParameters params,
      BagEventRepository repository) {
    return new SyncBagEventsWorker(appContext, params, repository);
  }
}
