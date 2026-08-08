export interface LoginInput {
  username: string;
  password: string;
}

export interface AuthTokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface CurrentUser {
  id: number;
  username: string;
  displayName: string;
  avatarUrl: string | null;
  mobile: string | null;
  email: string | null;
  organizationId: number | null;
  roles: string[];
}

export interface RegisterInput {
  username: string;
  password: string;
  displayName: string;
  mobile?: string;
  email?: string;
}

export interface RegistrationResponse {
  userId: number;
}

export interface UpdateProfileInput {
  displayName: string;
  mobile?: string;
  email?: string;
}

export interface ChangePasswordInput {
  currentPassword: string;
  newPassword: string;
}
