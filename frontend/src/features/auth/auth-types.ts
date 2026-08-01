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
  organizationId: number | null;
  roles: string[];
}

export type RegistrationType = 'PARTICIPANT' | 'ORGANIZER';

export interface RegisterInput {
  registrationType: RegistrationType;
  username: string;
  password: string;
  displayName: string;
  mobile?: string;
  email?: string;
  organizationName?: string;
  organizationDescription?: string;
  contactName?: string;
}

export interface RegistrationResponse {
  userId: number;
  registrationType: RegistrationType;
  organizerApplicationStatus: 'PENDING' | null;
}
