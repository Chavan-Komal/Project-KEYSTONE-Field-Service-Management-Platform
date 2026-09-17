import { api } from './client';
import type { Technician } from '../types';

export interface CreateTechnicianPayload {
  name: string;
  email: string;
  password: string;
  baseAddress?: string;
}

// POST /api/users/technicians — manager-only provisioning of a new
// technician account (staff accounts are never self-registered).
export async function createTechnician(payload: CreateTechnicianPayload): Promise<Technician> {
  const { data } = await api.post<Technician>('/users/technicians', payload);
  return data;
}

// POST /api/users/technicians/{id}/base — set or change a technician's
// home-base address (geocoded server-side). POST, not PATCH — see the
// backend controller comment for why.
export async function updateTechnicianBase(id: string, baseAddress: string): Promise<Technician> {
  const { data } = await api.post<Technician>(`/users/technicians/${id}/base`, { baseAddress });
  return data;
}
