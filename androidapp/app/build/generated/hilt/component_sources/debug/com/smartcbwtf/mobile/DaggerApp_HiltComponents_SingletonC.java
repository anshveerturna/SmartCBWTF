package com.smartcbwtf.mobile;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.hilt.work.HiltWorkerFactory;
import androidx.hilt.work.WorkerAssistedFactory;
import androidx.hilt.work.WorkerFactoryModule_ProvideFactoryFactory;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import androidx.work.ListenableWorker;
import androidx.work.WorkManager;
import androidx.work.WorkerParameters;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.gson.Gson;
import com.smartcbwtf.mobile.bluetooth.MockScaleService;
import com.smartcbwtf.mobile.bluetooth.RealBluetoothScaleService;
import com.smartcbwtf.mobile.bluetooth.ScaleService;
import com.smartcbwtf.mobile.database.AppDatabase;
import com.smartcbwtf.mobile.database.dao.AttendanceDao;
import com.smartcbwtf.mobile.database.dao.BagEventDao;
import com.smartcbwtf.mobile.database.dao.HcfDao;
import com.smartcbwtf.mobile.database.dao.UserProfileDao;
import com.smartcbwtf.mobile.di.AppModule_ProvideSharedPreferencesFactory;
import com.smartcbwtf.mobile.di.AppModule_ProvideWorkManagerFactory;
import com.smartcbwtf.mobile.di.CoroutineDispatchersModule_ProvideIoDispatcherFactory;
import com.smartcbwtf.mobile.di.DatabaseModule_ProvideAttendanceDaoFactory;
import com.smartcbwtf.mobile.di.DatabaseModule_ProvideBagEventDaoFactory;
import com.smartcbwtf.mobile.di.DatabaseModule_ProvideDatabaseFactory;
import com.smartcbwtf.mobile.di.DatabaseModule_ProvideHcfDaoFactory;
import com.smartcbwtf.mobile.di.DatabaseModule_ProvideUserProfileDaoFactory;
import com.smartcbwtf.mobile.di.NetworkModule_ProvideAttendanceApiFactory;
import com.smartcbwtf.mobile.di.NetworkModule_ProvideAuthApiFactory;
import com.smartcbwtf.mobile.di.NetworkModule_ProvideBagEventApiFactory;
import com.smartcbwtf.mobile.di.NetworkModule_ProvideGsonFactory;
import com.smartcbwtf.mobile.di.NetworkModule_ProvideHcfApiFactory;
import com.smartcbwtf.mobile.di.NetworkModule_ProvideLocationApiFactory;
import com.smartcbwtf.mobile.di.NetworkModule_ProvideOkHttpFactory;
import com.smartcbwtf.mobile.di.NetworkModule_ProvideProfileApiFactory;
import com.smartcbwtf.mobile.di.NetworkModule_ProvideRetrofitFactory;
import com.smartcbwtf.mobile.di.NetworkModule_ProvideRouteApiFactory;
import com.smartcbwtf.mobile.di.NetworkModule_ProvideVerificationApiFactory;
import com.smartcbwtf.mobile.di.ScaleModule_Companion_ProvideScaleServiceFactory;
import com.smartcbwtf.mobile.network.AuthInterceptor;
import com.smartcbwtf.mobile.network.api.AttendanceApi;
import com.smartcbwtf.mobile.network.api.AuthApi;
import com.smartcbwtf.mobile.network.api.BagEventApi;
import com.smartcbwtf.mobile.network.api.HcfApi;
import com.smartcbwtf.mobile.network.api.LocationApi;
import com.smartcbwtf.mobile.network.api.ProfileApi;
import com.smartcbwtf.mobile.network.api.RouteApi;
import com.smartcbwtf.mobile.network.api.VerificationApi;
import com.smartcbwtf.mobile.repository.DefaultAttendanceRepository;
import com.smartcbwtf.mobile.repository.DefaultAuthRepository;
import com.smartcbwtf.mobile.repository.DefaultBagEventRepository;
import com.smartcbwtf.mobile.repository.DefaultHcfRepository;
import com.smartcbwtf.mobile.repository.DefaultLocationRepository;
import com.smartcbwtf.mobile.repository.ProfileRepository;
import com.smartcbwtf.mobile.repository.RouteRepository;
import com.smartcbwtf.mobile.service.ForegroundLocationService;
import com.smartcbwtf.mobile.service.ForegroundLocationService_MembersInjector;
import com.smartcbwtf.mobile.storage.AppConfigStore;
import com.smartcbwtf.mobile.storage.DefaultAuthTokenStore;
import com.smartcbwtf.mobile.storage.SessionManager;
import com.smartcbwtf.mobile.ui.AttendanceFragment;
import com.smartcbwtf.mobile.ui.ChangePasswordFragment;
import com.smartcbwtf.mobile.ui.ChangePasswordFragment_MembersInjector;
import com.smartcbwtf.mobile.ui.ChangePasswordViewModel;
import com.smartcbwtf.mobile.ui.ChangePasswordViewModel_HiltModules;
import com.smartcbwtf.mobile.ui.HcfRegistrationFragment;
import com.smartcbwtf.mobile.ui.HomeFragment;
import com.smartcbwtf.mobile.ui.HomeFragment_MembersInjector;
import com.smartcbwtf.mobile.ui.LocationDisclosureFragment;
import com.smartcbwtf.mobile.ui.LocationDisclosureFragment_MembersInjector;
import com.smartcbwtf.mobile.ui.LoginFragment;
import com.smartcbwtf.mobile.ui.LoginFragment_MembersInjector;
import com.smartcbwtf.mobile.ui.MyRouteFragment;
import com.smartcbwtf.mobile.ui.PermissionsFragment;
import com.smartcbwtf.mobile.ui.PermissionsFragment_MembersInjector;
import com.smartcbwtf.mobile.ui.ProfileFragment;
import com.smartcbwtf.mobile.ui.ScanWeighFragment;
import com.smartcbwtf.mobile.ui.ScanWeighFragment_MembersInjector;
import com.smartcbwtf.mobile.ui.SettingsFragment;
import com.smartcbwtf.mobile.ui.SplashFragment;
import com.smartcbwtf.mobile.ui.StartPickupFragment;
import com.smartcbwtf.mobile.ui.VerifyAtCbtwfFragment;
import com.smartcbwtf.mobile.ui.scanner.QrScannerFragment;
import com.smartcbwtf.mobile.utils.LocationHelper;
import com.smartcbwtf.mobile.utils.NetworkMonitor;
import com.smartcbwtf.mobile.utils.PermissionHelper;
import com.smartcbwtf.mobile.viewmodel.AttendanceViewModel;
import com.smartcbwtf.mobile.viewmodel.AttendanceViewModel_HiltModules;
import com.smartcbwtf.mobile.viewmodel.AuthViewModel;
import com.smartcbwtf.mobile.viewmodel.AuthViewModel_HiltModules;
import com.smartcbwtf.mobile.viewmodel.HcfRegistrationViewModel;
import com.smartcbwtf.mobile.viewmodel.HcfRegistrationViewModel_HiltModules;
import com.smartcbwtf.mobile.viewmodel.HomeViewModel;
import com.smartcbwtf.mobile.viewmodel.HomeViewModel_HiltModules;
import com.smartcbwtf.mobile.viewmodel.MyRouteViewModel;
import com.smartcbwtf.mobile.viewmodel.MyRouteViewModel_HiltModules;
import com.smartcbwtf.mobile.viewmodel.ProfileViewModel;
import com.smartcbwtf.mobile.viewmodel.ProfileViewModel_HiltModules;
import com.smartcbwtf.mobile.viewmodel.ScanWeighViewModel;
import com.smartcbwtf.mobile.viewmodel.ScanWeighViewModel_HiltModules;
import com.smartcbwtf.mobile.viewmodel.SettingsViewModel;
import com.smartcbwtf.mobile.viewmodel.SettingsViewModel_HiltModules;
import com.smartcbwtf.mobile.viewmodel.StartPickupViewModel;
import com.smartcbwtf.mobile.viewmodel.StartPickupViewModel_HiltModules;
import com.smartcbwtf.mobile.viewmodel.VerifyAtPlantViewModel;
import com.smartcbwtf.mobile.viewmodel.VerifyAtPlantViewModel_HiltModules;
import com.smartcbwtf.mobile.work.LocationTrackingWorker;
import com.smartcbwtf.mobile.work.LocationTrackingWorker_AssistedFactory;
import com.smartcbwtf.mobile.work.SyncAttendanceWorker;
import com.smartcbwtf.mobile.work.SyncAttendanceWorker_AssistedFactory;
import com.smartcbwtf.mobile.work.SyncBagEventsWorker;
import com.smartcbwtf.mobile.work.SyncBagEventsWorker_AssistedFactory;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.IdentifierNameString;
import dagger.internal.KeepFieldType;
import dagger.internal.LazyClassKeyMap;
import dagger.internal.MapBuilder;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.SingleCheck;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;
import kotlinx.coroutines.CoroutineDispatcher;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

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
public final class DaggerApp_HiltComponents_SingletonC {
  private DaggerApp_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public App_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements App_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public App_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements App_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public App_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements App_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public App_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements App_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public App_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements App_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public App_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements App_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public App_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements App_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public App_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends App_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    private ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends App_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    private FragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public void injectAttendanceFragment(AttendanceFragment attendanceFragment) {
    }

