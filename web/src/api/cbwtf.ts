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

export interface HcfListItem {
  id: string;
  code: string;
  name: string;
  address: string;
  contactPhone: string | null;
  contactEmail: string | null;
  numberOfBeds: number | null;
  agreementId: string | null;
  agreementNumber: string | null;
  agreementStatus: string | null;
  duesStatus: string | null;
  agreementStartDate: string | null;
  agreementEndDate: string | null;
  lastPickupAt: string | null;
  createdAt: string;
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
  await apiClient.post(`/api/cbwtf/hcfs/${id}/reject`, data);
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

export default cbwtfApi;
