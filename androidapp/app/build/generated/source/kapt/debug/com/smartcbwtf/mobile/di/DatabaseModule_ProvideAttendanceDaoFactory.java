package com.smartcbwtf.mobile.di;

import com.smartcbwtf.mobile.database.AppDatabase;
import com.smartcbwtf.mobile.database.dao.AttendanceDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DatabaseModule_ProvideAttendanceDaoFactory implements Factory<AttendanceDao> {
  private final Provider<AppDatabase> dbProvider;

  public DatabaseModule_ProvideAttendanceDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public AttendanceDao get() {
    return provideAttendanceDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideAttendanceDaoFactory create(
      Provider<AppDatabase> dbProvider) {
    return new DatabaseModule_ProvideAttendanceDaoFactory(dbProvider);
  }

  public static AttendanceDao provideAttendanceDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideAttendanceDao(db));
  }
}
