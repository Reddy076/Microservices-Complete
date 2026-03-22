import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { RegisterComponent } from './register.component';
import { ReactiveFormsModule } from '@angular/forms';
import { Router, provideRouter } from '@angular/router';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AuthenticationService } from '../../../core/api/api/authentication.service';
import { of, throwError } from 'rxjs';
import { environment } from '../../../../environments/environment';

describe('RegisterComponent', () => {
  let component: RegisterComponent;
  let fixture: ComponentFixture<RegisterComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthenticationService>;
  let httpMock: HttpTestingController;
  let router: Router;

  beforeEach(async () => {
    localStorage.clear();
    const authSpy = jasmine.createSpyObj('AuthenticationService', ['register']);

    await TestBed.configureTestingModule({
      imports: [RegisterComponent, ReactiveFormsModule, HttpClientTestingModule],
      providers: [
        { provide: AuthenticationService, useValue: authSpy },
        provideRouter([])
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;

    authServiceSpy = TestBed.inject(AuthenticationService) as jasmine.SpyObj<AuthenticationService>;
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    spyOn(router, 'navigate');

    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should start at step 1', () => {
    expect(component.currentStep).toBe(1);
  });

  it('should invalidate step 1 when empty', () => {
    expect(component.accountForm.valid).toBeFalsy();
  });

  it('should move to step 2 when account info is valid', () => {
    component.accountForm.controls['username'].setValue('testuser');
    component.accountForm.controls['email'].setValue('test@test.com');
    component.onSubmitAccount();
    expect(component.currentStep).toBe(2);
  });

  it('should not move to step 2 when account info is invalid', () => {
    component.onSubmitAccount();
    expect(component.currentStep).toBe(1);
  });

  it('should move to step 3 when passwords are valid and match', () => {
    component.passwordForm.controls['password'].setValue('password123456');
    component.passwordForm.controls['confirmPassword'].setValue('password123456');
    component.onSubmitPassword();
    expect(component.currentStep).toBe(3);
  });

  it('should show error when passwords do not match', () => {
    component.goToStep(2);
    component.passwordForm.controls['password'].setValue('password123456');
    component.passwordForm.controls['confirmPassword'].setValue('different1234');
    component.onSubmitPassword();
    expect(component.errorMessage).toBe('Passwords do not match.');
    expect(component.currentStep).toBe(2);
  });

  it('should navigate between steps', () => {
    component.goToStep(3);
    expect(component.currentStep).toBe(3);
    component.goToStep(1);
    expect(component.currentStep).toBe(1);
  });

  it('should move to step 4 (email verify) on successful registration', () => {
    component.accountForm.controls['username'].setValue('testuser');
    component.accountForm.controls['email'].setValue('test@test.com');
    component.passwordForm.controls['password'].setValue('password123456');
    component.passwordForm.controls['confirmPassword'].setValue('password123456');
    component.securityQuestions.controls[0].patchValue({ question: 'Q1', answer: 'A1' });
    component.securityQuestions.controls[1].patchValue({ question: 'Q2', answer: 'A2' });
    component.securityQuestions.controls[2].patchValue({ question: 'Q3', answer: 'A3' });

    authServiceSpy.register.and.returnValue(of({} as any));

    component.onSubmitRegistration();

    expect(authServiceSpy.register).toHaveBeenCalled();
    expect(component.currentStep).toBe(4);
    expect(component.registeredUsername).toBe('testuser');
    expect(component.registeredEmail).toBe('test@test.com');
    expect(component.successMessage).toContain('test@test.com');
    expect(component.isLoading).toBeFalse();
  });

  it('should display error message on registration failure', () => {
    component.accountForm.controls['username'].setValue('testuser');
    component.accountForm.controls['email'].setValue('test@test.com');
    component.passwordForm.controls['password'].setValue('password123456');
    component.passwordForm.controls['confirmPassword'].setValue('password123456');
    component.securityQuestions.controls[0].patchValue({ question: 'Q1', answer: 'A1' });
    component.securityQuestions.controls[1].patchValue({ question: 'Q2', answer: 'A2' });
    component.securityQuestions.controls[2].patchValue({ question: 'Q3', answer: 'A3' });

    const errorResponse = { error: { message: 'Username already taken' } };
    authServiceSpy.register.and.returnValue(throwError(() => errorResponse));

    component.onSubmitRegistration();

    expect(component.errorMessage).toBe('Username already taken');
    expect(component.isLoading).toBeFalse();
  });

  it('should verify email with OTP code', fakeAsync(() => {
    component.registeredUsername = 'testuser';
    component.currentStep = 4;
    component.otpForm.controls['otpCode'].setValue('123456');

    component.onVerifyEmail();

    const req = httpMock.expectOne(r => r.url === `${environment.apiBaseUrl}/api/auth/verify-email`);
    expect(req.request.method).toBe('POST');
    expect(req.request.params.get('username')).toBe('testuser');
    expect(req.request.params.get('code')).toBe('123456');
    req.flush({ message: 'Email verified successfully. You can now log in.' });

    expect(component.successMessage).toContain('Email verified');
    tick(2000);
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  }));

  it('should show error on invalid OTP', () => {
    component.registeredUsername = 'testuser';
    component.currentStep = 4;
    component.otpForm.controls['otpCode'].setValue('000000');

    component.onVerifyEmail();

    const req = httpMock.expectOne(r => r.url === `${environment.apiBaseUrl}/api/auth/verify-email`);
    req.flush({ message: 'Invalid OTP' }, { status: 400, statusText: 'Bad Request' });

    expect(component.errorMessage).toContain('Invalid OTP');
    expect(component.isLoading).toBeFalse();
  });

  it('should resend verification OTP', () => {
    component.registeredUsername = 'testuser';
    component.registeredEmail = 'test@test.com';
    component.currentStep = 4;

    component.onResendOtp();

    const req = httpMock.expectOne(r => r.url === `${environment.apiBaseUrl}/api/auth/resend-verification-otp`);
    expect(req.request.method).toBe('POST');
    expect(req.request.params.get('username')).toBe('testuser');
    req.flush({ message: 'Verification code resent to your email.' });

    expect(component.successMessage).toContain('test@test.com');
    expect(component.isLoading).toBeFalse();
  });
});
