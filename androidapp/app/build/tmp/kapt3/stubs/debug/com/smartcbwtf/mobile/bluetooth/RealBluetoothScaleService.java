package com.smartcbwtf.mobile.bluetooth;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0088\u0001\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0006\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010#\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u000e\b\u0007\u0018\u00002\u00020\u0001B\u0019\b\u0007\u0012\b\b\u0001\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u000e\u0010.\u001a\u00020/H\u0096@\u00a2\u0006\u0002\u00100J\u0016\u00101\u001a\u00020/2\u0006\u00102\u001a\u00020\u0010H\u0097@\u00a2\u0006\u0002\u00103J\u000e\u00104\u001a\u00020/H\u0097@\u00a2\u0006\u0002\u00100J\u0010\u00105\u001a\u00020/2\u0006\u00106\u001a\u00020!H\u0002J\u0018\u00107\u001a\u00020/2\b\u00108\u001a\u0004\u0018\u00010\u0012H\u0096@\u00a2\u0006\u0002\u00109J\u000e\u0010:\u001a\u00020/H\u0097@\u00a2\u0006\u0002\u00100J\u000e\u0010;\u001a\u00020/H\u0096@\u00a2\u0006\u0002\u00100J\b\u0010<\u001a\u00020/H\u0003R\u0016\u0010\u0007\u001a\n \t*\u0004\u0018\u00010\b0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\n\u001a\n \t*\u0004\u0018\u00010\b0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\r0\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u000e\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00100\u000f0\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u0011\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00120\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u0013\u001a\n \t*\u0004\u0018\u00010\u00140\u0014X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0015\u001a\u0004\u0018\u00010\u0016X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0017\u001a\u00020\u0018X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0019\u001a\u0004\u0018\u00010\u001aX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u001b\u001a\b\u0012\u0004\u0012\u00020\r0\u001cX\u0096\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001d\u0010\u001eR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u001f\u001a\b\u0012\u0004\u0012\u00020!0 X\u0082\u0004\u00a2\u0006\u0002\n\u0000R \u0010\"\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00100\u000f0\u001cX\u0096\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b#\u0010\u001eR\u000e\u0010$\u001a\u00020%X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010&\u001a\b\u0012\u0004\u0012\u00020\u00120\'X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010(\u001a\u0004\u0018\u00010)X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010*\u001a\u00020+X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001c\u0010,\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00120\u001cX\u0096\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b-\u0010\u001e\u00a8\u0006="}, d2 = {"Lcom/smartcbwtf/mobile/bluetooth/RealBluetoothScaleService;", "Lcom/smartcbwtf/mobile/bluetooth/ScaleService;", "context", "Landroid/content/Context;", "permissionHelper", "Lcom/smartcbwtf/mobile/utils/PermissionHelper;", "(Landroid/content/Context;Lcom/smartcbwtf/mobile/utils/PermissionHelper;)V", "CHARACTERISTIC_UUID", "Ljava/util/UUID;", "kotlin.jvm.PlatformType", "SERVICE_UUID", "_connectionState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/smartcbwtf/mobile/bluetooth/ConnectionState;", "_discoveredDevices", "", "Landroid/bluetooth/BluetoothDevice;", "_weight", "", "bluetoothAdapter", "Landroid/bluetooth/BluetoothAdapter;", "bluetoothGatt", "Landroid/bluetooth/BluetoothGatt;", "bluetoothManager", "Landroid/bluetooth/BluetoothManager;", "connectionJob", "Lkotlinx/coroutines/Job;", "connectionState", "Lkotlinx/coroutines/flow/StateFlow;", "getConnectionState", "()Lkotlinx/coroutines/flow/StateFlow;", "discoveredDeviceSet", "", "", "discoveredDevices", "getDiscoveredDevices", "gattCallback", "Landroid/bluetooth/BluetoothGattCallback;", "recentWeights", "Lkotlin/collections/ArrayDeque;", "scanCallback", "Landroid/bluetooth/le/ScanCallback;", "scope", "Lkotlinx/coroutines/CoroutineScope;", "weight", "getWeight", "connect", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "connectToDevice", "device", "(Landroid/bluetooth/BluetoothDevice;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "disconnect", "parseWeight", "data", "simulateWeight", "value", "(Ljava/lang/Double;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "startScan", "stopScan", "stopScanInternal", "app_debug"})
public final class RealBluetoothScaleService implements com.smartcbwtf.mobile.bluetooth.ScaleService {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private final com.smartcbwtf.mobile.utils.PermissionHelper permissionHelper = null;
    @org.jetbrains.annotations.NotNull()
    private final android.bluetooth.BluetoothManager bluetoothManager = null;
    private final android.bluetooth.BluetoothAdapter bluetoothAdapter = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Double> _weight = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Double> weight = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.smartcbwtf.mobile.bluetooth.ConnectionState> _connectionState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.smartcbwtf.mobile.bluetooth.ConnectionState> connectionState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.util.List<android.bluetooth.BluetoothDevice>> _discoveredDevices = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<android.bluetooth.BluetoothDevice>> discoveredDevices = null;
    @org.jetbrains.annotations.Nullable()
    private android.bluetooth.BluetoothGatt bluetoothGatt;
    @org.jetbrains.annotations.Nullable()
    private android.bluetooth.le.ScanCallback scanCallback;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.CoroutineScope scope = null;
    @org.jetbrains.annotations.Nullable()
    private kotlinx.coroutines.Job connectionJob;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.collections.ArrayDeque<java.lang.Double> recentWeights = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Set<java.lang.String> discoveredDeviceSet = null;
    private final java.util.UUID SERVICE_UUID = null;
    private final java.util.UUID CHARACTERISTIC_UUID = null;
    @org.jetbrains.annotations.NotNull()
    private final android.bluetooth.BluetoothGattCallback gattCallback = null;
    
    @javax.inject.Inject()
    public RealBluetoothScaleService(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.utils.PermissionHelper permissionHelper) {
        super();
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public kotlinx.coroutines.flow.StateFlow<java.lang.Double> getWeight() {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public kotlinx.coroutines.flow.StateFlow<com.smartcbwtf.mobile.bluetooth.ConnectionState> getConnectionState() {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public kotlinx.coroutines.flow.StateFlow<java.util.List<android.bluetooth.BluetoothDevice>> getDiscoveredDevices() {
        return null;
    }
    
    @java.lang.Override()
    @android.annotation.SuppressLint(value = {"MissingPermission"})
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object startScan(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @android.annotation.SuppressLint(value = {"MissingPermission"})
    private final void stopScanInternal() {
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object stopScan(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @android.annotation.SuppressLint(value = {"MissingPermission"})
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object connectToDevice(@org.jetbrains.annotations.NotNull()
    android.bluetooth.BluetoothDevice device, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object connect(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @android.annotation.SuppressLint(value = {"MissingPermission"})
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object disconnect(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object simulateWeight(@org.jetbrains.annotations.Nullable()
    java.lang.Double value, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final void parseWeight(java.lang.String data) {
    }
}