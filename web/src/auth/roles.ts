import type { UserRole } from '../types/api';

export const SUPER_ADMIN_ONLY: UserRole[] = ['SUPER_ADMIN'];
export const CBWTF_ADMIN_ONLY: UserRole[] = ['CBWTF_ADMIN'];
export const HCF_ADMIN_ONLY: UserRole[] = ['HCF_ADMIN'];
export const TOP_MANAGEMENT_ADMIN_ONLY: UserRole[] = ['TOP_MANAGEMENT'];
export const ADMIN_ROLES: UserRole[] = ['SUPER_ADMIN', 'CBWTF_ADMIN'];
export const ALL_ADMIN_ROLES: UserRole[] = ['SUPER_ADMIN', 'CBWTF_ADMIN', 'HCF_ADMIN', 'TOP_MANAGEMENT'];
