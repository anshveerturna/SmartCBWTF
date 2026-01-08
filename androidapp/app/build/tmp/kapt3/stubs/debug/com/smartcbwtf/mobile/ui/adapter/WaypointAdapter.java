package com.smartcbwtf.mobile.ui.adapter;

/**
 * Adapter for displaying route waypoints (HCFs) in order.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00004\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u0002\n\u0002\b\u0004\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\u0018\u00002\u0012\u0012\u0004\u0012\u00020\u0002\u0012\b\u0012\u00060\u0003R\u00020\u00000\u0001:\u0002\u0012\u0013B\'\u0012\b\u0010\u0004\u001a\u0004\u0018\u00010\u0005\u0012\u0016\b\u0002\u0010\u0006\u001a\u0010\u0012\u0004\u0012\u00020\u0002\u0012\u0004\u0012\u00020\b\u0018\u00010\u0007\u00a2\u0006\u0002\u0010\tJ\u001c\u0010\n\u001a\u00020\b2\n\u0010\u000b\u001a\u00060\u0003R\u00020\u00002\u0006\u0010\f\u001a\u00020\rH\u0016J\u001c\u0010\u000e\u001a\u00060\u0003R\u00020\u00002\u0006\u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\rH\u0016R\u001c\u0010\u0006\u001a\u0010\u0012\u0004\u0012\u00020\u0002\u0012\u0004\u0012\u00020\b\u0018\u00010\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0004\u001a\u0004\u0018\u00010\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0014"}, d2 = {"Lcom/smartcbwtf/mobile/ui/adapter/WaypointAdapter;", "Landroidx/recyclerview/widget/ListAdapter;", "Lcom/smartcbwtf/mobile/network/model/MobileWaypointDTO;", "Lcom/smartcbwtf/mobile/ui/adapter/WaypointAdapter$WaypointViewHolder;", "routeColor", "", "onWaypointClick", "Lkotlin/Function1;", "", "(Ljava/lang/String;Lkotlin/jvm/functions/Function1;)V", "onBindViewHolder", "holder", "position", "", "onCreateViewHolder", "parent", "Landroid/view/ViewGroup;", "viewType", "WaypointDiffCallback", "WaypointViewHolder", "app_debug"})
public final class WaypointAdapter extends androidx.recyclerview.widget.ListAdapter<com.smartcbwtf.mobile.network.model.MobileWaypointDTO, com.smartcbwtf.mobile.ui.adapter.WaypointAdapter.WaypointViewHolder> {
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String routeColor = null;
    @org.jetbrains.annotations.Nullable()
    private final kotlin.jvm.functions.Function1<com.smartcbwtf.mobile.network.model.MobileWaypointDTO, kotlin.Unit> onWaypointClick = null;
    
    public WaypointAdapter(@org.jetbrains.annotations.Nullable()
    java.lang.String routeColor, @org.jetbrains.annotations.Nullable()
    kotlin.jvm.functions.Function1<? super com.smartcbwtf.mobile.network.model.MobileWaypointDTO, kotlin.Unit> onWaypointClick) {
        super(null);
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public com.smartcbwtf.mobile.ui.adapter.WaypointAdapter.WaypointViewHolder onCreateViewHolder(@org.jetbrains.annotations.NotNull()
    android.view.ViewGroup parent, int viewType) {
        return null;
    }
    
    @java.lang.Override()
    public void onBindViewHolder(@org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.ui.adapter.WaypointAdapter.WaypointViewHolder holder, int position) {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0004\u0018\u00002\b\u0012\u0004\u0012\u00020\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0003J\u0018\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00022\u0006\u0010\u0007\u001a\u00020\u0002H\u0016J\u0018\u0010\b\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00022\u0006\u0010\u0007\u001a\u00020\u0002H\u0016\u00a8\u0006\t"}, d2 = {"Lcom/smartcbwtf/mobile/ui/adapter/WaypointAdapter$WaypointDiffCallback;", "Landroidx/recyclerview/widget/DiffUtil$ItemCallback;", "Lcom/smartcbwtf/mobile/network/model/MobileWaypointDTO;", "()V", "areContentsTheSame", "", "oldItem", "newItem", "areItemsTheSame", "app_debug"})
    public static final class WaypointDiffCallback extends androidx.recyclerview.widget.DiffUtil.ItemCallback<com.smartcbwtf.mobile.network.model.MobileWaypointDTO> {
        
        public WaypointDiffCallback() {
            super();
        }
        
        @java.lang.Override()
        public boolean areItemsTheSame(@org.jetbrains.annotations.NotNull()
        com.smartcbwtf.mobile.network.model.MobileWaypointDTO oldItem, @org.jetbrains.annotations.NotNull()
        com.smartcbwtf.mobile.network.model.MobileWaypointDTO newItem) {
            return false;
        }
        
        @java.lang.Override()
        public boolean areContentsTheSame(@org.jetbrains.annotations.NotNull()
        com.smartcbwtf.mobile.network.model.MobileWaypointDTO oldItem, @org.jetbrains.annotations.NotNull()
        com.smartcbwtf.mobile.network.model.MobileWaypointDTO newItem) {
            return false;
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000,\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u0086\u0004\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u000e\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u000fR\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0010"}, d2 = {"Lcom/smartcbwtf/mobile/ui/adapter/WaypointAdapter$WaypointViewHolder;", "Landroidx/recyclerview/widget/RecyclerView$ViewHolder;", "itemView", "Landroid/view/View;", "(Lcom/smartcbwtf/mobile/ui/adapter/WaypointAdapter;Landroid/view/View;)V", "imgAttendanceStatus", "Landroid/widget/ImageView;", "textHcfAddress", "Landroid/widget/TextView;", "textHcfCode", "textHcfName", "textSequenceNumber", "bind", "", "waypoint", "Lcom/smartcbwtf/mobile/network/model/MobileWaypointDTO;", "app_debug"})
    public final class WaypointViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        @org.jetbrains.annotations.NotNull()
        private final android.widget.TextView textSequenceNumber = null;
        @org.jetbrains.annotations.NotNull()
        private final android.widget.TextView textHcfName = null;
        @org.jetbrains.annotations.NotNull()
        private final android.widget.TextView textHcfCode = null;
        @org.jetbrains.annotations.NotNull()
        private final android.widget.TextView textHcfAddress = null;
        @org.jetbrains.annotations.NotNull()
        private final android.widget.ImageView imgAttendanceStatus = null;
        
        public WaypointViewHolder(@org.jetbrains.annotations.NotNull()
        android.view.View itemView) {
            super(null);
        }
        
        public final void bind(@org.jetbrains.annotations.NotNull()
        com.smartcbwtf.mobile.network.model.MobileWaypointDTO waypoint) {
        }
    }
}