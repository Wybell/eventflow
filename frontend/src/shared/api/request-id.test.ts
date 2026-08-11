import { afterEach, describe, expect, it, vi } from 'vitest';
import { createRequestId } from './request-id';

describe('createRequestId', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('uses the native UUID generator when available', () => {
    const randomUUID = vi.fn(() => 'native-request-id');
    vi.stubGlobal('crypto', { randomUUID });

    expect(createRequestId()).toBe('native-request-id');
    expect(randomUUID).toHaveBeenCalledOnce();
  });

  it('creates a UUID v4 when randomUUID is unavailable on an HTTP origin', () => {
    const getRandomValues = vi.fn((values: Uint8Array) => {
      values.set(Array.from({ length: 16 }, (_, index) => index));
      return values;
    });
    vi.stubGlobal('crypto', { getRandomValues });

    expect(createRequestId()).toBe('00010203-0405-4607-8809-0a0b0c0d0e0f');
    expect(getRandomValues).toHaveBeenCalledOnce();
  });
});
