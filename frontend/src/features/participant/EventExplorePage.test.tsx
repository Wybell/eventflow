import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { Activity, ActivitySession } from '../activity/activity-types';
import { getPublishedActivities, getPublishedActivitySessions } from '../activity/activity-api';
import { getMyRegistrations } from '../registration/registration-api';
import type { CurrentUser } from '../auth/auth-types';
import { useSessionStore } from '../../shared/auth/session-store';
import { EventExplorePage } from './EventExplorePage';

vi.mock('../activity/activity-api', () => ({
  getPublishedActivities: vi.fn(),
  getPublishedActivitySessions: vi.fn(),
}));
vi.mock('../registration/registration-api', () => ({
  cancelRegistration: vi.fn(),
  getMyRegistrations: vi.fn(),
  registerForActivity: vi.fn(),
}));
vi.mock('../profile/ProfileMenu', () => ({ ProfileMenu: () => null }));

const USER: CurrentUser = {
  id: 7,
  username: 'participant',
  displayName: '参与者',
  avatarUrl: null,
  mobile: null,
  email: null,
  organizationId: null,
  roles: ['USER'],
};

describe('EventExplorePage registration actions', () => {
  beforeEach(() => {
    useSessionStore.setState({ accessToken: 'token', currentUser: USER });
    vi.mocked(getPublishedActivities).mockResolvedValue([activity()]);
    vi.mocked(getPublishedActivitySessions).mockResolvedValue([session()]);
    vi.mocked(getMyRegistrations).mockResolvedValue([]);
  });

  it('shows a registration button for an eligible session', async () => {
    renderPage([activity()]);

    fireEvent.click(await screen.findByRole('button', { name: '查看场次' }));

    expect(await screen.findByRole('button', { name: '报名此场次' })).toBeEnabled();
  });

  it('explains when registration has not opened yet', async () => {
    vi.mocked(getPublishedActivities).mockResolvedValue([
      activity({ registrationStartTime: dateFromNow(60 * 60 * 1000) }),
    ]);
    renderPage([activity({ registrationStartTime: dateFromNow(60 * 60 * 1000) })]);

    fireEvent.click(await screen.findByRole('button', { name: '查看场次' }));

    expect(await screen.findByRole('button', { name: '报名未开始' })).toBeDisabled();
  });

  it('opens the current users registrations', async () => {
    renderPage([activity()]);

    fireEvent.click(await screen.findByRole('button', { name: /我的报名/ }));

    expect(await screen.findByText('还没有报名记录')).toBeInTheDocument();
  });
});

function renderPage(activities: Activity[]) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false, staleTime: Number.POSITIVE_INFINITY } },
  });
  queryClient.setQueryData(['activities', 'public'], activities);
  queryClient.setQueryData(['activity-sessions', 'public', 11], [session()]);
  queryClient.setQueryData(['registrations', 'mine', USER.id], []);
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <EventExplorePage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

function activity(overrides: Partial<Activity> = {}): Activity {
  return {
    id: 11,
    createUserId: 3,
    title: '开发者峰会',
    summary: '面向开发者的公开活动',
    coverUrl: null,
    venueName: '会议中心',
    organizerName: 'EventFlow',
    contactName: 'Lin',
    contactMobile: null,
    contactEmail: null,
    status: 'PUBLISHED',
    reviewNote: null,
    reviewTime: null,
    publishedTime: dateFromNow(-24 * 60 * 60 * 1000),
    registrationStartTime: dateFromNow(-60 * 60 * 1000),
    registrationEndTime: dateFromNow(60 * 60 * 1000),
    ...overrides,
  };
}

function session(): ActivitySession {
  return {
    id: 21,
    activityId: 11,
    title: '峰会 1',
    startTime: dateFromNow(24 * 60 * 60 * 1000),
    endTime: dateFromNow(26 * 60 * 60 * 1000),
    totalQuota: 80,
    availableQuota: 80,
    reservedQuota: 0,
    confirmedQuota: 0,
    status: 'ACTIVE',
  };
}

function dateFromNow(offsetMilliseconds: number): string {
  return new Date(Date.now() + offsetMilliseconds).toISOString();
}
