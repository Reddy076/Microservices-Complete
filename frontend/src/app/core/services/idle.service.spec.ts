import { TestBed, fakeAsync, tick, flush } from '@angular/core/testing';
import { IdleService } from './idle.service';
import { Router } from '@angular/router';
import { AuthenticationService } from '../api';
import { of, throwError } from 'rxjs';

describe('IdleService', () => {
  let service: IdleService;
  let routerSpy: jasmine.SpyObj<Router>;
  let authSpy: jasmine.SpyObj<AuthenticationService>;

  const TIMEOUT_MS = 30 * 60 * 1000; // 30 minutes (matches service implementation)

  beforeEach(() => {
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    authSpy = jasmine.createSpyObj('AuthenticationService', ['logout']);

    TestBed.configureTestingModule({
      providers: [
        IdleService,
        { provide: Router, useValue: routerSpy },
        { provide: AuthenticationService, useValue: authSpy }
      ]
    });

    service = TestBed.inject(IdleService);
    localStorage.clear();
  });

  afterEach(() => {
    service.stopWatching();
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should timeout and logout after 30 minutes of inactivity', fakeAsync(() => {
    localStorage.setItem('access_token', 'test-token');
    authSpy.logout.and.returnValue(of({}) as any);

    service.startWatching();

    // Fast forward almost 30 minutes
    tick(TIMEOUT_MS - 1000);
    expect(authSpy.logout).not.toHaveBeenCalled();

    // Fast forward the last second
    tick(1000);

    expect(authSpy.logout).toHaveBeenCalledWith('Bearer test-token');
    expect(localStorage.getItem('access_token')).toBeNull();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);

    flush();
  }));

  it('should reset timer on user activity', fakeAsync(() => {
    localStorage.setItem('access_token', 'test-token');
    authSpy.logout.and.returnValue(of({}) as any);

    service.startWatching();

    // Fast forward 20 minutes
    tick(20 * 60 * 1000);

    // Simulate mouse move activity
    document.dispatchEvent(new Event('mousemove'));

    // Fast forward another 20 minutes (total 40 mins, but only 20 mins since activity)
    tick(20 * 60 * 1000);

    expect(authSpy.logout).not.toHaveBeenCalled();

    // Fast forward another 10 minutes to reach the 30 min mark since activity
    tick(10 * 60 * 1000);

    expect(authSpy.logout).toHaveBeenCalled();

    flush();
  }));

  it('should handle backend logout error gracefully', fakeAsync(() => {
    localStorage.setItem('access_token', 'test-token');
    authSpy.logout.and.returnValue(throwError(() => new Error('API Error')));

    service.startWatching();

    tick(TIMEOUT_MS);

    expect(authSpy.logout).toHaveBeenCalled();
    expect(localStorage.getItem('access_token')).toBeNull(); // Still clears locally
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);

    flush();
  }));
});