    @Override
    public void injectChangePasswordFragment(ChangePasswordFragment changePasswordFragment) {
      injectChangePasswordFragment2(changePasswordFragment);
    }

    @Override
    public void injectHcfRegistrationFragment(HcfRegistrationFragment hcfRegistrationFragment) {
    }

    @Override
    public void injectHomeFragment(HomeFragment homeFragment) {
      injectHomeFragment2(homeFragment);
    }

    @Override
    public void injectLocationDisclosureFragment(
        LocationDisclosureFragment locationDisclosureFragment) {
      injectLocationDisclosureFragment2(locationDisclosureFragment);
    }

    @Override
    public void injectLoginFragment(LoginFragment loginFragment) {
      injectLoginFragment2(loginFragment);
    }

    @Override
    public void injectMyRouteFragment(MyRouteFragment myRouteFragment) {
    }

    @Override
    public void injectPermissionsFragment(PermissionsFragment permissionsFragment) {
      injectPermissionsFragment2(permissionsFragment);
    }

    @Override
    public void injectProfileFragment(ProfileFragment profileFragment) {
    }

    @Override
    public void injectScanWeighFragment(ScanWeighFragment scanWeighFragment) {
      injectScanWeighFragment2(scanWeighFragment);
    }

    @Override
    public void injectSettingsFragment(SettingsFragment settingsFragment) {
    }

    @Override
    public void injectSplashFragment(SplashFragment splashFragment) {
    }

    @Override
    public void injectStartPickupFragment(StartPickupFragment startPickupFragment) {
    }

    @Override
    public void injectVerifyAtCbtwfFragment(VerifyAtCbtwfFragment verifyAtCbtwfFragment) {
    }

    @Override
    public void injectQrScannerFragment(QrScannerFragment qrScannerFragment) {
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }

    @CanIgnoreReturnValue
    private ChangePasswordFragment injectChangePasswordFragment2(ChangePasswordFragment instance) {
      ChangePasswordFragment_MembersInjector.injectAuthRepository(instance, singletonCImpl.defaultAuthRepositoryProvider.get());
      return instance;
    }

