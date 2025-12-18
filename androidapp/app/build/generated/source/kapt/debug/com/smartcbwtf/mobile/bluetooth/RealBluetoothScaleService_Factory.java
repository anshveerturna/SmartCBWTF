package com.smartcbwtf.mobile.bluetooth;

import android.content.Context;
import com.smartcbwtf.mobile.utils.PermissionHelper;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class RealBluetoothScaleService_Factory implements Factory<RealBluetoothScaleService> {
  private final Provider<Context> contextProvider;

  private final Provider<PermissionHelper> permissionHelperProvider;

  public RealBluetoothScaleService_Factory(Provider<Context> contextProvider,
      Provider<PermissionHelper> permissionHelperProvider) {
    this.contextProvider = contextProvider;
    this.permissionHelperProvider = permissionHelperProvider;
  }

  @Override
  public RealBluetoothScaleService get() {
    return newInstance(contextProvider.get(), permissionHelperProvider.get());
  }

  public static RealBluetoothScaleService_Factory create(Provider<Context> contextProvider,
      Provider<PermissionHelper> permissionHelperProvider) {
    return new RealBluetoothScaleService_Factory(contextProvider, permissionHelperProvider);
  }

  public static RealBluetoothScaleService newInstance(Context context,
      PermissionHelper permissionHelper) {
    return new RealBluetoothScaleService(context, permissionHelper);
  }
}
