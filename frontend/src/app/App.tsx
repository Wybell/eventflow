import './app.css';
import { lazy, Suspense } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { defaultRouteForUser } from '../features/auth/auth-routing';
import { LoginPage } from '../features/auth/LoginPage';
import { useSessionStore } from '../shared/auth/session-store';

const ActivityWorkspace = lazy(async () => ({
  default: (await import('../features/activity/ActivityWorkspace')).ActivityWorkspace,
}));
const RegisterPage = lazy(async () => ({
  default: (await import('../features/auth/RegisterPage')).RegisterPage,
}));
const EventExplorePage = lazy(async () => ({
  default: (await import('../features/participant/EventExplorePage')).EventExplorePage,
}));
const OrganizerApplicationsPage = lazy(async () => ({
  default: (await import('../features/admin/OrganizerApplicationsPage')).OrganizerApplicationsPage,
}));

export function App() {
  const currentUser = useSessionStore((state) => state.currentUser);
  return (
    <main className="app-shell" aria-label="EventFlow application">
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/register"
          element={
            <Suspense fallback={null}>
              <RegisterPage />
            </Suspense>
          }
        />
        <Route
          path="/access"
          element={
            <Navigate to={currentUser ? defaultRouteForUser(currentUser) : '/login'} replace />
          }
        />
        <Route
          path="/events"
          element={
            <Suspense fallback={null}>
              <RoleRoute roles={['USER']}>
                <EventExplorePage />
              </RoleRoute>
            </Suspense>
          }
        />
        <Route
          path="/organizer/activities"
          element={
            <Suspense fallback={null}>
              <RoleRoute roles={['ORGANIZER']}>
                <ActivityWorkspace />
              </RoleRoute>
            </Suspense>
          }
        />
        <Route
          path="/admin/organizer-applications"
          element={
            <Suspense fallback={null}>
              <RoleRoute roles={['ADMIN']}>
                <OrganizerApplicationsPage />
              </RoleRoute>
            </Suspense>
          }
        />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </main>
  );
}

function RoleRoute({ children, roles }: { children: React.ReactNode; roles: string[] }) {
  const currentUser = useSessionStore((state) => state.currentUser);
  if (currentUser === null) {
    return <Navigate to="/login" replace />;
  }
  if (!roles.some((role) => currentUser.roles.includes(role))) {
    return <Navigate to={defaultRouteForUser(currentUser)} replace />;
  }
  return <>{children}</>;
}
