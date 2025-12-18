package com.smartcbwtf.mobile.di;

import com.smartcbwtf.mobile.database.AppDatabase;
import com.smartcbwtf.mobile.database.dao.BagEventDao;
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
public final class DatabaseModule_ProvideBagEventDaoFactory implements Factory<BagEventDao> {
  private final Provider<AppDatabase> dbProvider;

  public DatabaseModule_ProvideBagEventDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public BagEventDao get() {
    return provideBagEventDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideBagEventDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new DatabaseModule_ProvideBagEventDaoFactory(dbProvider);
  }

  public static BagEventDao provideBagEventDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideBagEventDao(db));
  }
}
