import type { CurrentUser } from './auth-types';

export function defaultRouteForUser(user: CurrentUser): string {
  if (user.roles.includes('ADMIN')) {
    return '/admin/organizer-applications';
  }
  if (user.roles.includes('ORGANIZER')) {
    return '/organizer/activities';
  }
  return '/events';
}
