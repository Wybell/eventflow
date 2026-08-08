import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it } from 'vitest';
import type { CurrentUser } from '../auth/auth-types';
import { useSessionStore } from '../../shared/auth/session-store';
import { ProfileMenu } from './ProfileMenu';

const USER: CurrentUser = {
  id: 7,
  username: 'wybell',
  displayName: 'Wybell',
  avatarUrl: null,
  mobile: null,
  email: null,
  organizationId: null,
  roles: ['USER'],
};

describe('ProfileMenu', () => {
  beforeEach(() => {
    useSessionStore.setState({ accessToken: 'token', currentUser: USER });
  });

  it('opens the current users profile from the default avatar trigger', async () => {
    render(
      <QueryClientProvider client={new QueryClient()}>
        <BrowserRouter>
          <ProfileMenu />
        </BrowserRouter>
      </QueryClientProvider>,
    );

    fireEvent.click(screen.getByRole('button', { name: 'Wybell，打开个人菜单' }));
    fireEvent.click(await screen.findByText('个人资料'));

    expect(await screen.findByText('个人中心')).toBeInTheDocument();
    expect(screen.getByDisplayValue('wybell')).toBeDisabled();
    expect(screen.getByDisplayValue('Wybell')).toBeInTheDocument();
  });
});
