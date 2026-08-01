import { create } from 'zustand';
import type { CurrentUser } from '../../features/auth/auth-types';

interface SessionState {
  accessToken: string | null;
  currentUser: CurrentUser | null;
  setAccessToken: (accessToken: string) => void;
  setSession: (accessToken: string, currentUser: CurrentUser) => void;
  clearSession: () => void;
}

export const useSessionStore = create<SessionState>((set) => ({
  accessToken: null,
  currentUser: null,
  setAccessToken: (accessToken) => set({ accessToken }),
  setSession: (accessToken, currentUser) => set({ accessToken, currentUser }),
  clearSession: () => set({ accessToken: null, currentUser: null }),
}));
