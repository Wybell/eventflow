import './app.css';
import { lazy, Suspense } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { LoginPage } from '../features/auth/LoginPage';

const ActivityWorkspace = lazy(async () => ({
  default: (await import('../features/activity/ActivityWorkspace')).ActivityWorkspace,
}));

export function App() {
  return (
    <main className="app-shell" aria-label="EventFlow application">
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/access" element={<Navigate to="/organizer/activities" replace />} />
        <Route
          path="/organizer/activities"
          element={
            <Suspense fallback={null}>
              <ActivityWorkspace />
            </Suspense>
          }
        />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </main>
  );
}
