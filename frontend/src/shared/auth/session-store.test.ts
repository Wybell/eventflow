import { beforeEach, describe, expect, it } from 'vitest';
import type { CurrentUser } from '../../features/auth/auth-types';
import { queryClient } from '../lib/query-client';
import { useSessionStore } from './session-store';

const USER: CurrentUser = {
  id: 2,
  username: 'wybell',
  displayName: 'Wybell',
  organizationId: null,
  roles: ['USER'],
};

describe('session store', () => {
  beforeEach(() => {
    queryClient.clear();
    useSessionStore.setState({ accessToken: null, currentUser: null });
  });

  it('clears cached server data when replacing the authenticated user', () => {
    queryClient.setQueryData(['activities', 'mine', 1], [{ id: 18 }]);

    useSessionStore.getState().setSession('new-token', USER);

    expect(queryClient.getQueryData(['activities', 'mine', 1])).toBeUndefined();
  });

  it('clears cached server data when signing out', () => {
    queryClient.setQueryData(['activities', 'mine', USER.id], [{ id: 23 }]);
    useSessionStore.setState({ accessToken: 'token', currentUser: USER });

    useSessionStore.getState().clearSession();

    expect(queryClient.getQueryData(['activities', 'mine', USER.id])).toBeUndefined();
  });
});
