import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { CurrentUser } from '../../features/auth/auth-types';
import { queryClient } from '../lib/query-client';

interface SessionState {
  accessToken: string | null;
  currentUser: CurrentUser | null;
  setAccessToken: (accessToken: string) => void;
  setSession: (accessToken: string, currentUser: CurrentUser) => void;
  updateCurrentUser: (currentUser: CurrentUser) => void;
  clearSession: () => void;
}

export const useSessionStore = create<SessionState>()(
  persist(
    (set) => ({
      accessToken: null,
      currentUser: null,
      setAccessToken: (accessToken) => set({ accessToken }),
      setSession: (accessToken, currentUser) => {
        queryClient.clear();
        set({ accessToken, currentUser });
      },
      updateCurrentUser: (currentUser) => set({ currentUser }),
      clearSession: () => {
        queryClient.clear();
        set({ accessToken: null, currentUser: null });
      },
    }),
    {
      name: 'eventflow-session-v1',
      partialize: (state) => ({
        accessToken: state.accessToken,
        currentUser: state.currentUser,
      }),
    },
  ),
);
