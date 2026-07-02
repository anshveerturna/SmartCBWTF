export const hasFiniteCoordinate = (value: number | null | undefined): value is number =>
  typeof value === 'number' && Number.isFinite(value);

export const hasCoordinates = (
  latitude: number | null | undefined,
  longitude: number | null | undefined
): boolean => hasFiniteCoordinate(latitude) && hasFiniteCoordinate(longitude);

export const googleMapsUrl = (latitude: number, longitude: number): string =>
  `https://www.google.com/maps?q=${encodeURIComponent(`${latitude},${longitude}`)}`;

export const openExternalUrl = (url: string): void => {
  const opened = window.open(url, '_blank', 'noopener,noreferrer');
  if (opened) {
    opened.opener = null;
  }
};
