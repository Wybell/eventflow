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
