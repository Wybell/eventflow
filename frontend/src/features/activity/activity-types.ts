export type ActivityStatus = 'DRAFT' | 'PENDING_REVIEW' | 'REJECTED' | 'PUBLISHED' | 'OFFLINE';

export interface Activity {
  id: number;
  createUserId: number;
  title: string;
  summary: string | null;
  coverUrl: string | null;
  venueName: string | null;
  organizerName: string;
  contactName: string;
  contactMobile: string | null;
  contactEmail: string | null;
  status: ActivityStatus;
  reviewNote: string | null;
  reviewTime: string | null;
  publishedTime: string | null;
  registrationStartTime: string;
  registrationEndTime: string;
}

export interface ActivityInput {
  title: string;
  summary?: string;
  coverUrl?: string;
  venueName?: string;
  organizerName: string;
  contactName: string;
  contactMobile?: string;
  contactEmail?: string;
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
