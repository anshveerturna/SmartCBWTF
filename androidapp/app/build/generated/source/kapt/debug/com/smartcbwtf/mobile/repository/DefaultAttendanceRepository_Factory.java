package com.smartcbwtf.mobile.repository;

import com.smartcbwtf.mobile.database.dao.AttendanceDao;
import com.smartcbwtf.mobile.network.api.AttendanceApi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import kotlinx.coroutines.CoroutineDispatcher;

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
public final class DefaultAttendanceRepository_Factory implements Factory<DefaultAttendanceRepository> {
  private final Provider<AttendanceDao> daoProvider;

  private final Provider<AttendanceApi> apiProvider;

  private final Provider<CoroutineDispatcher> ioDispatcherProvider;

  public DefaultAttendanceRepository_Factory(Provider<AttendanceDao> daoProvider,
      Provider<AttendanceApi> apiProvider, Provider<CoroutineDispatcher> ioDispatcherProvider) {
    this.daoProvider = daoProvider;
    this.apiProvider = apiProvider;
    this.ioDispatcherProvider = ioDispatcherProvider;
  }

  @Override
  public DefaultAttendanceRepository get() {
    return newInstance(daoProvider.get(), apiProvider.get(), ioDispatcherProvider.get());
  }

  public static DefaultAttendanceRepository_Factory create(Provider<AttendanceDao> daoProvider,
      Provider<AttendanceApi> apiProvider, Provider<CoroutineDispatcher> ioDispatcherProvider) {
    return new DefaultAttendanceRepository_Factory(daoProvider, apiProvider, ioDispatcherProvider);
  }

  public static DefaultAttendanceRepository newInstance(AttendanceDao dao, AttendanceApi api,
      CoroutineDispatcher ioDispatcher) {
    return new DefaultAttendanceRepository(dao, api, ioDispatcher);
  }
}
