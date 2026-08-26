export interface Task {
  id: number;
  title: string;
  status: 'INACTIVA' | 'ACTIVA' | 'TERMINADA';
  priority: 'ALTA' | 'MEDIA' | 'BAJA';
}