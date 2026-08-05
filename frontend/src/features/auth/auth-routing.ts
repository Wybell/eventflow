import type { CurrentUser } from './auth-types';

export function defaultRouteForUser(user: CurrentUser): string {
  if (user.roles.includes('ADMIN')) {
    return '/admin/activity-reviews';
  }
  return '/events';
}
