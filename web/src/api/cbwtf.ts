import apiClient from './client';

// ============= Types =============

export interface RiskAlert {
  severity: 'CRITICAL' | 'HIGH' | 'MEDIUM';
  type: 'SUBSCRIPTION_EXPIRY' | 'CPCB_OVERDUE' | 'INVOICE_OVERDUE' | 'VEHICLE_OFFLINE' | 'AGREEMENT_EXPIRY';
  title: string;
  description: string;
  entityId: string | null;
}

export interface RecentBagEvent {
  qrCode: string | null;
  hcfName: string | null;
  eventType: string;
  anomalyState: string | null;
  eventTs: string;
}

export interface AgreementSummary {
  agreementNumber: string;
  hcfName: string | null;
  status: string;
  duesStatus: string;
  endDate: string | null;
  daysUntilExpiry: number;
}

export interface CBWTFDashboardDTO {
  // Overview Metrics
  activeAgreements: number;
  totalAgreements: number;
  activeHcfs: number;
  totalBagLabelsIssued: number;
  bagsProcessedToday: number;
  bagsProcessedThisWeek: number;
  bagsProcessedThisMonth: number;

  // Vehicle & Staff Metrics
  vehiclesOnline: number;
  totalVehicles: number;
  staffPresentToday: number;
  totalStaff: number;

  // Financial Metrics
  pendingInvoiceAmount: number;
  pendingInvoiceCount: number;
  paidInvoiceAmountThisMonth: number;
  paidInvoiceCountThisMonth: number;
  totalRevenueAllTime: number;

  // Health Metrics
  agreementsExpiringSoon: number;
  agreementsWithDuesPending: number;
  agreementsInDispute: number;
  anomalyBagsThisWeek: number;

  // Recent Activity
  recentBagEvents: RecentBagEvent[];
  expiringAgreements: AgreementSummary[];

  // Risk Alerts
  riskAlerts: RiskAlert[];

  // Facility Info
  facilityName: string;
  subscriptionPlan: string;
  subscriptionExpiresAt: string | null;
  subscriptionDaysLeft: number;
}

// ============= API Functions =============

export const cbwtfApi = {
  /**
   * Get dashboard metrics for the current CBWTF.
   * Requires CBWTF_ADMIN role.
   */
  getDashboard: async (): Promise<CBWTFDashboardDTO> => {
    const response = await apiClient.get('/api/cbwtf/dashboard');
    return response.data;
  },
};

export default cbwtfApi;
