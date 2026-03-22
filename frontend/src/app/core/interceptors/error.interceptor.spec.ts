import { TestBed } from '@angular/core/testing';
import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { errorInterceptor } from './error.interceptor';
import { AuthenticationService } from '../api';
import { of, throwError } from 'rxjs';

describe('errorInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let routerSpy: jasmine.SpyObj<Router>;
  let authSpy: jasmine.SpyObj<AuthenticationService>;

  beforeEach(() => {
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    authSpy = jasmine.createSpyObj('AuthenticationService', ['refreshToken']);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        { provide: Router, useValue: routerSpy },
        { provide: AuthenticationService, useValue: authSpy }
      ]
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    localStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should attempt refresh token on 401 response if refresh_token exists', () => {
    localStorage.setItem('access_token', 'old-token');
    localStorage.setItem('refresh_token', 'valid-refresh');

    authSpy.refreshToken.and.returnValue(of({
      accessToken: 'new-token',
      refreshToken: 'new-refresh'
    }) as any);

    http.get('/test').subscribe(res => {
      expect(res).toBeTruthy();
    });

    const req = httpMock.expectOne('/test');
    req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });

    expect(authSpy.refreshToken).toHaveBeenCalledWith({ refreshToken: 'valid-refresh' });
    expect(localStorage.getItem('access_token')).toBe('new-token');
    expect(localStorage.getItem('refresh_token')).toBe('new-refresh');

    // Expected a retry request due to successful refresh
    const retryReq = httpMock.expectOne('/test');
    expect(retryReq.request.headers.get('Authorization')).toBe('Bearer new-token');
    retryReq.flush({ success: true });
  });

  it('should logout on 401 response if refresh fails', (done) => {
    localStorage.setItem('access_token', 'old-token');
    localStorage.setItem('refresh_token', 'invalid-refresh');

    // Use 401 HttpErrorResponse so the catchError branch calls logoutAndRedirect()
    authSpy.refreshToken.and.returnValue(throwError(() =>
      new HttpErrorResponse({ status: 401, statusText: 'Unauthorized' })
    ));

    http.get('/test').subscribe({
      next: () => fail('Should have failed'),
      error: (err) => {
        // The interceptor propagates the refresh error — message may vary
        expect(err).toBeTruthy();
        done();
      }
    });

    const req = httpMock.expectOne('/test');
    req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });

    expect(authSpy.refreshToken).toHaveBeenCalled();
    expect(localStorage.getItem('access_token')).toBeNull();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should logout on 401 response if no refresh_token exists', () => {
    localStorage.setItem('access_token', 'old-token');

    http.get('/test').subscribe({
      next: () => fail('Should have failed'),
      error: (err) => {
        expect(err.message).toBe('Session expired. Please log in again.');
      }
    });

    const req = httpMock.expectOne('/test');
    req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });

    expect(authSpy.refreshToken).not.toHaveBeenCalled();
    expect(localStorage.getItem('access_token')).toBeNull();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should pass through other errors without redirecting to login', () => {
    localStorage.setItem('access_token', 'test-token');

    http.get('/test').subscribe({
      next: () => fail('Should have failed with 500 error'),
      error: (err) => {
        expect(err.status).toBe(500);
      }
    });

    const req = httpMock.expectOne('/test');
    req.flush('Server Error', { status: 500, statusText: 'Server Error' });

    expect(localStorage.getItem('access_token')).toBe('test-token');
    expect(routerSpy.navigate).not.toHaveBeenCalled();
  });
});