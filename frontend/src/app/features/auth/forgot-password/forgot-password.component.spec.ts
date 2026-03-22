import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ForgotPasswordComponent } from './forgot-password.component';
import { ReactiveFormsModule } from '@angular/forms';
import { Router, provideRouter } from '@angular/router';
import { AuthenticationService } from '../../../core/api/api/authentication.service';
import { of, throwError } from 'rxjs';

describe('ForgotPasswordComponent', () => {
  let component: ForgotPasswordComponent;
  let fixture: ComponentFixture<ForgotPasswordComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthenticationService>;
  let router: Router;

  beforeEach(async () => {
    localStorage.clear();
    const authSpy = jasmine.createSpyObj('AuthenticationService', ['resetPassword']);

    await TestBed.configureTestingModule({
      imports: [ForgotPasswordComponent, ReactiveFormsModule],
      providers: [
        { provide: AuthenticationService, useValue: authSpy },
        provideRouter([])
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ForgotPasswordComponent);
    component = fixture.componentInstance;

    authServiceSpy = TestBed.inject(AuthenticationService) as jasmine.SpyObj<AuthenticationService>;
    router = TestBed.inject(Router);
    spyOn(router, 'navigate');

    fixture.detectChanges();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should start at step 1', () => {
    expect(component.currentStep).toBe(1);
  });

  it('should move to step 2 when username is provided', () => {
    component.usernameForm.controls['username'].setValue('testuser');
    component.onSubmitUsername();
    expect(component.currentStep).toBe(2);
  });

  it('should not move to step 2 when username is empty', () => {
    component.onSubmitUsername();
    expect(component.currentStep).toBe(1);
  });

  it('should navigate between steps', () => {
    component.goToStep(3);
    expect(component.currentStep).toBe(3);
    component.goToStep(1);
    expect(component.currentStep).toBe(1);
  });

  it('should call resetPassword on valid submission', () => {
    component.usernameForm.controls['username'].setValue('testuser');

    component.securityAnswers.controls[0].patchValue({ question: 'Q1', answer: 'A1' });
    component.securityAnswers.controls[1].patchValue({ question: 'Q2', answer: 'A2' });
    component.securityAnswers.controls[2].patchValue({ question: 'Q3', answer: 'A3' });

    component.passwordForm.controls['newMasterPassword'].setValue('newpassword12');
    component.passwordForm.controls['confirmPassword'].setValue('newpassword12');

    authServiceSpy.resetPassword.and.returnValue(of({} as any));

    component.onSubmitRecovery();

    expect(authServiceSpy.resetPassword).toHaveBeenCalled();
    expect(component.successMessage).toContain('Password has been reset successfully');
  });

  it('should show error when passwords do not match', () => {
    component.usernameForm.controls['username'].setValue('testuser');

    component.securityAnswers.controls[0].patchValue({ question: 'Q1', answer: 'A1' });
    component.securityAnswers.controls[1].patchValue({ question: 'Q2', answer: 'A2' });
    component.securityAnswers.controls[2].patchValue({ question: 'Q3', answer: 'A3' });

    component.passwordForm.controls['newMasterPassword'].setValue('newpassword12');
    component.passwordForm.controls['confirmPassword'].setValue('different1234');

    component.onSubmitRecovery();

    expect(component.errorMessage).toBe('New passwords do not match.');
    expect(authServiceSpy.resetPassword).not.toHaveBeenCalled();
  });

  it('should display error on API failure', () => {
    component.usernameForm.controls['username'].setValue('testuser');

    component.securityAnswers.controls[0].patchValue({ question: 'Q1', answer: 'A1' });
    component.securityAnswers.controls[1].patchValue({ question: 'Q2', answer: 'A2' });
    component.securityAnswers.controls[2].patchValue({ question: 'Q3', answer: 'A3' });

    component.passwordForm.controls['newMasterPassword'].setValue('newpassword12');
    component.passwordForm.controls['confirmPassword'].setValue('newpassword12');

    const errorResponse = { error: { message: 'Invalid security answers' } };
    authServiceSpy.resetPassword.and.returnValue(throwError(() => errorResponse));

    component.onSubmitRecovery();

    expect(component.errorMessage).toBe('Invalid security answers');
    expect(component.isLoading).toBeFalse();
  });
});
