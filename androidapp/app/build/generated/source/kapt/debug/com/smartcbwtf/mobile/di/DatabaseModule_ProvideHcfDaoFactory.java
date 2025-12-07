package com.smartcbwtf.mobile.di;

import com.smartcbwtf.mobile.database.AppDatabase;
import com.smartcbwtf.mobile.database.dao.HcfDao;
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
public final class DatabaseModule_ProvideHcfDaoFactory implements Factory<HcfDao> {
  private final Provider<AppDatabase> dbProvider;

  public DatabaseModule_ProvideHcfDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public HcfDao get() {
    return provideHcfDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideHcfDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new DatabaseModule_ProvideHcfDaoFactory(dbProvider);
  }

  public static HcfDao provideHcfDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideHcfDao(db));
  }
}
