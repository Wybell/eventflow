import { describe, expect, it } from 'vitest';
import { toApiError } from './http-client';

describe('toApiError', () => {
  it('maps unknown failures to a safe client error', () => {
    expect(toApiError(new Error('network interrupted'))).toEqual({
      code: 2000,
      message: '系统繁忙，请稍后重试',
      status: 0,
      requestId: null,
    });
  });
});
