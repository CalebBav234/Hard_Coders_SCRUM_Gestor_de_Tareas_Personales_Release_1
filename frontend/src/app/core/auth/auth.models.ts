/** Usuario autenticado tal como lo devuelve /api/auth (el rol real se valida siempre en el backend). */
export interface CurrentUser {
  userId: number;
  email: string;
  givenNames: string;
  familyNames: string;
  fullName: string;
  chief: boolean;
  active: boolean;
}

export interface LoginResponse {
  token: string;
  expiresAt: string;
  user: CurrentUser;
}

export interface StoredSession {
  token: string;
  expiresAt: string;
  user: CurrentUser;
}
