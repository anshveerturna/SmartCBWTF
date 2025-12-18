package com.smartcbwtf.mobile.ui;

import com.smartcbwtf.mobile.utils.PermissionHelper;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class ScanWeighFragment_MembersInjector implements MembersInjector<ScanWeighFragment> {
  private final Provider<PermissionHelper> permissionHelperProvider;

  public ScanWeighFragment_MembersInjector(Provider<PermissionHelper> permissionHelperProvider) {
    this.permissionHelperProvider = permissionHelperProvider;
  }

  public static MembersInjector<ScanWeighFragment> create(
      Provider<PermissionHelper> permissionHelperProvider) {
    return new ScanWeighFragment_MembersInjector(permissionHelperProvider);
  }

  @Override
  public void injectMembers(ScanWeighFragment instance) {
    injectPermissionHelper(instance, permissionHelperProvider.get());
  }

  @InjectedFieldSignature("com.smartcbwtf.mobile.ui.ScanWeighFragment.permissionHelper")
  public static void injectPermissionHelper(ScanWeighFragment instance,
      PermissionHelper permissionHelper) {
    instance.permissionHelper = permissionHelper;
  }
}
