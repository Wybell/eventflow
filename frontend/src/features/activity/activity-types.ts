export type ActivityStatus = 'DRAFT' | 'PUBLISHED' | 'OFFLINE';

export interface Activity {
  id: number;
  organizationId: number;
  title: string;
  summary: string | null;
  coverUrl: string | null;
  venueName: string | null;
  status: ActivityStatus;
  registrationStartTime: string;
  registrationEndTime: string;
}

export interface ActivityInput {
  title: string;
  summary?: string;
  coverUrl?: string;
  venueName?: string;
  registrationStartTime: string;
  registrationEndTime: string;
}

export interface ActivitySession {
  id: number;
  activityId: number;
  title: string;
  startTime: string;
  endTime: string;
  totalQuota: number;
  availableQuota: number;
  reservedQuota: number;
  confirmedQuota: number;
  status: 'ACTIVE' | 'CLOSED';
}

export interface ActivitySessionInput {
  title: string;
  startTime: string;
  endTime: string;
  totalQuota: number;
}
