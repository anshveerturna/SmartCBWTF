package com.smartcbwtf.mobile.repository;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000<\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\bf\u0018\u00002\u00020\u0001J\u0014\u0010\u0002\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00050\u00040\u0003H&J\u001a\u0010\u0006\u001a\u00020\u00072\n\b\u0002\u0010\b\u001a\u0004\u0018\u00010\tH\u00a6@\u00a2\u0006\u0002\u0010\nJ\u000e\u0010\u000b\u001a\u00020\fH\u00a6@\u00a2\u0006\u0002\u0010\rJ\u0016\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u0011H\u00a6@\u00a2\u0006\u0002\u0010\u0012\u00a8\u0006\u0013"}, d2 = {"Lcom/smartcbwtf/mobile/repository/HcfRepository;", "", "getAll", "Lkotlinx/coroutines/flow/Flow;", "", "Lcom/smartcbwtf/mobile/database/entity/HcfEntity;", "getLatestTerms", "Lcom/smartcbwtf/mobile/network/model/TermsResponse;", "facilityId", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "refresh", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "register", "Lcom/smartcbwtf/mobile/network/model/HcfRegistrationResponse;", "request", "Lcom/smartcbwtf/mobile/network/model/HcfRegistrationRequest;", "(Lcom/smartcbwtf/mobile/network/model/HcfRegistrationRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public abstract interface HcfRepository {
    
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.smartcbwtf.mobile.database.entity.HcfEntity>> getAll();
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object refresh(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    /**
     * Register a new HCF with full agreement details.
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object register(@org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.network.model.HcfRegistrationRequest request, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.smartcbwtf.mobile.network.model.HcfRegistrationResponse> $completion);
    
    /**
     * Get the latest active terms and conditions for the facility.
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getLatestTerms(@org.jetbrains.annotations.Nullable()
    java.lang.String facilityId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.smartcbwtf.mobile.network.model.TermsResponse> $completion);
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 3, xi = 48)
    public static final class DefaultImpls {
    }
}