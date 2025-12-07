package com.smartcbwtf.mobile.repository;

import com.smartcbwtf.mobile.database.dao.HcfDao;
import com.smartcbwtf.mobile.network.api.HcfApi;
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
public final class DefaultHcfRepository_Factory implements Factory<DefaultHcfRepository> {
  private final Provider<HcfDao> daoProvider;

  private final Provider<HcfApi> apiProvider;

  private final Provider<CoroutineDispatcher> ioDispatcherProvider;

  public DefaultHcfRepository_Factory(Provider<HcfDao> daoProvider, Provider<HcfApi> apiProvider,
      Provider<CoroutineDispatcher> ioDispatcherProvider) {
    this.daoProvider = daoProvider;
    this.apiProvider = apiProvider;
    this.ioDispatcherProvider = ioDispatcherProvider;
  }

  @Override
  public DefaultHcfRepository get() {
    return newInstance(daoProvider.get(), apiProvider.get(), ioDispatcherProvider.get());
  }

  public static DefaultHcfRepository_Factory create(Provider<HcfDao> daoProvider,
      Provider<HcfApi> apiProvider, Provider<CoroutineDispatcher> ioDispatcherProvider) {
    return new DefaultHcfRepository_Factory(daoProvider, apiProvider, ioDispatcherProvider);
  }

  public static DefaultHcfRepository newInstance(HcfDao dao, HcfApi api,
      CoroutineDispatcher ioDispatcher) {
    return new DefaultHcfRepository(dao, api, ioDispatcher);
  }
}
