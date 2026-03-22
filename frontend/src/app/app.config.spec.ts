import { appConfig } from './app.config';
import { BASE_PATH } from './core/api';
import { environment } from '../environments/environment';

describe('appConfig', () => {
  it('should have providers defined', () => {
    expect(appConfig.providers).toBeTruthy();
    expect(appConfig.providers.length).toBeGreaterThan(0);
  });

  it('should provide BASE_PATH token with environment.apiBaseUrl', () => {
    const basePathProvider = appConfig.providers.find(
      (p: any) => p && p.provide === BASE_PATH
    ) as any;
    expect(basePathProvider).toBeTruthy();
    expect(basePathProvider.useValue).toBe(environment.apiBaseUrl);
  });
});