    @CanIgnoreReturnValue
    private HomeFragment injectHomeFragment2(HomeFragment instance) {
      HomeFragment_MembersInjector.injectLocationHelper(instance, singletonCImpl.locationHelperProvider.get());
      HomeFragment_MembersInjector.injectLocationRepository(instance, singletonCImpl.defaultLocationRepositoryProvider.get());
      return instance;
    }

    @CanIgnoreReturnValue
    private LocationDisclosureFragment injectLocationDisclosureFragment2(
        LocationDisclosureFragment instance) {
      LocationDisclosureFragment_MembersInjector.injectLocationRepository(instance, singletonCImpl.defaultLocationRepositoryProvider.get());
      return instance;
    }

    @CanIgnoreReturnValue
    private LoginFragment injectLoginFragment2(LoginFragment instance) {
      LoginFragment_MembersInjector.injectSharedPreferences(instance, singletonCImpl.provideSharedPreferencesProvider.get());
      LoginFragment_MembersInjector.injectLocationRepository(instance, singletonCImpl.defaultLocationRepositoryProvider.get());
      return instance;
    }

    @CanIgnoreReturnValue
    private PermissionsFragment injectPermissionsFragment2(PermissionsFragment instance) {
      PermissionsFragment_MembersInjector.injectSharedPreferences(instance, singletonCImpl.provideSharedPreferencesProvider.get());
      PermissionsFragment_MembersInjector.injectLocationRepository(instance, singletonCImpl.defaultLocationRepositoryProvider.get());
      return instance;
    }

