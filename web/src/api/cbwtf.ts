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

export interface AnomalyBagDTO {
  id: string;
  eventTs: string;
  hcfName: string;
  category: string;
  anomalyState: string;
  weightKg: number;
  collectedByUserId: string;
  staffName: string | null;
  gpsLat: number;
  gpsLon: number;
  eventType: string;
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

  /**
   * Get category breakdown for pie chart.
   */
  getCategoryBreakdown: async (): Promise<{ name: string; value: number; color: string }[]> => {
    const response = await apiClient.get('/api/cbwtf/dashboard/category-breakdown');
    return response.data;
  },

  /**
   * Get weekly trend for area chart.
   */
  getWeeklyTrend: async (): Promise<{ date: string; yellow: number; red: number; blue: number; white: number }[]> => {
    const response = await apiClient.get('/api/cbwtf/dashboard/weekly-trend');
    return response.data;
  },

  /**
   * Get trend comparison (today vs yesterday).
   */
  getTrendComparison: async (): Promise<{ todayBags: number; yesterdayBags: number; percentChange: number; isPositive: boolean }> => {
    const response = await apiClient.get('/api/cbwtf/dashboard/trend-comparison');
    return response.data;
  },

  /**
   * Get anomaly bags for the current week.
   */
  getAnomalyBags: async (): Promise<AnomalyBagDTO[]> => {
    const response = await apiClient.get('/api/cbwtf/dashboard/anomaly-bags');
    return response.data;
  },
};

// ============= Analytics Page Types =============

export interface TotalWasteResponse {
  totalWeightKg: number;
  periodLabel: string;
  eventCount: number;
}

export interface CategoryBreakdown {
  category: string;
  weightKg: number;
  percentContribution: number;
}

export interface WasteByCategoryResponse {
  categories: CategoryBreakdown[];
  grandTotalKg: number;
}

export interface HcfOption {
  id: string;
  name: string;
}

export interface ProcessedBagEntry {
  id: string;
  category: string;
  qrCode: string;
  weightKg: number;
  timestamp: string;
  staffName: string;
  hcfName: string;
  eventType: string;
  anomalyState: string | null;
}

