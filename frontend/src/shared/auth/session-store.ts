import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { CurrentUser } from '../../features/auth/auth-types';
import { queryClient } from '../lib/query-client';

interface SessionState {
  accessToken: string | null;
  refreshToken: string | null;
  currentUser: CurrentUser | null;
  setAccessToken: (accessToken: string) => void;
  setSession: (accessToken: string, refreshToken: string, currentUser: CurrentUser) => void;
  updateTokens: (accessToken: string, refreshToken: string) => void;
  updateCurrentUser: (currentUser: CurrentUser) => void;
  clearSession: () => void;
}

export const useSessionStore = create<SessionState>()(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      currentUser: null,
      setAccessToken: (accessToken) => set({ accessToken }),
      setSession: (accessToken, refreshToken, currentUser) => {
        queryClient.clear();
        set({ accessToken, refreshToken, currentUser });
      },
      updateTokens: (accessToken, refreshToken) => {
        set({ accessToken, refreshToken });
      },
      updateCurrentUser: (currentUser) => set({ currentUser }),
      clearSession: () => {
        queryClient.clear();
        set({ accessToken: null, refreshToken: null, currentUser: null });
      },
    }),
    {
      name: 'eventflow-session-v1',
      partialize: (state) => ({
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        currentUser: state.currentUser,
      }),
    },
  ),
);