    @CanIgnoreReturnValue
    private ScanWeighFragment injectScanWeighFragment2(ScanWeighFragment instance) {
      ScanWeighFragment_MembersInjector.injectPermissionHelper(instance, singletonCImpl.permissionHelperProvider.get());
      return instance;
    }
  }

  private static final class ViewCImpl extends App_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    private ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends App_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    private ActivityCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public void injectMainActivity(MainActivity mainActivity) {
      injectMainActivity2(mainActivity);
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Map<Class<?>, Boolean> getViewModelKeys() {
      return LazyClassKeyMap.<Boolean>of(MapBuilder.<String, Boolean>newMapBuilder(11).put(LazyClassKeyProvider.com_smartcbwtf_mobile_viewmodel_AttendanceViewModel, AttendanceViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_smartcbwtf_mobile_viewmodel_AuthViewModel, AuthViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_smartcbwtf_mobile_ui_ChangePasswordViewModel, ChangePasswordViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_smartcbwtf_mobile_viewmodel_HcfRegistrationViewModel, HcfRegistrationViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_smartcbwtf_mobile_viewmodel_HomeViewModel, HomeViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_smartcbwtf_mobile_viewmodel_MyRouteViewModel, MyRouteViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_smartcbwtf_mobile_viewmodel_ProfileViewModel, ProfileViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_smartcbwtf_mobile_viewmodel_ScanWeighViewModel, ScanWeighViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_smartcbwtf_mobile_viewmodel_SettingsViewModel, SettingsViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_smartcbwtf_mobile_viewmodel_StartPickupViewModel, StartPickupViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_smartcbwtf_mobile_viewmodel_VerifyAtPlantViewModel, VerifyAtPlantViewModel_HiltModules.KeyModule.provide()).build());
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @CanIgnoreReturnValue
    private MainActivity injectMainActivity2(MainActivity instance) {
      MainActivity_MembersInjector.injectAuthRepository(instance, singletonCImpl.defaultAuthRepositoryProvider.get());
      return instance;
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_smartcbwtf_mobile_viewmodel_ScanWeighViewModel = "com.smartcbwtf.mobile.viewmodel.ScanWeighViewModel";

      static String com_smartcbwtf_mobile_viewmodel_SettingsViewModel = "com.smartcbwtf.mobile.viewmodel.SettingsViewModel";

      static String com_smartcbwtf_mobile_viewmodel_VerifyAtPlantViewModel = "com.smartcbwtf.mobile.viewmodel.VerifyAtPlantViewModel";

      static String com_smartcbwtf_mobile_viewmodel_ProfileViewModel = "com.smartcbwtf.mobile.viewmodel.ProfileViewModel";

      static String com_smartcbwtf_mobile_viewmodel_HomeViewModel = "com.smartcbwtf.mobile.viewmodel.HomeViewModel";

      static String com_smartcbwtf_mobile_viewmodel_HcfRegistrationViewModel = "com.smartcbwtf.mobile.viewmodel.HcfRegistrationViewModel";

      static String com_smartcbwtf_mobile_ui_ChangePasswordViewModel = "com.smartcbwtf.mobile.ui.ChangePasswordViewModel";

      static String com_smartcbwtf_mobile_viewmodel_AttendanceViewModel = "com.smartcbwtf.mobile.viewmodel.AttendanceViewModel";

      static String com_smartcbwtf_mobile_viewmodel_AuthViewModel = "com.smartcbwtf.mobile.viewmodel.AuthViewModel";

      static String com_smartcbwtf_mobile_viewmodel_MyRouteViewModel = "com.smartcbwtf.mobile.viewmodel.MyRouteViewModel";

      static String com_smartcbwtf_mobile_viewmodel_StartPickupViewModel = "com.smartcbwtf.mobile.viewmodel.StartPickupViewModel";

      @KeepFieldType
      ScanWeighViewModel com_smartcbwtf_mobile_viewmodel_ScanWeighViewModel2;

      @KeepFieldType
      SettingsViewModel com_smartcbwtf_mobile_viewmodel_SettingsViewModel2;

      @KeepFieldType
      VerifyAtPlantViewModel com_smartcbwtf_mobile_viewmodel_VerifyAtPlantViewModel2;

      @KeepFieldType
      ProfileViewModel com_smartcbwtf_mobile_viewmodel_ProfileViewModel2;

      @KeepFieldType
      HomeViewModel com_smartcbwtf_mobile_viewmodel_HomeViewModel2;

      @KeepFieldType
      HcfRegistrationViewModel com_smartcbwtf_mobile_viewmodel_HcfRegistrationViewModel2;

      @KeepFieldType
      ChangePasswordViewModel com_smartcbwtf_mobile_ui_ChangePasswordViewModel2;

      @KeepFieldType
      AttendanceViewModel com_smartcbwtf_mobile_viewmodel_AttendanceViewModel2;

      @KeepFieldType
      AuthViewModel com_smartcbwtf_mobile_viewmodel_AuthViewModel2;

      @KeepFieldType
      MyRouteViewModel com_smartcbwtf_mobile_viewmodel_MyRouteViewModel2;

      @KeepFieldType
      StartPickupViewModel com_smartcbwtf_mobile_viewmodel_StartPickupViewModel2;
    }
  }

  private static final class ViewModelCImpl extends App_HiltComponents.ViewModelC {
    private final SavedStateHandle savedStateHandle;

    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<AttendanceViewModel> attendanceViewModelProvider;

    private Provider<AuthViewModel> authViewModelProvider;

    private Provider<ChangePasswordViewModel> changePasswordViewModelProvider;

    private Provider<HcfRegistrationViewModel> hcfRegistrationViewModelProvider;

    private Provider<HomeViewModel> homeViewModelProvider;

    private Provider<MyRouteViewModel> myRouteViewModelProvider;

    private Provider<ProfileViewModel> profileViewModelProvider;

    private Provider<ScanWeighViewModel> scanWeighViewModelProvider;

    private Provider<SettingsViewModel> settingsViewModelProvider;

    private Provider<StartPickupViewModel> startPickupViewModelProvider;

    private Provider<VerifyAtPlantViewModel> verifyAtPlantViewModelProvider;

    private ViewModelCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, SavedStateHandle savedStateHandleParam,
        ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.savedStateHandle = savedStateHandleParam;
      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.attendanceViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.authViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.changePasswordViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.hcfRegistrationViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
      this.homeViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 4);
      this.myRouteViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 5);
      this.profileViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 6);
      this.scanWeighViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 7);
      this.settingsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 8);
      this.startPickupViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 9);
      this.verifyAtPlantViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 10);
    }

    @Override
    public Map<Class<?>, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return LazyClassKeyMap.<javax.inject.Provider<ViewModel>>of(MapBuilder.<String, javax.inject.Provider<ViewModel>>newMapBuilder(11).put(LazyClassKeyProvider.com_smartcbwtf_mobile_viewmodel_AttendanceViewModel, ((Provider) attendanceViewModelProvider)).put(LazyClassKeyProvider.com_smartcbwtf_mobile_viewmodel_AuthViewModel, ((Provider) authViewModelProvider)).put(LazyClassKeyProvider.com_smartcbwtf_mobile_ui_ChangePasswordViewModel, ((Provider) changePasswordViewModelProvider)).put(LazyClassKeyProvider.com_smartcbwtf_mobile_viewmodel_HcfRegistrationViewModel, ((Provider) hcfRegistrationViewModelProvider)).put(LazyClassKeyProvider.com_smartcbwtf_mobile_viewmodel_HomeViewModel, ((Provider) homeViewModelProvider)).put(LazyClassKeyProvider.com_smartcbwtf_mobile_viewmodel_MyRouteViewModel, ((Provider) myRouteViewModelProvider)).put(LazyClassKeyProvider.com_smartcbwtf_mobile_viewmodel_ProfileViewModel, ((Provider) profileViewModelProvider)).put(LazyClassKeyProvider.com_smartcbwtf_mobile_viewmodel_ScanWeighViewModel, ((Provider) scanWeighViewModelProvider)).put(LazyClassKeyProvider.com_smartcbwtf_mobile_viewmodel_SettingsViewModel, ((Provider) settingsViewModelProvider)).put(LazyClassKeyProvider.com_smartcbwtf_mobile_viewmodel_StartPickupViewModel, ((Provider) startPickupViewModelProvider)).put(LazyClassKeyProvider.com_smartcbwtf_mobile_viewmodel_VerifyAtPlantViewModel, ((Provider) verifyAtPlantViewModelProvider)).build());
    }

    @Override
    public Map<Class<?>, Object> getHiltViewModelAssistedMap() {
      return Collections.<Class<?>, Object>emptyMap();
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_smartcbwtf_mobile_ui_ChangePasswordViewModel = "com.smartcbwtf.mobile.ui.ChangePasswordViewModel";

      static String com_smartcbwtf_mobile_viewmodel_SettingsViewModel = "com.smartcbwtf.mobile.viewmodel.SettingsViewModel";

      static String com_smartcbwtf_mobile_viewmodel_AuthViewModel = "com.smartcbwtf.mobile.viewmodel.AuthViewModel";

      static String com_smartcbwtf_mobile_viewmodel_VerifyAtPlantViewModel = "com.smartcbwtf.mobile.viewmodel.VerifyAtPlantViewModel";

      static String com_smartcbwtf_mobile_viewmodel_StartPickupViewModel = "com.smartcbwtf.mobile.viewmodel.StartPickupViewModel";

      static String com_smartcbwtf_mobile_viewmodel_HcfRegistrationViewModel = "com.smartcbwtf.mobile.viewmodel.HcfRegistrationViewModel";

      static String com_smartcbwtf_mobile_viewmodel_HomeViewModel = "com.smartcbwtf.mobile.viewmodel.HomeViewModel";

      static String com_smartcbwtf_mobile_viewmodel_ProfileViewModel = "com.smartcbwtf.mobile.viewmodel.ProfileViewModel";

      static String com_smartcbwtf_mobile_viewmodel_ScanWeighViewModel = "com.smartcbwtf.mobile.viewmodel.ScanWeighViewModel";

      static String com_smartcbwtf_mobile_viewmodel_AttendanceViewModel = "com.smartcbwtf.mobile.viewmodel.AttendanceViewModel";

      static String com_smartcbwtf_mobile_viewmodel_MyRouteViewModel = "com.smartcbwtf.mobile.viewmodel.MyRouteViewModel";

      @KeepFieldType
      ChangePasswordViewModel com_smartcbwtf_mobile_ui_ChangePasswordViewModel2;

      @KeepFieldType
      SettingsViewModel com_smartcbwtf_mobile_viewmodel_SettingsViewModel2;

      @KeepFieldType
      AuthViewModel com_smartcbwtf_mobile_viewmodel_AuthViewModel2;

      @KeepFieldType
      VerifyAtPlantViewModel com_smartcbwtf_mobile_viewmodel_VerifyAtPlantViewModel2;

      @KeepFieldType
      StartPickupViewModel com_smartcbwtf_mobile_viewmodel_StartPickupViewModel2;

      @KeepFieldType
      HcfRegistrationViewModel com_smartcbwtf_mobile_viewmodel_HcfRegistrationViewModel2;

      @KeepFieldType
      HomeViewModel com_smartcbwtf_mobile_viewmodel_HomeViewModel2;

      @KeepFieldType
      ProfileViewModel com_smartcbwtf_mobile_viewmodel_ProfileViewModel2;

      @KeepFieldType
      ScanWeighViewModel com_smartcbwtf_mobile_viewmodel_ScanWeighViewModel2;

      @KeepFieldType
      AttendanceViewModel com_smartcbwtf_mobile_viewmodel_AttendanceViewModel2;

      @KeepFieldType
      MyRouteViewModel com_smartcbwtf_mobile_viewmodel_MyRouteViewModel2;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.smartcbwtf.mobile.viewmodel.AttendanceViewModel 
          return (T) new AttendanceViewModel(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.locationHelperProvider.get(), singletonCImpl.defaultHcfRepositoryProvider.get(), singletonCImpl.defaultAttendanceRepositoryProvider.get());

          case 1: // com.smartcbwtf.mobile.viewmodel.AuthViewModel 
          return (T) new AuthViewModel(singletonCImpl.defaultAuthRepositoryProvider.get());

          case 2: // com.smartcbwtf.mobile.ui.ChangePasswordViewModel 
          return (T) new ChangePasswordViewModel(singletonCImpl.provideProfileApiProvider.get(), singletonCImpl.defaultAuthTokenStoreProvider.get(), singletonCImpl.defaultAuthRepositoryProvider.get());

          case 3: // com.smartcbwtf.mobile.viewmodel.HcfRegistrationViewModel 
          return (T) new HcfRegistrationViewModel(viewModelCImpl.savedStateHandle, ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.defaultHcfRepositoryProvider.get(), singletonCImpl.locationHelperProvider.get(), singletonCImpl.sessionManagerProvider.get());

          case 4: // com.smartcbwtf.mobile.viewmodel.HomeViewModel 
          return (T) new HomeViewModel(singletonCImpl.defaultAuthRepositoryProvider.get(), singletonCImpl.defaultBagEventRepositoryProvider.get(), singletonCImpl.profileRepositoryProvider.get(), singletonCImpl.provideWorkManagerProvider.get());

          case 5: // com.smartcbwtf.mobile.viewmodel.MyRouteViewModel 
          return (T) new MyRouteViewModel(singletonCImpl.routeRepositoryProvider.get());

          case 6: // com.smartcbwtf.mobile.viewmodel.ProfileViewModel 
          return (T) new ProfileViewModel(singletonCImpl.profileRepositoryProvider.get());

          case 7: // com.smartcbwtf.mobile.viewmodel.ScanWeighViewModel 
          return (T) new ScanWeighViewModel(singletonCImpl.provideScaleServiceProvider.get(), singletonCImpl.defaultBagEventRepositoryProvider.get(), singletonCImpl.locationHelperProvider.get(), singletonCImpl.provideVerificationApiProvider.get(), singletonCImpl.sessionManagerProvider.get());

          case 8: // com.smartcbwtf.mobile.viewmodel.SettingsViewModel 
          return (T) new SettingsViewModel(singletonCImpl.defaultAuthRepositoryProvider.get(), singletonCImpl.defaultBagEventRepositoryProvider.get(), singletonCImpl.provideScaleServiceProvider.get());

          case 9: // com.smartcbwtf.mobile.viewmodel.StartPickupViewModel 
          return (T) new StartPickupViewModel(singletonCImpl.defaultHcfRepositoryProvider.get(), singletonCImpl.locationHelperProvider.get());

          case 10: // com.smartcbwtf.mobile.viewmodel.VerifyAtPlantViewModel 
          return (T) new VerifyAtPlantViewModel(singletonCImpl.provideScaleServiceProvider.get(), singletonCImpl.defaultBagEventRepositoryProvider.get(), singletonCImpl.locationHelperProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends App_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    private Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    private ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle 
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends App_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }

    @Override
    public void injectForegroundLocationService(
        ForegroundLocationService foregroundLocationService) {
      injectForegroundLocationService2(foregroundLocationService);
    }

    @CanIgnoreReturnValue
    private ForegroundLocationService injectForegroundLocationService2(
        ForegroundLocationService instance) {
      ForegroundLocationService_MembersInjector.injectLocationRepository(instance, singletonCImpl.defaultLocationRepositoryProvider.get());
      ForegroundLocationService_MembersInjector.injectAuthTokenStore(instance, singletonCImpl.defaultAuthTokenStoreProvider.get());
      ForegroundLocationService_MembersInjector.injectAppConfigStore(instance, singletonCImpl.appConfigStoreProvider.get());
      return instance;
    }
  }

  private static final class SingletonCImpl extends App_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    private Provider<SharedPreferences> provideSharedPreferencesProvider;

    private Provider<DefaultAuthTokenStore> defaultAuthTokenStoreProvider;

    private Provider<AuthInterceptor> authInterceptorProvider;

    private Provider<OkHttpClient> provideOkHttpProvider;

    private Provider<Gson> provideGsonProvider;

    private Provider<Retrofit> provideRetrofitProvider;

    private Provider<LocationApi> provideLocationApiProvider;

    private Provider<DefaultLocationRepository> defaultLocationRepositoryProvider;

    private Provider<LocationHelper> locationHelperProvider;

    private Provider<AppConfigStore> appConfigStoreProvider;

    private Provider<LocationTrackingWorker_AssistedFactory> locationTrackingWorker_AssistedFactoryProvider;

    private Provider<AppDatabase> provideDatabaseProvider;

    private Provider<AttendanceApi> provideAttendanceApiProvider;

    private Provider<CoroutineDispatcher> provideIoDispatcherProvider;

    private Provider<DefaultAttendanceRepository> defaultAttendanceRepositoryProvider;

    private Provider<SyncAttendanceWorker_AssistedFactory> syncAttendanceWorker_AssistedFactoryProvider;

    private Provider<BagEventApi> provideBagEventApiProvider;

    private Provider<DefaultBagEventRepository> defaultBagEventRepositoryProvider;

    private Provider<SyncBagEventsWorker_AssistedFactory> syncBagEventsWorker_AssistedFactoryProvider;

    private Provider<AuthApi> provideAuthApiProvider;

    private Provider<NetworkMonitor> networkMonitorProvider;

    private Provider<DefaultAuthRepository> defaultAuthRepositoryProvider;

    private Provider<PermissionHelper> permissionHelperProvider;

    private Provider<HcfApi> provideHcfApiProvider;

    private Provider<DefaultHcfRepository> defaultHcfRepositoryProvider;

    private Provider<ProfileApi> provideProfileApiProvider;

    private Provider<SessionManager> sessionManagerProvider;

    private Provider<ProfileRepository> profileRepositoryProvider;

    private Provider<WorkManager> provideWorkManagerProvider;

    private Provider<RouteApi> provideRouteApiProvider;

    private Provider<RouteRepository> routeRepositoryProvider;

    private Provider<RealBluetoothScaleService> realBluetoothScaleServiceProvider;

    private Provider<MockScaleService> mockScaleServiceProvider;

    private Provider<ScaleService> provideScaleServiceProvider;

    private Provider<VerificationApi> provideVerificationApiProvider;

    private SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);

    }

    private AttendanceDao attendanceDao() {
      return DatabaseModule_ProvideAttendanceDaoFactory.provideAttendanceDao(provideDatabaseProvider.get());
    }

    private BagEventDao bagEventDao() {
      return DatabaseModule_ProvideBagEventDaoFactory.provideBagEventDao(provideDatabaseProvider.get());
    }

    private Map<String, javax.inject.Provider<WorkerAssistedFactory<? extends ListenableWorker>>> mapOfStringAndProviderOfWorkerAssistedFactoryOf(
        ) {
      return MapBuilder.<String, javax.inject.Provider<WorkerAssistedFactory<? extends ListenableWorker>>>newMapBuilder(3).put("com.smartcbwtf.mobile.work.LocationTrackingWorker", ((Provider) locationTrackingWorker_AssistedFactoryProvider)).put("com.smartcbwtf.mobile.work.SyncAttendanceWorker", ((Provider) syncAttendanceWorker_AssistedFactoryProvider)).put("com.smartcbwtf.mobile.work.SyncBagEventsWorker", ((Provider) syncBagEventsWorker_AssistedFactoryProvider)).build();
    }

    private HiltWorkerFactory hiltWorkerFactory() {
      return WorkerFactoryModule_ProvideFactoryFactory.provideFactory(mapOfStringAndProviderOfWorkerAssistedFactoryOf());
    }

    private HcfDao hcfDao() {
      return DatabaseModule_ProvideHcfDaoFactory.provideHcfDao(provideDatabaseProvider.get());
    }

    private UserProfileDao userProfileDao() {
      return DatabaseModule_ProvideUserProfileDaoFactory.provideUserProfileDao(provideDatabaseProvider.get());
    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.provideSharedPreferencesProvider = DoubleCheck.provider(new SwitchingProvider<SharedPreferences>(singletonCImpl, 7));
      this.defaultAuthTokenStoreProvider = DoubleCheck.provider(new SwitchingProvider<DefaultAuthTokenStore>(singletonCImpl, 6));
      this.authInterceptorProvider = DoubleCheck.provider(new SwitchingProvider<AuthInterceptor>(singletonCImpl, 5));
      this.provideOkHttpProvider = DoubleCheck.provider(new SwitchingProvider<OkHttpClient>(singletonCImpl, 4));
      this.provideGsonProvider = DoubleCheck.provider(new SwitchingProvider<Gson>(singletonCImpl, 8));
      this.provideRetrofitProvider = DoubleCheck.provider(new SwitchingProvider<Retrofit>(singletonCImpl, 3));
      this.provideLocationApiProvider = DoubleCheck.provider(new SwitchingProvider<LocationApi>(singletonCImpl, 2));
      this.defaultLocationRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<DefaultLocationRepository>(singletonCImpl, 1));
      this.locationHelperProvider = DoubleCheck.provider(new SwitchingProvider<LocationHelper>(singletonCImpl, 9));
      this.appConfigStoreProvider = DoubleCheck.provider(new SwitchingProvider<AppConfigStore>(singletonCImpl, 10));
      this.locationTrackingWorker_AssistedFactoryProvider = SingleCheck.provider(new SwitchingProvider<LocationTrackingWorker_AssistedFactory>(singletonCImpl, 0));
      this.provideDatabaseProvider = DoubleCheck.provider(new SwitchingProvider<AppDatabase>(singletonCImpl, 13));
      this.provideAttendanceApiProvider = DoubleCheck.provider(new SwitchingProvider<AttendanceApi>(singletonCImpl, 14));
      this.provideIoDispatcherProvider = DoubleCheck.provider(new SwitchingProvider<CoroutineDispatcher>(singletonCImpl, 15));
      this.defaultAttendanceRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<DefaultAttendanceRepository>(singletonCImpl, 12));
      this.syncAttendanceWorker_AssistedFactoryProvider = SingleCheck.provider(new SwitchingProvider<SyncAttendanceWorker_AssistedFactory>(singletonCImpl, 11));
      this.provideBagEventApiProvider = DoubleCheck.provider(new SwitchingProvider<BagEventApi>(singletonCImpl, 18));
      this.defaultBagEventRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<DefaultBagEventRepository>(singletonCImpl, 17));
      this.syncBagEventsWorker_AssistedFactoryProvider = SingleCheck.provider(new SwitchingProvider<SyncBagEventsWorker_AssistedFactory>(singletonCImpl, 16));
      this.provideAuthApiProvider = DoubleCheck.provider(new SwitchingProvider<AuthApi>(singletonCImpl, 20));
      this.networkMonitorProvider = DoubleCheck.provider(new SwitchingProvider<NetworkMonitor>(singletonCImpl, 21));
      this.defaultAuthRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<DefaultAuthRepository>(singletonCImpl, 19));
      this.permissionHelperProvider = DoubleCheck.provider(new SwitchingProvider<PermissionHelper>(singletonCImpl, 22));
      this.provideHcfApiProvider = DoubleCheck.provider(new SwitchingProvider<HcfApi>(singletonCImpl, 24));
      this.defaultHcfRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<DefaultHcfRepository>(singletonCImpl, 23));
      this.provideProfileApiProvider = DoubleCheck.provider(new SwitchingProvider<ProfileApi>(singletonCImpl, 25));
      this.sessionManagerProvider = DoubleCheck.provider(new SwitchingProvider<SessionManager>(singletonCImpl, 26));
      this.profileRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<ProfileRepository>(singletonCImpl, 27));
      this.provideWorkManagerProvider = DoubleCheck.provider(new SwitchingProvider<WorkManager>(singletonCImpl, 28));
      this.provideRouteApiProvider = DoubleCheck.provider(new SwitchingProvider<RouteApi>(singletonCImpl, 30));
      this.routeRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<RouteRepository>(singletonCImpl, 29));
      this.realBluetoothScaleServiceProvider = DoubleCheck.provider(new SwitchingProvider<RealBluetoothScaleService>(singletonCImpl, 32));
      this.mockScaleServiceProvider = DoubleCheck.provider(new SwitchingProvider<MockScaleService>(singletonCImpl, 33));
      this.provideScaleServiceProvider = DoubleCheck.provider(new SwitchingProvider<ScaleService>(singletonCImpl, 31));
      this.provideVerificationApiProvider = DoubleCheck.provider(new SwitchingProvider<VerificationApi>(singletonCImpl, 34));
    }

    @Override
    public void injectApp(App app) {
      injectApp2(app);
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return Collections.<Boolean>emptySet();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    @CanIgnoreReturnValue
    private App injectApp2(App instance) {
      App_MembersInjector.injectWorkerFactory(instance, hiltWorkerFactory());
      return instance;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.smartcbwtf.mobile.work.LocationTrackingWorker_AssistedFactory 
          return (T) new LocationTrackingWorker_AssistedFactory() {
            @Override
            public LocationTrackingWorker create(Context appContext, WorkerParameters params) {
              return new LocationTrackingWorker(appContext, params, singletonCImpl.defaultLocationRepositoryProvider.get(), singletonCImpl.locationHelperProvider.get(), singletonCImpl.defaultAuthTokenStoreProvider.get(), singletonCImpl.appConfigStoreProvider.get());
            }
          };

          case 1: // com.smartcbwtf.mobile.repository.DefaultLocationRepository 
          return (T) new DefaultLocationRepository(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.provideLocationApiProvider.get(), singletonCImpl.defaultAuthTokenStoreProvider.get());

          case 2: // com.smartcbwtf.mobile.network.api.LocationApi 
          return (T) NetworkModule_ProvideLocationApiFactory.provideLocationApi(singletonCImpl.provideRetrofitProvider.get());

          case 3: // retrofit2.Retrofit 
          return (T) NetworkModule_ProvideRetrofitFactory.provideRetrofit(singletonCImpl.provideOkHttpProvider.get(), singletonCImpl.provideGsonProvider.get());

          case 4: // okhttp3.OkHttpClient 
          return (T) NetworkModule_ProvideOkHttpFactory.provideOkHttp(singletonCImpl.authInterceptorProvider.get());

          case 5: // com.smartcbwtf.mobile.network.AuthInterceptor 
          return (T) new AuthInterceptor(singletonCImpl.defaultAuthTokenStoreProvider.get());

          case 6: // com.smartcbwtf.mobile.storage.DefaultAuthTokenStore 
          return (T) new DefaultAuthTokenStore(singletonCImpl.provideSharedPreferencesProvider.get());

          case 7: // android.content.SharedPreferences 
          return (T) AppModule_ProvideSharedPreferencesFactory.provideSharedPreferences(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 8: // com.google.gson.Gson 
          return (T) NetworkModule_ProvideGsonFactory.provideGson();

          case 9: // com.smartcbwtf.mobile.utils.LocationHelper 
          return (T) new LocationHelper(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 10: // com.smartcbwtf.mobile.storage.AppConfigStore 
          return (T) new AppConfigStore(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 11: // com.smartcbwtf.mobile.work.SyncAttendanceWorker_AssistedFactory 
          return (T) new SyncAttendanceWorker_AssistedFactory() {
            @Override
            public SyncAttendanceWorker create(Context appContext2, WorkerParameters params2) {
              return new SyncAttendanceWorker(appContext2, params2, singletonCImpl.defaultAttendanceRepositoryProvider.get());
            }
          };

          case 12: // com.smartcbwtf.mobile.repository.DefaultAttendanceRepository 
          return (T) new DefaultAttendanceRepository(singletonCImpl.attendanceDao(), singletonCImpl.provideAttendanceApiProvider.get(), singletonCImpl.provideIoDispatcherProvider.get());

          case 13: // com.smartcbwtf.mobile.database.AppDatabase 
          return (T) DatabaseModule_ProvideDatabaseFactory.provideDatabase(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 14: // com.smartcbwtf.mobile.network.api.AttendanceApi 
          return (T) NetworkModule_ProvideAttendanceApiFactory.provideAttendanceApi(singletonCImpl.provideRetrofitProvider.get());

          case 15: // kotlinx.coroutines.CoroutineDispatcher 
          return (T) CoroutineDispatchersModule_ProvideIoDispatcherFactory.provideIoDispatcher();

          case 16: // com.smartcbwtf.mobile.work.SyncBagEventsWorker_AssistedFactory 
          return (T) new SyncBagEventsWorker_AssistedFactory() {
            @Override
            public SyncBagEventsWorker create(Context appContext3, WorkerParameters params3) {
              return new SyncBagEventsWorker(appContext3, params3, singletonCImpl.defaultBagEventRepositoryProvider.get());
            }
          };

          case 17: // com.smartcbwtf.mobile.repository.DefaultBagEventRepository 
          return (T) new DefaultBagEventRepository(singletonCImpl.bagEventDao(), singletonCImpl.provideBagEventApiProvider.get(), singletonCImpl.provideIoDispatcherProvider.get());

          case 18: // com.smartcbwtf.mobile.network.api.BagEventApi 
          return (T) NetworkModule_ProvideBagEventApiFactory.provideBagEventApi(singletonCImpl.provideRetrofitProvider.get());

          case 19: // com.smartcbwtf.mobile.repository.DefaultAuthRepository 
          return (T) new DefaultAuthRepository(singletonCImpl.provideAuthApiProvider.get(), singletonCImpl.defaultAuthTokenStoreProvider.get(), singletonCImpl.appConfigStoreProvider.get(), singletonCImpl.networkMonitorProvider.get(), singletonCImpl.provideIoDispatcherProvider.get());

          case 20: // com.smartcbwtf.mobile.network.api.AuthApi 
          return (T) NetworkModule_ProvideAuthApiFactory.provideAuthApi(singletonCImpl.provideRetrofitProvider.get());

          case 21: // com.smartcbwtf.mobile.utils.NetworkMonitor 
          return (T) new NetworkMonitor(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 22: // com.smartcbwtf.mobile.utils.PermissionHelper 
          return (T) new PermissionHelper(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 23: // com.smartcbwtf.mobile.repository.DefaultHcfRepository 
          return (T) new DefaultHcfRepository(singletonCImpl.hcfDao(), singletonCImpl.provideHcfApiProvider.get(), singletonCImpl.networkMonitorProvider.get(), singletonCImpl.provideIoDispatcherProvider.get());

          case 24: // com.smartcbwtf.mobile.network.api.HcfApi 
          return (T) NetworkModule_ProvideHcfApiFactory.provideHcfApi(singletonCImpl.provideRetrofitProvider.get());

          case 25: // com.smartcbwtf.mobile.network.api.ProfileApi 
          return (T) NetworkModule_ProvideProfileApiFactory.provideProfileApi(singletonCImpl.provideRetrofitProvider.get());

          case 26: // com.smartcbwtf.mobile.storage.SessionManager 
          return (T) new SessionManager(singletonCImpl.provideSharedPreferencesProvider.get());

          case 27: // com.smartcbwtf.mobile.repository.ProfileRepository 
          return (T) new ProfileRepository(singletonCImpl.provideProfileApiProvider.get(), singletonCImpl.userProfileDao());

          case 28: // androidx.work.WorkManager 
          return (T) AppModule_ProvideWorkManagerFactory.provideWorkManager(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 29: // com.smartcbwtf.mobile.repository.RouteRepository 
          return (T) new RouteRepository(singletonCImpl.provideRouteApiProvider.get());

          case 30: // com.smartcbwtf.mobile.network.api.RouteApi 
          return (T) NetworkModule_ProvideRouteApiFactory.provideRouteApi(singletonCImpl.provideRetrofitProvider.get());

          case 31: // com.smartcbwtf.mobile.bluetooth.ScaleService 
          return (T) ScaleModule_Companion_ProvideScaleServiceFactory.provideScaleService(singletonCImpl.realBluetoothScaleServiceProvider.get(), singletonCImpl.mockScaleServiceProvider.get());

          case 32: // com.smartcbwtf.mobile.bluetooth.RealBluetoothScaleService 
          return (T) new RealBluetoothScaleService(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.permissionHelperProvider.get());

          case 33: // com.smartcbwtf.mobile.bluetooth.MockScaleService 
          return (T) new MockScaleService();

          case 34: // com.smartcbwtf.mobile.network.api.VerificationApi 
          return (T) NetworkModule_ProvideVerificationApiFactory.provideVerificationApi(singletonCImpl.provideRetrofitProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