export interface ProcessedBagsResponse {
  bags: ProcessedBagEntry[];
  totalCount: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

// ============= Analytics Page API Functions =============

export const getAnalyticsTotalWaste = async (
  from: string,
  to: string,
  hcfId?: string
): Promise<TotalWasteResponse> => {
  const params = new URLSearchParams({ from, to });
  if (hcfId) params.append('hcfId', hcfId);
  const response = await apiClient.get(`/api/analytics/page/total-waste?${params}`);
  return response.data;
};

export const getAnalyticsWasteByCategory = async (
  from: string,
  to: string,
  hcfId?: string
): Promise<WasteByCategoryResponse> => {
  const params = new URLSearchParams({ from, to });
  if (hcfId) params.append('hcfId', hcfId);
  const response = await apiClient.get(`/api/analytics/page/waste-by-category?${params}`);
  return response.data;
};

export const getAnalyticsActiveHcfs = async (): Promise<HcfOption[]> => {
  const response = await apiClient.get('/api/analytics/page/hcfs/active');
  return response.data;
};

export const getAnalyticsProcessedBags = async (
  from: string,
  to: string,
  hcfId?: string,
  page: number = 0,
  pageSize: number = 20
): Promise<ProcessedBagsResponse> => {
  const params = new URLSearchParams({ from, to, page: page.toString(), pageSize: pageSize.toString() });
  if (hcfId) params.append('hcfId', hcfId);
  const response = await apiClient.get(`/api/analytics/page/processed-bags?${params}`);
  return response.data;
};

// ============= Vehicle & GPS Types =============

export interface VehicleDTO {
  id: string;
  registrationNumber: string;
  vehicleType: string;
  gpsStatus: string;
  lastGpsAt: string | null;
  lastLatitude: number | null;
  lastLongitude: number | null;
  driverName: string | null;
  status: string;
}

export interface LivePositionDTO {
  id: string;
  registrationNumber: string;
  vehicleType: string;
  latitude: number | null;
  longitude: number | null;
  lastGpsAt: string | null;
  gpsStatus: string;
  driverName: string | null;
}

export interface LiveMapDTO {
  vehicles: LivePositionDTO[];
  onlineCount: number;
  totalCount: number;
  timestamp: string;
}

export interface GpsLocationDTO {
  latitude: number;
  longitude: number;
  speed: number | null;
  heading: number | null;
  recordedAt: string;
}

// ============= Vehicle API Functions =============

export const getVehicles = async (): Promise<VehicleDTO[]> => {
  const response = await apiClient.get('/api/cbwtf/vehicles');
  return response.data;
};

export const getVehicle = async (id: string): Promise<VehicleDTO> => {
  const response = await apiClient.get(`/api/cbwtf/vehicles/${id}`);
  return response.data;
};

export const getLiveMap = async (): Promise<LiveMapDTO> => {
  const response = await apiClient.get('/api/cbwtf/vehicles/live-map');
  return response.data;
};

export const getVehicleLastLocation = async (id: string): Promise<GpsLocationDTO | null> => {
  const response = await apiClient.get(`/api/cbwtf/vehicles/${id}/last-location`);
  return response.data || null;
};

export const getVehicleTrail = async (id: string, limit = 50): Promise<GpsLocationDTO[]> => {
  const response = await apiClient.get(`/api/cbwtf/vehicles/${id}/trail?limit=${limit}`);
  return response.data;
};

// ============= Facility Settings Types =============

export interface LegalProfileDTO {
  legalName?: string;
  tradeName?: string;
  authorizationNumber?: string;
  spcbName?: string;
  spcbState?: string;
  gstin?: string;
  pan?: string;
  registeredAddress?: string;
  registeredState?: string;
  registeredPincode?: string;
  officialEmail?: string;
  officialPhone?: string;
  logoUrl?: string;
  logoChecksum?: string;
  signatureUrl?: string;
  signatureChecksum?: string;
}

export interface FinancialSettingsDTO {
  cgstPercent: number;
  sgstPercent: number;
  igstPercent: number;
  gstEnabled: boolean;
}

export interface PaymentReminderDTO {
  gracePeriodDays: number;
  autoAlertEscalation: boolean;
}

export interface AgreementRulesDTO {
  defaultAgreementValidityMonths: number;
  agreementRenewalWindowDays: number;
  blockOverlappingAgreements: boolean;
}

export interface OperationalRulesDTO {
  qrValidityDays: number;
  allowMultipleActiveQrs: boolean;
  requireCbwtfVerification: boolean;
  gpsGeofenceRadiusM: number;
  maxUnverifiedBags: number;
  blueWasteMinPercent: number;
}

export interface ComplianceSettingsDTO {
  dailyReportTime: string;
  monthlyReportDay: number;
  annualFormIvDate?: string;
  enforceChecksum: boolean;
}

export interface EmailSettingsDTO {
  // Read-only: System-computed sender display name
  resolvedSenderName: string;
  // Read-only: System-computed sender email address
  resolvedSenderEmail: string;
  // Read-only: Whether sender slug is locked
  senderSlugLocked: boolean;
  // Editable: Use generic sender (no-reply@smartcbwtf.com)
  useGenericSender: boolean;
  // Editable: CBWTF notification receiving email
  notificationEmail: string | null;
  // Editable: CC admin on HCF emails
  ccAdminOnHcfEmails: boolean;
  // Editable: Enable email notifications
  emailNotificationsEnabled: boolean;
  // Editable: Enable in-app alerts
  inAppAlertsEnabled: boolean;
}

export interface LockedFieldsDTO {
  gstLocked: boolean;
  complianceLocked: boolean;
  qrRulesLocked: boolean;
  firstInvoiceAt?: string;
  firstQrGeneratedAt?: string;
  firstComplianceReportAt?: string;
}

export interface FacilitySettingsDTO {
  settingsVersion: number;
  legal: LegalProfileDTO;
  financial: FinancialSettingsDTO;
  paymentReminders: PaymentReminderDTO;
  agreementRules: AgreementRulesDTO;
  operational: OperationalRulesDTO;
  compliance: ComplianceSettingsDTO;
  email: EmailSettingsDTO;
  lockedFields: LockedFieldsDTO;
  createdAt: string;
  updatedAt: string;
}

export interface SettingsAuditDTO {
  id: string;
  section: string;
  settingKey: string;
  oldValue: string;
  newValue: string;
  changedBy: string;
  changedByUsername: string;
  changedAt: string;
  ipAddress?: string;
}

export interface SystemReadinessResult {
  ready: boolean;
  errors: string[];
}

// ============= Settings API Functions =============

export const getFacilitySettings = async (): Promise<FacilitySettingsDTO> => {
  const response = await apiClient.get<FacilitySettingsDTO>('/api/cbwtf/settings');
  return response.data;
};

export const checkSystemReadiness = async (): Promise<SystemReadinessResult> => {
  const response = await apiClient.get<SystemReadinessResult>('/api/cbwtf/settings/readiness');
  return response.data;
};

export const updateLegalProfile = async (data: LegalProfileDTO): Promise<void> => {
  await apiClient.put('/api/cbwtf/settings/legal', data);
};

export const updateFinancialSettings = async (data: FinancialSettingsDTO): Promise<void> => {
  await apiClient.put('/api/cbwtf/settings/financial', data);
};

export const updatePaymentReminders = async (data: PaymentReminderDTO): Promise<void> => {
  await apiClient.put('/api/cbwtf/settings/payment-reminders', data);
};

export const updateAgreementRules = async (data: AgreementRulesDTO): Promise<void> => {
  await apiClient.put('/api/cbwtf/settings/agreement-rules', data);
};

export const updateOperationalRules = async (data: OperationalRulesDTO): Promise<void> => {
  await apiClient.put('/api/cbwtf/settings/operational', data);
};

export const updateComplianceSettings = async (data: ComplianceSettingsDTO): Promise<void> => {
  await apiClient.put('/api/cbwtf/settings/compliance', data);
};

export const updateEmailSettings = async (data: EmailSettingsDTO): Promise<void> => {
  await apiClient.put('/api/cbwtf/settings/email', data);
};

export const getSettingsAuditHistory = async (
  section?: string,
  page = 0,
  size = 20
): Promise<{ content: SettingsAuditDTO[]; totalElements: number }> => {
  const params = new URLSearchParams();
  if (section) params.append('section', section);
  params.append('page', page.toString());
  params.append('size', size.toString());
  
  const response = await apiClient.get<{ content: SettingsAuditDTO[]; totalElements: number }>(
    `/api/cbwtf/settings/audit-history?${params.toString()}`
  );
  return response.data;
};

// ============= Staff Management Types =============

export interface StaffDTO {
  id: string;
  username: string;
  fullName: string;
  email: string | null;
  phone: string | null;
  gender: string | null;
  dob: string | null;
  profilePhotoUrl: string | null;
  role: 'DRIVER' | 'PLANT_OPERATOR';
  active: boolean;
  gpsStatus: 'ONLINE' | 'OFFLINE' | 'NEVER';
  lastGpsAt: string | null;
  createdAt: string;
  tempPassword?: string; // Only returned on creation
}

export interface StaffDetailDTO extends StaffDTO {
  lastGpsLat: number | null;
  lastGpsLon: number | null;
  lastAttendanceHcf: string | null;
  lastAttendanceAt: string | null;
  lastLoginAt: string | null;
  updatedAt: string;
}

export interface CreateStaffRequest {
  fullName: string;
  email?: string;
  phone?: string;
  role: 'DRIVER' | 'PLANT_OPERATOR';
  password?: string;
}

export interface UpdateStaffRequest {
  fullName: string;
  email?: string;
  phone?: string;
  gender?: string;
  dob?: string;
  profilePhotoUrl?: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

// ============= Staff API Functions =============

export const getStaffList = async (page = 0, size = 20, role?: string): Promise<PageResponse<StaffDTO>> => {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  if (role) params.append('role', role);
  const response = await apiClient.get(`/api/cbwtf/staff?${params.toString()}`);
  return response.data;
};

export const getStaffDetail = async (id: string): Promise<StaffDetailDTO> => {
  const response = await apiClient.get(`/api/cbwtf/staff/${id}`);
  return response.data;
};

export const createStaff = async (data: CreateStaffRequest): Promise<StaffDTO> => {
  const response = await apiClient.post('/api/cbwtf/staff', data);
  return response.data;
};

export const updateStaff = async (id: string, data: UpdateStaffRequest): Promise<StaffDTO> => {
  const response = await apiClient.put(`/api/cbwtf/staff/${id}`, data);
  return response.data;
};

export const disableStaff = async (id: string): Promise<StaffDTO> => {
  const response = await apiClient.post(`/api/cbwtf/staff/${id}/disable`);
  return response.data;
};

export const enableStaff = async (id: string): Promise<StaffDTO> => {
  const response = await apiClient.post(`/api/cbwtf/staff/${id}/enable`);
  return response.data;
};

export const unlockStaff = async (id: string): Promise<StaffDTO> => {
  const response = await apiClient.post(`/api/cbwtf/staff/${id}/unlock`);
  return response.data;
};

export interface UpdateCredentialsRequest {
  username?: string;
  password?: string;
  forcePasswordChange?: boolean;
}

export const updateStaffCredentials = async (id: string, data: UpdateCredentialsRequest): Promise<StaffDTO> => {
  const response = await apiClient.put(`/api/cbwtf/staff/${id}/credentials`, data);
  return response.data;
};

export const requestGpsRefresh = async (id: string): Promise<void> => {
  await apiClient.post(`/api/cbwtf/staff/${id}/request-gps-refresh`);
};

export const uploadStaffPhoto = async (id: string, file: File): Promise<{ photoUrl: string }> => {
  const formData = new FormData();
  formData.append('file', file);
  const response = await apiClient.post(`/api/cbwtf/staff/${id}/photo`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
  return response.data;
};

export const removeStaffPhoto = async (id: string): Promise<{ message: string }> => {
  const response = await apiClient.delete(`/api/cbwtf/staff/${id}/photo`);
  return response.data;
};

// ============= HCF Management Types =============

// Billing model enum
export type BillingModel = 'BEDDED' | 'FIXED_MONTHLY';

// Approval status enum
export type ApprovalStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface HcfListItem {
  id: string;
  code: string;
  name: string;
  address: string;
  contactPhone: string | null;
  contactEmail: string | null;
  numberOfBeds: number | null;
  monthlyCharges: number | null;
  billingModel: BillingModel | null;
  approvalStatus: ApprovalStatus;
  rejectionReason: string | null;
  approvedBy: string | null;
  approvedAt: string | null;
  agreementId: string | null;
  agreementNumber: string | null;
  agreementStatus: string | null;
  duesStatus: string | null;
  agreementStartDate: string | null;
  agreementEndDate: string | null;
  lastPickupAt: string | null;
  createdAt: string;
  updatedAt: string | null;
  status: string | null;
  // Bed Access Category (regulatory classification)
  bedAccessCategory: 'BEDS_0_TO_30' | 'ABOVE_30_BEDS' | null;
  bedAccessCategoryDisplay: string | null;
  portalEligible: boolean;
  // New filter fields
  city: string | null;
  state: string | null;
  hcfType: 'HOSPITAL' | 'DENTAL' | 'CLINIC' | 'PATHOLOGY_COLLECTION' | 'PATHOLOGY_STORAGE' | null;
  hcfTypeDisplay: string | null;
  seatCount: number | null;
}

export interface AgreementInfo {
  id: string;
  agreementNumber: string;
  status: string;
  duesStatus: string;
  startDate: string;
  endDate: string | null;
  perBedPerDayRate: number;
  createdAt: string;
}

export interface BillingConfigInfo {
  id: string;
  baseGramsPerBedPerDay: number;
  baseRatePerBedPerDay: number;
  effectiveFrom: string;
  effectiveTo: string | null;
  active: boolean;
  // Global excess rate (from Facility)
  globalExcessRatePerKg: number | null;
  globalExcessRateEffectiveFrom: string | null;
}

export interface OperationalSummary {
  totalPickups: number;
  totalAttendanceMarks: number;
  lastPickupAt: string | null;
  lastAttendanceAt: string | null;
  totalWasteKg: number | null;
}

export interface HcfDetail {
  id: string;
  code: string;
  name: string;
  address: string;
  pincode: string | null;
  state: string | null;
  contactPhone: string | null;
  contactEmail: string | null;
  numberOfBeds: number | null;
  hcfStatus: string;
  gpsLat: number;
  gpsLon: number;
  doctorName: string | null;
  panNo: string | null;
  gstNo: string | null;
  pcbAuthorizationNo: string | null;
  aadharNo: string | null;
  monthlyCharges: number | null;
  bedded: boolean | null;
  otherNotes: string | null;
  registrationGpsLat: number | null;
  registrationGpsLon: number | null;
  registrationGpsAccuracy: number | null;
  registeredByUsername: string | null;
  createdAt: string;
  updatedAt: string;
  // Ownership information
  ownershipType: string | null;
  rentAgreementUrl: string | null;
  agreement: AgreementInfo | null;
  billingConfig: BillingConfigInfo | null;
  summary: OperationalSummary | null;
}

export interface UpdateHcfRequest {
  name?: string;
  contactEmail?: string;
  contactPhone?: string;
  address?: string;
  numberOfBeds?: number;
  doctorName?: string;
  gstNo?: string;
  panNo?: string;
  aadharNo?: string;
  pcbAuthorizationNo?: string;
  monthlyCharges?: number;
  bedded?: boolean;
  otherNotes?: string;
}

export interface UpdateLocationRequest {
  latitude: number;
  longitude: number;
}

export interface DeactivateHcfRequest {
  reason: string;
  terminate?: boolean;
}

export interface BillingConfigRequest {
  baseGramsPerBedPerDay?: number;
  baseRatePerBedPerDay: number;
  excessRatePerKg: number;
}

export interface HcfApprovalRequest {
  perBedPerDayRate: number;
  excessRatePerKg: number;
}

export interface HcfRejectionRequest {
  reason: string;
}

// ============= HCF API Functions =============

export const getHcfList = async (): Promise<HcfListItem[]> => {
  const response = await apiClient.get('/api/cbwtf/hcfs');
  return response.data;
};

export const getHcfDetail = async (id: string): Promise<HcfDetail> => {
  const response = await apiClient.get(`/api/cbwtf/hcfs/${id}`);
  return response.data;
};

export const updateHcf = async (id: string, data: UpdateHcfRequest): Promise<HcfDetail> => {
  const response = await apiClient.put(`/api/cbwtf/hcfs/${id}`, data);
  return response.data;
};

export const updateHcfLocation = async (id: string, data: UpdateLocationRequest): Promise<HcfDetail> => {
  const response = await apiClient.put(`/api/cbwtf/hcfs/${id}/location`, data);
  return response.data;
};

export const deactivateHcf = async (id: string, data: DeactivateHcfRequest): Promise<void> => {
  await apiClient.post(`/api/cbwtf/hcfs/${id}/deactivate`, data);
};

export const activateHcf = async (id: string): Promise<void> => {
  await apiClient.post(`/api/cbwtf/hcfs/${id}/activate`);
};

export interface UpdateAgreementRequest {
  startDate: string; // YYYY-MM-DD
  endDate: string; // YYYY-MM-DD
}

export const updateHcfAgreement = async (id: string, data: UpdateAgreementRequest): Promise<HcfDetail> => {
  const response = await apiClient.put(`/api/cbwtf/hcfs/${id}/agreement`, data);
  return response.data;
};

export const getHcfBillingConfig = async (id: string): Promise<BillingConfigInfo> => {
  const response = await apiClient.get(`/api/cbwtf/hcfs/${id}/billing`);
  return response.data;
};

export const updateHcfBillingConfig = async (id: string, data: BillingConfigRequest): Promise<BillingConfigInfo> => {
  const response = await apiClient.put(`/api/cbwtf/hcfs/${id}/billing`, data);
  return response.data;
};

export const getPendingHcfs = async (): Promise<HcfListItem[]> => {
  const response = await apiClient.get('/api/cbwtf/hcfs/pending');
  return response.data;
};

export const approveHcf = async (id: string, data: HcfApprovalRequest): Promise<HcfDetail> => {
  const response = await apiClient.post(`/api/cbwtf/hcfs/${id}/approve`, data);
  return response.data;
};

export const rejectHcf = async (id: string, data: HcfRejectionRequest): Promise<void> => {
  await apiClient.post(`/api/hcfs/${id}/reject`, data);
};

// Request to update HCF billing model (only for PENDING/REJECTED)
export interface HcfBillingModelUpdateRequest {
  billingModel: BillingModel;
  numberOfBeds: number | null;
  monthlyCharges: number | null;
}

// Update HCF billing model before approval
export const updateHcfBillingModel = async (id: string, data: HcfBillingModelUpdateRequest): Promise<HcfListItem> => {
  const response = await apiClient.put(`/api/hcfs/${id}`, data);
  return response.data;
};

// Simple approve HCF (without agreement creation)
export const simpleApproveHcf = async (id: string): Promise<HcfListItem> => {
  const response = await apiClient.post(`/api/hcfs/${id}/simple-approve`);
  return response.data;
};

// Resubmit rejected HCF for approval
export const resubmitHcf = async (id: string): Promise<HcfListItem> => {
  const response = await apiClient.post(`/api/hcfs/${id}/resubmit`);
  return response.data;
};

export interface RenewAgreementRequest {
  startDate: string;
  endDate: string;
  perBedPerDayRate: number;
}

export const renewAgreement = async (id: string, data: RenewAgreementRequest): Promise<HcfDetail> => {
  const response = await apiClient.post(`/api/cbwtf/hcfs/${id}/agreements/renew`, data);
  return response.data;
};

// ============= HCF Admin Registration =============

export interface CbwtfAdminHcfRegistrationRequest {
  name: string;
  address: string;
  pincode: string;
  state: string;
  doctorName: string;
  contactPhone: string;
  contactEmail: string;
  // Identity fields (optional)
  panNo?: string;
  gstNo?: string;
  aadharNo?: string;
  ownershipType: string;
  rentAgreementUrl?: string;
  bedded: boolean;
  numberOfBeds?: number;
  monthlyCharges?: number;
  otherNotes?: string;
  gpsLat: number;
  gpsLon: number;
  agreementStartDate: string;
  agreementEndDate: string;
  perBedPerDayRate: number;
  // New HCF category fields
  hcfType?: 'HOSPITAL' | 'DENTAL' | 'CLINIC' | 'PATHOLOGY_COLLECTION' | 'PATHOLOGY_STORAGE';
  city?: string;
  seatCount?: number;
}

export const registerHcf = async (data: CbwtfAdminHcfRegistrationRequest): Promise<HcfDetail> => {
  const response = await apiClient.post('/api/cbwtf/hcfs', data);
  return response.data;
};

export const uploadRentAgreement = async (file: File): Promise<{ url: string }> => {
  const formData = new FormData();
  formData.append('file', file);
  const response = await apiClient.post('/api/cbwtf/hcfs/upload-rent-agreement', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
  return response.data;
};

// ============= Attendance =============

export interface AttendanceRecord {
  id: string;
  staffName: string;
  staffRole: string | null;
  hcfName: string;
  hcfId: string | null;
  hcfAddress: string;
  eventTs: string;
  gpsLat: number | null;
  gpsLon: number | null;
}

export interface AttendanceListResponse {
  records: AttendanceRecord[];
  totalRecords: number;
  totalPages: number;
  currentPage: number;
}

export const getAttendanceLogs = async (page = 0, size = 50): Promise<AttendanceListResponse> => {
  const response = await apiClient.get('/api/cbwtf/attendance', {
    params: { page, size }
  });
  return response.data;
};

// ============= QR Authorization =============

export interface QrGenerateRequest {
  hcfId: string;
  wasteCategory: 'YELLOW' | 'RED' | 'BLUE' | 'WHITE';
  validFrom: string;
  validTo: string;
}

export interface QrGenerateResponse {
  qrId: string;
  qrPayloadJson: string;
}

export interface QrDetail {
  id: string;
  agreementId: string;
  agreementNumber: string;
  hcfId: string;
  hcfName: string;
  wasteCategory: 'YELLOW' | 'RED' | 'BLUE' | 'WHITE';
  validFrom: string;
  validTo: string;
  status: 'ACTIVE' | 'USED' | 'VERIFIED' | 'EXPIRED' | 'REVOKED' | 'BLOCKED';
  createdAt: string;
  usedAt: string | null;
  verifiedAt: string | null;
  qrPayloadJson: string;
}

export const generateQr = async (data: QrGenerateRequest): Promise<QrGenerateResponse> => {
  const response = await apiClient.post('/api/cbwtf/qr/generate', data);
  return response.data;
};

export const getQrDetail = async (id: string): Promise<QrDetail> => {
  const response = await apiClient.get(`/api/cbwtf/qr/${id}`);
  return response.data;
};

export const listQrs = async (hcfId?: string, status?: string): Promise<QrDetail[]> => {
  const response = await apiClient.get('/api/cbwtf/qr', {
    params: { hcfId, status }
  });
  return response.data;
};

export const revokeQr = async (id: string, reason?: string): Promise<void> => {
  await apiClient.post(`/api/cbwtf/qr/${id}/revoke`, null, {
    params: { reason }
  });
};

// ============= Billing API =============

export interface BillSummary {
  id: string;
  hcfName: string;
  billingMonth: string;
  totalAmount: number;
  status: string;
  invoiceNumber: string | null;
}

export interface BillDetail {
  // Bill Identity
  id: string;
  billingMonth: string;
  status: string;
  
  // HCF & Agreement Info
  hcfName: string;
  agreementCode: string;
  agreementVersion: number;
  
  // Pickup Snapshot
  pickupEventCount: number;
  pickupWeightKg: number;
  pickupEventHash: string;
  
  // Rate Snapshot (Frozen at billing time)
  bedCount: number;
  baseGramsPerBedPerDay: number;
  baseRatePerBedPerDay: number;
  excessRatePerKg: number;
  excessRateEffectiveFrom: string;
  
  // Calculation Breakdown
  baseAllowanceKg: number;
  excessWeightKg: number;
  baseAmount: number;
  excessAmount: number;
  subtotal: number;
  cgst: number;
  sgst: number;
  totalAmount: number;
  
  // Invoice reference
  invoiceNumber: string | null;
}

export interface InvoiceDetail {
  id: string;
  invoiceNumber: string;
  invoiceDate: string;
  financialYear: string;
  totalAmount: number;
  pdfUrl: string | null;
  integrityHash: string;
}

export interface BillsResponse {
  content: BillSummary[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface FacilityBillingConfig {
  excessRatePerKg: number;
  excessRateEffectiveFrom: string;
}

export interface ExcessRateHistory {
  ratePerKg: number;
  effectiveFrom: string;
  changedAt: string;
  changedBy: string | null;
}

export const listBills = async (page = 0, size = 20): Promise<BillsResponse> => {
  const response = await apiClient.get('/api/cbwtf/billing/bills', {
    params: { page, size }
  });
  return response.data;
};

export const getBillsForMonth = async (year: number, month: number): Promise<BillSummary[]> => {
  const response = await apiClient.get(`/api/cbwtf/billing/bills/month/${year}/${month}`);
  return response.data;
};

export const getBillDetail = async (billId: string): Promise<BillDetail> => {
  const response = await apiClient.get(`/api/cbwtf/billing/bills/${billId}`);
  return response.data;
};

export const getInvoice = async (billId: string): Promise<InvoiceDetail> => {
  const response = await apiClient.get(`/api/cbwtf/billing/bills/${billId}/invoice`);
  return response.data;
};

export const downloadInvoicePdf = async (billId: string): Promise<Blob> => {
  const response = await apiClient.get(`/api/cbwtf/billing/bills/${billId}/invoice/pdf`, {
    responseType: 'blob'
  });
  return response.data;
};

/**
 * Download operational bill PDF (not invoice).
 * Use this instead of downloadInvoicePdf for new code.
 */
export const downloadBillPdf = async (billId: string): Promise<Blob> => {
  const response = await apiClient.get(`/api/cbwtf/billing/bills/${billId}/pdf`, {
    responseType: 'blob'
  });
  return response.data;
};

export const triggerBillGeneration = async (billingMonth: string): Promise<{ billsGenerated: number }> => {
  const response = await apiClient.post('/api/cbwtf/billing/generate', { billingMonth });
  return response.data;
};

export const getFacilityBillingConfig = async (): Promise<FacilityBillingConfig> => {
  const response = await apiClient.get('/api/cbwtf/billing/config');
  return response.data;
};

export const updateExcessRate = async (ratePerKg: number, effectiveFrom: string): Promise<void> => {
  await apiClient.post('/api/cbwtf/billing/config/excess-rate', {
    ratePerKg,
    effectiveFrom
  });
};

export const getExcessRateHistory = async (): Promise<ExcessRateHistory[]> => {
  const response = await apiClient.get('/api/cbwtf/billing/config/excess-rate/history');
  return response.data;
};

// ============= Invoice List API =============

export interface InvoiceSummary {
  id: string;
  invoiceNumber: string;
  hcfName: string;
  billingMonth: string;
  invoiceDate: string;
  totalAmount: number;
}

export interface InvoicesResponse {
  content: InvoiceSummary[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface InvoiceFullDetail {
  id: string;
  invoiceNumber: string;
  invoiceDate: string;
  financialYear: string;
  hcfName: string;
  billingMonth: string;
  totalAmount: number;
  pdfUrl: string | null;
  integrityHash: string;
  billId: string;
}

export const listInvoices = async (page = 0, size = 20): Promise<InvoicesResponse> => {
  const response = await apiClient.get('/api/cbwtf/billing/invoices', {
    params: { page, size }
  });
  return response.data;
};

export const getInvoiceDetail = async (invoiceId: string): Promise<InvoiceFullDetail> => {
  const response = await apiClient.get(`/api/cbwtf/billing/invoices/${invoiceId}`);
  return response.data;
};

export const downloadInvoiceById = async (invoiceId: string): Promise<Blob> => {
  const response = await apiClient.get(`/api/cbwtf/billing/invoices/${invoiceId}/pdf`, {
    responseType: 'blob'
  });
  return response.data;
};

// ============= Compliance Reports API =============

export const getComplianceReports = async (type: string, page = 0, size = 20) => {
  const response = await apiClient.get(`/api/cbwtf/compliance/${type}`, {
    params: { page, size }
  });
  return response.data;
};

export const downloadComplianceReportPdf = async (type: string, id: string) => {
  const response = await apiClient.get(`/api/cbwtf/compliance/${type}/${id}/pdf`, {
    responseType: 'blob'
  });
  const url = window.URL.createObjectURL(new Blob([response.data]));
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', `${type}_report_${id}.pdf`);
  document.body.appendChild(link);
  link.click();
  link.remove();
};

export const downloadAnnualReportExcel = async (id: string) => {
  const response = await apiClient.get(`/api/cbwtf/compliance/annual/${id}/excel`, {
    responseType: 'blob'
  });
  const url = window.URL.createObjectURL(new Blob([response.data]));
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', `form_iv_${id}.xlsx`);
  document.body.appendChild(link);
  link.click();
  link.remove();
};

// ============= Alerts API =============

export const getAlerts = async (page = 0, size = 20, category?: string) => {
  const response = await apiClient.get('/api/cbwtf/alerts', {
    params: { page, size, category }
  });
  return response.data;
};

export const getUnreadAlertCount = async () => {
  const response = await apiClient.get('/api/cbwtf/alerts/unread-count');
  return response.data;
};

export const markAlertAsRead = async (id: string) => {
  const response = await apiClient.put(`/api/cbwtf/alerts/${id}/read`);
  return response.data;
};

export const markAllAlertsAsRead = async () => {
  const response = await apiClient.put('/api/cbwtf/alerts/read-all');
  return response.data;
};

// ============= Notification Settings API =============

export const getNotificationSettings = async () => {
  const response = await apiClient.get('/api/cbwtf/settings/notifications');
  return response.data;
};

export const updateNotificationSettings = async (settings: {
  paymentReminderStartDays?: number;
  paymentReminderFrequencyDays?: number;
  maxOverdueReminders?: number;
  agreementExpiryWarningDays?: number;
}) => {
  const response = await apiClient.put('/api/cbwtf/settings/notifications', settings);
  return response.data;
};

// ============= Email Templates API =============

export interface EmailTemplateDTO {
  id?: string;
  templateCode: string;
  subjectTemplate: string;
  bodyTemplate: string;
  version?: number;
  isActive?: boolean;
  requiredPlaceholders?: string[];
  availablePlaceholders?: string[];
}

export const getEmailTemplates = async (): Promise<EmailTemplateDTO[]> => {
  const response = await apiClient.get('/api/cbwtf/email-templates');
  return response.data;
};

export const getEmailTemplate = async (templateCode: string): Promise<EmailTemplateDTO> => {
  const response = await apiClient.get(`/api/cbwtf/email-templates/${templateCode}`);
  return response.data;
};

export const updateEmailTemplate = async (templateCode: string, data: EmailTemplateDTO): Promise<void> => {
  await apiClient.put(`/api/cbwtf/email-templates/${templateCode}`, data);
};

export const previewEmailTemplate = async (
  templateCode: string, 
  bodyTemplate: string, 
  sampleData: Record<string, string>
): Promise<{ html: string }> => {
  const response = await apiClient.post(`/api/cbwtf/email-templates/${templateCode}/preview`, {
    bodyTemplate,
    sampleData
  });
  return response.data;
};

export const resetEmailTemplate = async (templateCode: string): Promise<void> => {
  await apiClient.post(`/api/cbwtf/email-templates/${templateCode}/reset`);
};

export const getEmailTemplatePlaceholders = async (templateCode: string): Promise<{
  required: string[];
  available: string[];
}> => {
  const response = await apiClient.get(`/api/cbwtf/email-templates/${templateCode}/placeholders`);
  return response.data;
};

// ============= Branding API =============

export interface BrandingDTO {
  logoUrl?: string;
  logoChecksum?: string;
  primaryColor?: string;
  secondaryColor?: string;
  invoiceFooterText?: string;
  receiptFooterText?: string;
  showLogoOnInvoice?: boolean;
  showLogoOnReceipt?: boolean;
  showLogoOnEmail?: boolean;
}

export const getBranding = async (): Promise<BrandingDTO> => {
  const response = await apiClient.get('/api/cbwtf/branding');
  return response.data;
};

export const updateBranding = async (data: BrandingDTO): Promise<void> => {
  await apiClient.put('/api/cbwtf/branding', data);
};

export const uploadLogo = async (file: File): Promise<{ logoUrl: string }> => {
  const formData = new FormData();
  formData.append('file', file);
  const response = await apiClient.post('/api/cbwtf/branding/logo', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
  return response.data;
};

export const deleteLogo = async (): Promise<void> => {
  await apiClient.delete('/api/cbwtf/branding/logo');
};

// ============= Consumables API =============

export interface ConsumableCategoryDTO {
  id: string;
  name: string;
  displayOrder: number;
  isActive: boolean;
  createdAt: string;
  itemCount: number;
}

export interface ConsumablePricingHistoryItem {
  id: string;
  pricePerUnit: number;
  gstRate: number;
  effectiveFrom: string;
  effectiveTo: string | null;
  isActive: boolean;
  createdAt: string;
}

export interface ConsumableItemDTO {
  id: string;
  consumableCode: string;
  name: string;
  description: string | null;
  hsnCode: string | null;
  unitOfMeasure: string;
  imageUrl: string | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  categoryId: string;
  categoryName: string;
  activePrice: number | null;
  activeGstRate: number | null;
  priceEffectiveFrom: string | null;
  referenceType: string | null;
  referenceQuantity: number | null;
  referenceDisplayText: string | null;
  pricingHistory: ConsumablePricingHistoryItem[] | null;
}

export interface CreateConsumableRequest {
  categoryId: string;
  consumableCode: string;
  name: string;
  description?: string;
  hsnCode?: string;
  unitOfMeasure: string;
  initialPrice?: number;
  gstRate?: number;
  priceEffectiveFrom?: string;
  referenceType?: 'PER_100_BEDS_PER_YEAR' | 'PER_MONTH' | 'FIXED';
  referenceQuantity?: number;
}

export interface UpdateConsumableRequest {
  categoryId?: string;
  name?: string;
  description?: string;
  hsnCode?: string;
  unitOfMeasure?: string;
}

export interface AddPricingRequest {
  pricePerUnit: number;
  gstRate?: number;
  effectiveFrom: string;
}

export const listConsumables = async (includeInactive = false): Promise<ConsumableItemDTO[]> => {
  const response = await apiClient.get('/api/cbwtf/consumables', {
    params: { includeInactive }
  });
  return response.data;
};

export const getConsumable = async (id: string): Promise<ConsumableItemDTO> => {
  const response = await apiClient.get(`/api/cbwtf/consumables/${id}`);
  return response.data;
};

export const createConsumable = async (data: CreateConsumableRequest): Promise<ConsumableItemDTO> => {
  const response = await apiClient.post('/api/cbwtf/consumables', data);
  return response.data;
};

export const updateConsumable = async (id: string, data: UpdateConsumableRequest): Promise<ConsumableItemDTO> => {
  const response = await apiClient.put(`/api/cbwtf/consumables/${id}`, data);
  return response.data;
};

export const addConsumablePricing = async (id: string, data: AddPricingRequest): Promise<ConsumableItemDTO> => {
  const response = await apiClient.post(`/api/cbwtf/consumables/${id}/pricing`, data);
  return response.data;
};

export const getConsumablePricingHistory = async (id: string): Promise<ConsumablePricingHistoryItem[]> => {
  const response = await apiClient.get(`/api/cbwtf/consumables/${id}/pricing`);
  return response.data;
};

export const uploadConsumableImage = async (id: string, file: File): Promise<ConsumableItemDTO> => {
  const formData = new FormData();
  formData.append('file', file);
  const response = await apiClient.post(`/api/cbwtf/consumables/${id}/image`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
  return response.data;
};

export const deleteConsumableImage = async (id: string): Promise<ConsumableItemDTO> => {
  const response = await apiClient.delete(`/api/cbwtf/consumables/${id}/image`);
  return response.data;
};

export const deactivateConsumable = async (id: string): Promise<void> => {
  await apiClient.patch(`/api/cbwtf/consumables/${id}/deactivate`);
};

export const activateConsumable = async (id: string): Promise<void> => {
  await apiClient.patch(`/api/cbwtf/consumables/${id}/activate`);
};

export const listConsumableCategories = async (): Promise<ConsumableCategoryDTO[]> => {
  const response = await apiClient.get('/api/cbwtf/consumables/categories');
  return response.data;
};

export default cbwtfApi;

// ============= Route Planning Types =============

export type RouteStatus = 'DRAFT' | 'ACTIVE' | 'TEMPORARILY_SUSPENDED';

export interface RouteDTO {
  id: string;
  name: string;
  description: string | null;
  color: string;
  status: RouteStatus;
  isActive: boolean;
  waypointCount: number;
  assignedStaffName: string | null;
  assignedStaffId: string | null;
  completionDays: number | null;
  cycleStartDate: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface RouteWaypointDTO {
  id: string;
  sequenceOrder: number;
  estimatedStopMinutes: number;
  hcfId: string;
  hcfCode: string;
  hcfName: string;
  hcfAddress: string;
  gpsLat: number | null;
  gpsLon: number | null;
}

export interface RouteAssignmentDTO {
  id: string;
  staffId: string | null;
  staffName: string | null;
  staffPhone: string | null;
  vehicleId: string | null;
  vehicleRegistration: string | null;
  assignedFrom: string;
  assignedTo: string | null;
  isActive: boolean;
  createdAt: string;
}

export interface RouteDetailDTO {
  id: string;
  name: string;
  description: string | null;
  color: string;
  status: RouteStatus;
  isActive: boolean;
  waypoints: RouteWaypointDTO[];
  currentAssignment: RouteAssignmentDTO | null;
  assignmentHistory: RouteAssignmentDTO[];
  createdAt: string;
  updatedAt: string;
}

export interface HcfGeoPointDTO {
  id: string;
  code: string;
  name: string;
  address: string;
  gpsLat: number | null;
  gpsLon: number | null;
  status: string;
  routeIds: string[];
}

export interface WaypointCoordinate {
  order: number;
  hcfId: string;
  hcfName: string;
  lat: number | null;
  lon: number | null;
}

export interface RouteWithWaypointsDTO {
  id: string;
  name: string;
  color: string;
  status: RouteStatus;
  isActive: boolean;
  assignedStaffName: string | null;
  coordinates: WaypointCoordinate[];
}

export interface RouteMapDataDTO {
  hcfs: HcfGeoPointDTO[];
  routes: RouteWithWaypointsDTO[];
}

export interface CreateRouteRequest {
  name: string;
  description?: string;
  color?: string;
  completionDays?: number;
}

export interface UpdateRouteRequest {
  name?: string;
  description?: string;
  color?: string;
  status?: RouteStatus;
  completionDays?: number;
}

export interface SetWaypointsRequest {
  hcfIds: string[];
}

export interface AssignRouteRequest {
  staffId: string;
  vehicleId?: string;
}

// ============= Route Planning API Functions =============

export const createRoute = async (request: CreateRouteRequest): Promise<RouteDTO> => {
  const response = await apiClient.post('/api/cbwtf/routes', request);
  return response.data;
};

export const listRoutes = async (): Promise<RouteDTO[]> => {
  const response = await apiClient.get('/api/cbwtf/routes');
  return response.data;
};

export const getRoute = async (id: string): Promise<RouteDetailDTO> => {
  const response = await apiClient.get(`/api/cbwtf/routes/${id}`);
  return response.data;
};

export const updateRoute = async (id: string, request: UpdateRouteRequest): Promise<RouteDTO> => {
  const response = await apiClient.put(`/api/cbwtf/routes/${id}`, request);
  return response.data;
};

export const setRouteStatus = async (id: string, status: RouteStatus): Promise<RouteDTO> => {
  const response = await apiClient.patch(`/api/cbwtf/routes/${id}/status`, { status });
  return response.data;
};

export const setRouteWaypoints = async (id: string, request: SetWaypointsRequest): Promise<RouteWaypointDTO[]> => {
  const response = await apiClient.put(`/api/cbwtf/routes/${id}/waypoints`, request);
  return response.data;
};

export const assignRoute = async (id: string, request: AssignRouteRequest): Promise<RouteAssignmentDTO> => {
  const response = await apiClient.post(`/api/cbwtf/routes/${id}/assign`, request);
  return response.data;
};

export const unassignRoute = async (id: string): Promise<void> => {
  await apiClient.delete(`/api/cbwtf/routes/${id}/assign`);
};

export const getRouteMapData = async (routeId?: string, activeOnly?: boolean): Promise<RouteMapDataDTO> => {
  const params = new URLSearchParams();
  if (routeId) params.append('routeId', routeId);
  if (activeOnly !== undefined) params.append('activeOnly', String(activeOnly));
  
  const url = params.toString() ? `/api/cbwtf/routes/map-data?${params}` : '/api/cbwtf/routes/map-data';
  const response = await apiClient.get(url);
  return response.data;
};

// ============= Staff Selection API =============

export interface StaffOption {
  id: string;
  name: string;
}

export const getStaffForSelection = async (): Promise<StaffOption[]> => {
  // Fetch staff/drivers for route assignment (paginated response)
  const response = await apiClient.get('/api/cbwtf/staff?size=100');
  // Handle both Page<Staff> (with content) and List<Staff> responses
  const staffList = Array.isArray(response.data) ? response.data : (response.data.content || []);
  return staffList.map((s: { id: string; fullName?: string; name?: string; username?: string }) => ({
    id: s.id,
    name: s.fullName || s.name || s.username || 'Unknown Staff',
  }));
};

// ============= Route Execution Types =============

export interface RouteExecutionLogDTO {
  logId: string;
  waypointId: string;
  hcfId: string;
  hcfName: string;
  hcfCode: string;
  sequenceOrder: number;
  status: 'PENDING' | 'COMPLETED' | 'MISSED';
  visitedAt: string | null;
  staffName: string | null;
}

export interface RouteCycleDTO {
  cycleId: string;
  cycleNumber: number;
  cycleStart: string;
  cycleEnd: string;
  totalWaypoints: number;
  completedWaypoints: number;
  missedWaypoints: number;
  completionPercentage: number;
  status: 'IN_PROGRESS' | 'COMPLETED' | 'INCOMPLETE';
  staffName: string | null;
  completedAt: string | null;
}

export interface RouteExecutionDTO {
  routeId: string;
  routeName: string;
  completionDays: number | null;
  activeCycle: RouteCycleDTO | null;
  executionLogs: RouteExecutionLogDTO[];
}

export interface RouteCycleHistoryDTO {
  cycleId: string;
  routeId: string;
  routeName: string;
  cycleNumber: number;
  cycleStart: string;
  cycleEnd: string;
  totalWaypoints: number;
  completedWaypoints: number;
  missedWaypoints: number;
  completionPercentage: number;
  status: 'IN_PROGRESS' | 'COMPLETED' | 'INCOMPLETE';
  staffId: string | null;
  staffName: string | null;
  completedAt: string | null;
  createdAt: string;
}

export interface RouteAlertDTO {
  alertId: string;
  routeId: string;
  routeName: string;
  routeColor: string;
  cycleId: string;
  cycleNumber: number;
  cycleStart: string;
  cycleEnd: string;
  alertType: 'ROUTE_INCOMPLETE' | 'WAYPOINT_MISSED' | 'ROUTE_NOT_STARTED';
  severity: 'INFO' | 'WARNING' | 'CRITICAL';
  title: string;
  message: string | null;
  missedHcfCount: number;
  staffId: string | null;
  staffName: string | null;
  isResolved: boolean;
  resolvedByName: string | null;
  resolvedAt: string | null;
  resolutionNotes: string | null;
  createdAt: string;
}

// ============= Route Execution API Functions =============

export const getRouteExecution = async (routeId: string): Promise<RouteExecutionDTO> => {
  const response = await apiClient.get(`/api/cbwtf/routes/${routeId}/execution`);
  return response.data;
};

export const getRouteCycleHistory = async (routeId: string, page = 0, size = 10): Promise<RouteCycleHistoryDTO[]> => {
  const response = await apiClient.get(`/api/cbwtf/routes/${routeId}/history?page=${page}&size=${size}`);
  return response.data;
};

export const getRouteAlerts = async (): Promise<RouteAlertDTO[]> => {
  const response = await apiClient.get('/api/cbwtf/routes/alerts');
  return response.data;
};

export const getRouteAlertCount = async (): Promise<{ count: number }> => {
  const response = await apiClient.get('/api/cbwtf/routes/alerts/count');
  return response.data;
};

export const resolveRouteAlert = async (alertId: string, notes?: string): Promise<RouteAlertDTO> => {
  const response = await apiClient.post(`/api/cbwtf/routes/alerts/${alertId}/resolve`, { notes });
  return response.data;
};
