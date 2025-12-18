package com.smartcbwtf.mobile.work;

import android.content.Context;
import androidx.work.WorkerParameters;
import com.smartcbwtf.mobile.repository.AttendanceRepository;
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
public final class SyncAttendanceWorker_Factory {
  private final Provider<AttendanceRepository> repositoryProvider;

  public SyncAttendanceWorker_Factory(Provider<AttendanceRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  public SyncAttendanceWorker get(Context appContext, WorkerParameters params) {
    return newInstance(appContext, params, repositoryProvider.get());
  }

  public static SyncAttendanceWorker_Factory create(
      Provider<AttendanceRepository> repositoryProvider) {
    return new SyncAttendanceWorker_Factory(repositoryProvider);
  }

  public static SyncAttendanceWorker newInstance(Context appContext, WorkerParameters params,
      AttendanceRepository repository) {
    return new SyncAttendanceWorker(appContext, params, repository);
  }
}
