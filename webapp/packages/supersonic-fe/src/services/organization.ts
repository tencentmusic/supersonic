import request from '@/services/request';

const API_BASE = process.env.AUTH_API_BASE_URL || '/api/auth/';

export interface Organization {
  id: string;
  parentId: string;
  name: string;
  fullName: string;
  subOrganizations?: Organization[];
  isRoot?: boolean;
}

export interface OrganizationReq {
  parentId?: number;
  name: string;
  sortOrder?: number;
  status?: number;
}

export interface UserOrganizationReq {
  userId?: number;
  userIds?: number[];
  organizationId: number;
  isPrimary?: boolean;
}

// Get organization tree
export async function getOrganizationTree() {
  return request.get<any>(`${API_BASE}organization/tree`);
}

// Get organization by id
export async function getOrganization(id: number) {
  return request.get<any>(`${API_BASE}organization/${id}`);
}

// Create organization
export async function createOrganization(data: OrganizationReq) {
  return request.post<any>(`${API_BASE}organization`, {
    data,
  });
}

// Update organization
export async function updateOrganization(id: number, data: OrganizationReq) {
  return request.put<any>(`${API_BASE}organization/${id}`, {
    data,
  });
}

// Delete organization
export async function deleteOrganization(id: number) {
  return request.delete<any>(`${API_BASE}organization/${id}`);
}

// Get users by organization
export async function getUsersByOrganization(id: number) {
  return request.get<any>(`${API_BASE}organization/${id}/users`);
}

// Get user's organizations
export async function getUserOrganizations(userId: number) {
  return request.get<any>(`${API_BASE}organization/user/${userId}`);
}

// Assign user to organization
export async function assignUserToOrganization(data: UserOrganizationReq) {
  return request.post<any>(`${API_BASE}organization/assign`, {
    data,
  });
}

// Remove user from organization
export async function removeUserFromOrganization(data: UserOrganizationReq) {
  return request.post<any>(`${API_BASE}organization/remove`, {
    data,
  });
}

// Set user's primary organization
export async function setUserPrimaryOrganization(data: UserOrganizationReq) {
  return request.post<any>(`${API_BASE}organization/setPrimary`, {
    data,
  });
}

// Batch assign users to organization
export async function batchAssignUsersToOrganization(data: UserOrganizationReq) {
  return request.post<any>(`${API_BASE}organization/batchAssign`, {
    data,
  });
}

// Batch remove users from organization
export async function batchRemoveUsersFromOrganization(data: UserOrganizationReq) {
  return request.post<any>(`${API_BASE}organization/batchRemove`, {
    data,
  });
}
