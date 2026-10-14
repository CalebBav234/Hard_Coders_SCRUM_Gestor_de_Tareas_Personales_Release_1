/** Contrato HTTP de /api/projects (Épica 1). Montos en BOB, duraciones en minutos. */

export type ProjectStatusName = 'PLANIFICADO' | 'EN_EJECUCION' | 'EN_PAUSA' | 'FINALIZADO' | 'CANCELADO';

export interface UserSummary {
  userId: number;
  fullName: string;
  email: string | null;
  chief: boolean;
  active: boolean;
}

export interface Project {
  projectId: number;
  name: string;
  description: string | null;
  status: ProjectStatusName;
  closed: boolean;
  budgetBob: number;
  allocatedBudgetBob: number;
  estimatedDurationMinutes: number;
  manager: UserSummary;
  createdBy: UserSummary;
  currentRevision: number;
  createdAt: string;
  updatedAt: string;
  version: number;
  /** Permisos del actor calculados por el backend (solo para mostrar u ocultar acciones). */
  canEditDetails: boolean;
  canEditPlan: boolean;
  canDelegate: boolean;
  /** US2.2/US2.3: el actor puede construir la WBS y asignar Personal en este proyecto. */
  canManageHierarchy: boolean;
  allowedStatuses: ProjectStatusName[];
}

export interface PlanRevision {
  revisionNumber: number;
  budgetBob: number;
  estimatedDurationMinutes: number;
  changedAt: string;
  changedBy: UserSummary;
  reason: string;
}

export interface CreateProjectRequest {
  name: string;
  description: string | null;
  managerUserId: number;
  budgetBob: number;
  estimatedDurationMinutes: number;
  reason: string | null;
}

export interface UpdateProjectDetailsRequest {
  name: string;
  description: string | null;
  version: number;
}

export interface ReplanProjectRequest {
  budgetBob: number;
  estimatedDurationMinutes: number;
  reason: string;
  version: number;
}

export interface DelegateProjectRequest {
  managerUserId: number;
  reason: string;
  version: number;
}

export const PROJECT_STATUS_LABELS: Record<ProjectStatusName, string> = {
  PLANIFICADO: 'Planificado',
  EN_EJECUCION: 'En ejecución',
  EN_PAUSA: 'En pausa',
  FINALIZADO: 'Finalizado',
  CANCELADO: 'Cancelado',
};

export function formatBob(amount: number | null | undefined): string {
  const value = Number(amount ?? 0);
  return `Bs ${new Intl.NumberFormat('es-BO', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value)}`;
}

export function formatMinutes(minutes: number | null | undefined): string {
  const total = Math.max(0, Math.floor(Number(minutes ?? 0)));
  const hours = Math.floor(total / 60);
  const rest = total % 60;
  if (hours === 0) {
    return `${rest} min`;
  }
  return rest === 0 ? `${hours} h` : `${hours} h ${rest} min`;
}
