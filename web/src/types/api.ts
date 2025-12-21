// API Response Types
export interface ApiError {
  message: string;
  code?: string;
  details?: Record<string, string>;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

// Auth Types
export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
}

export interface JwtPayload {
  sub: string;
  role: UserRole;
  full_name: string | null;
  profile_photo_url: string | null;
  tenant_id: string | null;
  hcf_id: string | null;
  must_change_password?: boolean;
  iat: number;
  exp: number;
}

export type UserRole = 
  | 'SUPER_ADMIN'
  | 'CBWTF_ADMIN'
  | 'HCF_ADMIN'
  | 'DRIVER'
  | 'PLANT_OPERATOR';

// User Types
export interface User {
  id: string;
  username: string;
  fullName: string;
  email: string;
  phone: string;
  role: UserRole;
  facilityId: string | null;
  hcfId: string | null;
  createdAt: string;
}

// Facility (Tenant/CBWTF) Types
export interface Facility {
  id: string;
  code: string;
  name: string;
  address: string;
  contactEmail: string;
  contactPhone: string;
  gpsLat: number;
  gpsLon: number;
  geofenceRadiusM: number;
  subscriptionPlan: 'BASIC' | 'PRO' | 'ENTERPRISE';
  subscriptionStatus: 'ACTIVE' | 'EXPIRED' | 'SUSPENDED';
  subscriptionExpiresAt: string | null;
  createdAt: string;
}

// HCF Types
export type HcfStatus = 'PENDING_APPROVAL' | 'ACTIVE' | 'SUSPENDED' | 'TERMINATED';

export interface Hcf {
  id: string;
  code: string;
  name: string;
  address: string;
  contactEmail: string;
  contactPhone: string;
  numberOfBeds: number;
  gpsLat: number;
  gpsLon: number;
  status: HcfStatus;
  createdAt: string;
  updatedAt: string;
}

// Waste Category
export type WasteCategory = 'YELLOW' | 'RED' | 'BLUE' | 'WHITE';

// Analytics Types
export interface DashboardMetrics {
  totalBags: number;
  totalWeightKg: number;
  verifiedBags: number;
  mismatchCount: number;
  missingBags: number;
  blueWastePercentage: number;
  categoryBreakdown: CategoryBreakdown[];
  revenueInvoiced: number;
  revenueCollected: number;
  revenueOutstanding: number;
}

export interface CategoryBreakdown {
  category: WasteCategory;
  bags: number;
  weightKg: number;
  percentage: number;
}

export interface TrendDataPoint {
  date: string;
  value: number;
  category?: WasteCategory;
}

// Alert Types
export type AlertSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type AlertType = 'MISSING_BAG' | 'WEIGHT_MISMATCH' | 'ROUTE_DEVIATION' | 'VERIFICATION_DELAY';

export interface Alert {
  id: string;
  type: AlertType;
  severity: AlertSeverity;
  message: string;
  entityId: string;
  entityType: string;
  acknowledged: boolean;
  acknowledgedAt: string | null;
  createdAt: string;
}

// Invoice Types
export type InvoiceStatus = 'DRAFT' | 'ISSUED' | 'PAID' | 'OVERDUE' | 'CANCELLED';

export interface Invoice {
  id: string;
  invoiceNumber: string;
  hcfId: string;
  hcfName: string;
  periodStart: string;
  periodEnd: string;
  baseAmount: number;
  taxAmount: number;
  totalAmount: number;
  status: InvoiceStatus;
  pdfUrl: string | null;
  createdAt: string;
}

// Bag Label Types
export type LabelStatus = 'ISSUED' | 'USED' | 'EXPIRED' | 'VOIDED';

export interface BagLabel {
  id: string;
  hcfId: string;
  category: WasteCategory;
  serialNo: string;
  qrCode: string;
  status: LabelStatus;
  issuedAt: string;
  usedAt: string | null;
}
