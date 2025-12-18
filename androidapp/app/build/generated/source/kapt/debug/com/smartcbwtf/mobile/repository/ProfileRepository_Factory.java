package com.smartcbwtf.mobile.repository;

import com.smartcbwtf.mobile.database.dao.UserProfileDao;
import com.smartcbwtf.mobile.network.api.ProfileApi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class ProfileRepository_Factory implements Factory<ProfileRepository> {
  private final Provider<ProfileApi> profileApiProvider;

  private final Provider<UserProfileDao> userProfileDaoProvider;

  public ProfileRepository_Factory(Provider<ProfileApi> profileApiProvider,
      Provider<UserProfileDao> userProfileDaoProvider) {
    this.profileApiProvider = profileApiProvider;
    this.userProfileDaoProvider = userProfileDaoProvider;
  }

  @Override
  public ProfileRepository get() {
    return newInstance(profileApiProvider.get(), userProfileDaoProvider.get());
  }

  public static ProfileRepository_Factory create(Provider<ProfileApi> profileApiProvider,
      Provider<UserProfileDao> userProfileDaoProvider) {
    return new ProfileRepository_Factory(profileApiProvider, userProfileDaoProvider);
  }

  public static ProfileRepository newInstance(ProfileApi profileApi,
      UserProfileDao userProfileDao) {
    return new ProfileRepository(profileApi, userProfileDao);
  }
}
