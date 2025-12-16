package com.smartcbwtf.mobile.ui.adapter;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00006\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0006\u0018\u00002\u0012\u0012\u0004\u0012\u00020\u0002\u0012\b\u0012\u00060\u0003R\u00020\u00000\u0001:\u0002\u0014\u0015B\u0019\u0012\u0012\u0010\u0004\u001a\u000e\u0012\u0004\u0012\u00020\u0002\u0012\u0004\u0012\u00020\u00060\u0005\u00a2\u0006\u0002\u0010\u0007J\u001c\u0010\n\u001a\u00020\u00062\n\u0010\u000b\u001a\u00060\u0003R\u00020\u00002\u0006\u0010\f\u001a\u00020\rH\u0016J\u001c\u0010\u000e\u001a\u00060\u0003R\u00020\u00002\u0006\u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\rH\u0016J\u0010\u0010\u0012\u001a\u00020\u00062\b\u0010\u0013\u001a\u0004\u0018\u00010\tR\u001a\u0010\u0004\u001a\u000e\u0012\u0004\u0012\u00020\u0002\u0012\u0004\u0012\u00020\u00060\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\b\u001a\u0004\u0018\u00010\tX\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0016"}, d2 = {"Lcom/smartcbwtf/mobile/ui/adapter/HcfSelectionAdapter;", "Landroidx/recyclerview/widget/ListAdapter;", "Lcom/smartcbwtf/mobile/viewmodel/NearbyHcf;", "Lcom/smartcbwtf/mobile/ui/adapter/HcfSelectionAdapter$HcfViewHolder;", "onHcfSelected", "Lkotlin/Function1;", "", "(Lkotlin/jvm/functions/Function1;)V", "selectedHcfId", "", "onBindViewHolder", "holder", "position", "", "onCreateViewHolder", "parent", "Landroid/view/ViewGroup;", "viewType", "setSelectedHcf", "hcfId", "HcfDiffCallback", "HcfViewHolder", "app_debug"})
public final class HcfSelectionAdapter extends androidx.recyclerview.widget.ListAdapter<com.smartcbwtf.mobile.viewmodel.NearbyHcf, com.smartcbwtf.mobile.ui.adapter.HcfSelectionAdapter.HcfViewHolder> {
    @org.jetbrains.annotations.NotNull()
    private final kotlin.jvm.functions.Function1<com.smartcbwtf.mobile.viewmodel.NearbyHcf, kotlin.Unit> onHcfSelected = null;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String selectedHcfId;
    
    public HcfSelectionAdapter(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.smartcbwtf.mobile.viewmodel.NearbyHcf, kotlin.Unit> onHcfSelected) {
        super(null);
    }
    
    public final void setSelectedHcf(@org.jetbrains.annotations.Nullable()
    java.lang.String hcfId) {
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public com.smartcbwtf.mobile.ui.adapter.HcfSelectionAdapter.HcfViewHolder onCreateViewHolder(@org.jetbrains.annotations.NotNull()
    android.view.ViewGroup parent, int viewType) {
        return null;
    }
    
    @java.lang.Override()
    public void onBindViewHolder(@org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.ui.adapter.HcfSelectionAdapter.HcfViewHolder holder, int position) {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0004\u0018\u00002\b\u0012\u0004\u0012\u00020\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0003J\u0018\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00022\u0006\u0010\u0007\u001a\u00020\u0002H\u0016J\u0018\u0010\b\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00022\u0006\u0010\u0007\u001a\u00020\u0002H\u0016\u00a8\u0006\t"}, d2 = {"Lcom/smartcbwtf/mobile/ui/adapter/HcfSelectionAdapter$HcfDiffCallback;", "Landroidx/recyclerview/widget/DiffUtil$ItemCallback;", "Lcom/smartcbwtf/mobile/viewmodel/NearbyHcf;", "()V", "areContentsTheSame", "", "oldItem", "newItem", "areItemsTheSame", "app_debug"})
    public static final class HcfDiffCallback extends androidx.recyclerview.widget.DiffUtil.ItemCallback<com.smartcbwtf.mobile.viewmodel.NearbyHcf> {
        
        public HcfDiffCallback() {
            super();
        }
        
        @java.lang.Override()
        public boolean areItemsTheSame(@org.jetbrains.annotations.NotNull()
        com.smartcbwtf.mobile.viewmodel.NearbyHcf oldItem, @org.jetbrains.annotations.NotNull()
        com.smartcbwtf.mobile.viewmodel.NearbyHcf newItem) {
            return false;
        }
        
        @java.lang.Override()
        public boolean areContentsTheSame(@org.jetbrains.annotations.NotNull()
        com.smartcbwtf.mobile.viewmodel.NearbyHcf oldItem, @org.jetbrains.annotations.NotNull()
        com.smartcbwtf.mobile.viewmodel.NearbyHcf newItem) {
            return false;
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001e\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u0086\u0004\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u000e\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\bR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\t"}, d2 = {"Lcom/smartcbwtf/mobile/ui/adapter/HcfSelectionAdapter$HcfViewHolder;", "Landroidx/recyclerview/widget/RecyclerView$ViewHolder;", "binding", "Lcom/smartcbwtf/mobile/databinding/ItemHcfSelectionBinding;", "(Lcom/smartcbwtf/mobile/ui/adapter/HcfSelectionAdapter;Lcom/smartcbwtf/mobile/databinding/ItemHcfSelectionBinding;)V", "bind", "", "nearbyHcf", "Lcom/smartcbwtf/mobile/viewmodel/NearbyHcf;", "app_debug"})
    public final class HcfViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        @org.jetbrains.annotations.NotNull()
        private final com.smartcbwtf.mobile.databinding.ItemHcfSelectionBinding binding = null;
        
        public HcfViewHolder(@org.jetbrains.annotations.NotNull()
        com.smartcbwtf.mobile.databinding.ItemHcfSelectionBinding binding) {
            super(null);
        }
        
        public final void bind(@org.jetbrains.annotations.NotNull()
        com.smartcbwtf.mobile.viewmodel.NearbyHcf nearbyHcf) {
        }
    }
}