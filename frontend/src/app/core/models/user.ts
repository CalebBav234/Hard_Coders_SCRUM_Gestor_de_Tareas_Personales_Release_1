export interface CreateUserRequest {
  email: string;
  givenNames: string;
  familyNames: string;
  password: string;
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
