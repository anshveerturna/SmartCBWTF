package com.smartcbwtf.mobile.repository;

import com.smartcbwtf.mobile.database.dao.BagEventDao;
import com.smartcbwtf.mobile.network.api.BagEventApi;
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
public final class DefaultBagEventRepository_Factory implements Factory<DefaultBagEventRepository> {
  private final Provider<BagEventDao> daoProvider;

  private final Provider<BagEventApi> apiProvider;

  private final Provider<CoroutineDispatcher> ioDispatcherProvider;

  public DefaultBagEventRepository_Factory(Provider<BagEventDao> daoProvider,
      Provider<BagEventApi> apiProvider, Provider<CoroutineDispatcher> ioDispatcherProvider) {
    this.daoProvider = daoProvider;
    this.apiProvider = apiProvider;
    this.ioDispatcherProvider = ioDispatcherProvider;
  }

  @Override
  public DefaultBagEventRepository get() {
    return newInstance(daoProvider.get(), apiProvider.get(), ioDispatcherProvider.get());
  }

  public static DefaultBagEventRepository_Factory create(Provider<BagEventDao> daoProvider,
      Provider<BagEventApi> apiProvider, Provider<CoroutineDispatcher> ioDispatcherProvider) {
    return new DefaultBagEventRepository_Factory(daoProvider, apiProvider, ioDispatcherProvider);
  }

  public static DefaultBagEventRepository newInstance(BagEventDao dao, BagEventApi api,
      CoroutineDispatcher ioDispatcher) {
    return new DefaultBagEventRepository(dao, api, ioDispatcher);
  }
}
