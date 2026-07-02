import { useLocationTracker } from './useLocationTracker';

/**
 * LocationTracker component - include in layout for operational users.
 */
export default function LocationTracker() {
  const { isTracking } = useLocationTracker();

  // This is a silent tracker - no UI needed
  // Location is tracked automatically in the background
  if (isTracking) {
    console.debug('[LocationTracker] Tracking enabled');
  }

  return null;
}
