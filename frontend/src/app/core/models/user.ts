export interface CreateUserRequest {
  email: string;
  givenNames: string;
  familyNames: string;
  password: string;
}

/** Campos de perfil que el Jefe puede editar de un usuario (nada de credenciales ni privilegios). */
export interface UpdateUserRequest {
  email?: string;
  givenNames?: string;
  familyNames?: string;
}

export interface UserResponse {
  userId: number;
  email: string;
  givenNames: string;
  familyNames: string;
  fullName: string;
  chief: boolean;
  active: boolean;
}
