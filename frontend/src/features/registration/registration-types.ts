export type RegistrationStatus = 'CONFIRMED' | 'CANCELLED';

export interface ActivityRegistration {
  id: number;
  activityId: number;
  sessionId: number;
  status: RegistrationStatus;
  activityTitle: string;
  venueName: string | null;
  sessionTitle: string;
  sessionStartTime: string;
  sessionEndTime: string;
  createTime: string;
  updateTime: string;
}

export interface RegistrationInput {
  activityId: number;
  sessionId: number;
}

export interface OrganizerRegistration {
  id: number;
  activityId: number;
  sessionId: number;
  status: RegistrationStatus;
  displayName: string;
  username: string;
  mobile: string | null;
  email: string | null;
  sessionTitle: string;
  registrationTime: string;
  updateTime: string;
}

export interface OrganizerRegistrationPage {
  total: number;
  confirmedCount: number;
  cancelledCount: number;
  page: number;
  size: number;
  items: OrganizerRegistration[];
}

export interface OrganizerRegistrationQuery {
  sessionId?: number;
  status?: RegistrationStatus | 'ALL';
  keyword?: string;
  page?: number;
  size?: number;
}
