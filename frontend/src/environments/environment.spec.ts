import { environment } from './environment';

describe('environment (dev)', () => {
  it('should have production flag set to false', () => {
    expect(environment.production).toBeFalse();
  });

  it('should have a non-empty apiBaseUrl pointing to the backend', () => {
    expect(environment.apiBaseUrl).toBeTruthy();
    expect(environment.apiBaseUrl).toContain('localhost');
  });
});
