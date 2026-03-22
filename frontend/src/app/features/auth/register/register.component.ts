import { Component, inject } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AuthenticationService } from '../../../core/api/api/authentication.service';
import { RegistrationRequest } from '../../../core/api/model/models';
import { CommonModule } from '@angular/common';
import { environment } from '../../../../environments/environment';
import { CustomSelectComponent, SelectOption } from '../../../shared/custom-select/custom-select.component';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, CommonModule, CustomSelectComponent],
  templateUrl: './register.component.html',
  styleUrl: './register.component.css'
})
export class RegisterComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthenticationService);
  private http = inject(HttpClient);
  private router = inject(Router);

  currentStep = 1; // 1 = Account, 2 = Password, 3 = Security Questions, 4 = Email Verify

  // Predefined list of security questions
  predefinedSecurityQuestions: string[] = [
    "What was your first pet's name?",
    "In what city were you born?",
    "What is your mother's maiden name?",
    "What was the name of your first school?",
    "What is your favorite book?"
  ];

  // Step 1: Account Info
  accountForm = this.fb.group({
    username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50)]],
    email: ['', [Validators.required, Validators.email]]
  });

  // Step 2: Password
  passwordForm = this.fb.group({
    password: ['', [Validators.required, Validators.minLength(12)]],
    confirmPassword: ['', [Validators.required]],
    passwordHint: ['', [Validators.maxLength(500)]]
  });

  // Step 3: Security Questions
  securityForm = this.fb.group({
    securityQuestions: this.fb.array([
      this.createSecurityQuestionGroup(),
      this.createSecurityQuestionGroup(),
      this.createSecurityQuestionGroup()
    ])
  }, { validators: this.uniqueQuestionsValidator() });

  // Step 4: Email OTP Verification
  otpForm = this.fb.group({
    otpCode: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(6)]]
  });

  get securityQuestions() {
    return this.securityForm.get('securityQuestions') as FormArray;
  }

  private createSecurityQuestionGroup(): FormGroup {
    return this.fb.group({
      question: ['', Validators.required],
      answer: ['', Validators.required]
    });
  }

  // Validator to ensure all 3 security questions are unique
  uniqueQuestionsValidator() {
    return (control: AbstractControl): ValidationErrors | null => {
      const formArray = control.get('securityQuestions') as FormArray;
      if (!formArray) return null;

      const questions = formArray.controls.map(group => group.get('question')?.value);
      const uniqueQuestions = new Set(questions.filter(q => q && q.trim() !== ''));

      // Check if there are duplicates among selected questions
      const hasDuplicates = questions.filter(q => q && q.trim() !== '').length !== uniqueQuestions.size;

      return hasDuplicates ? { duplicateQuestions: true } : null;
    };
  }

  // Get available questions as SelectOption[] for the custom select component
  getAvailableQuestionOptions(dropdownIndex: number): SelectOption[] {
    return this.getAvailableQuestions(dropdownIndex).map(q => ({ value: q, label: q }));
  }

  // Get available questions for a specific dropdown index (excludes questions selected in other dropdowns)
  getAvailableQuestions(dropdownIndex: number): string[] {
    const formArray = this.securityQuestions;
    const selectedQuestions = formArray.controls
      .map((group, index) => index !== dropdownIndex ? group.get('question')?.value : null)
      .filter(q => q && q.trim() !== '');

    return this.predefinedSecurityQuestions.filter(q => !selectedQuestions.includes(q));
  }

  // Check if there are duplicate questions selected
  hasDuplicateQuestions(): boolean {
    return this.securityForm.hasError('duplicateQuestions');
  }

  registeredUsername = '';
  registeredEmail = '';
  errorMessage = '';
  successMessage = '';
  isLoading = false;

  goToStep(step: number) {
    this.errorMessage = '';
    this.successMessage = '';
    this.currentStep = step;
  }

  onSubmitAccount() {
    if (this.accountForm.invalid) return;
    this.goToStep(2);
  }

  onSubmitPassword() {
    if (this.passwordForm.invalid) return;
    if (this.passwordForm.value.password !== this.passwordForm.value.confirmPassword) {
      this.errorMessage = 'Passwords do not match.';
      return;
    }
    this.goToStep(3);
  }

  onSubmitRegistration() {
    if (this.securityForm.invalid) return;

    this.isLoading = true;
    this.errorMessage = '';

    const req: RegistrationRequest = {
      username: this.accountForm.value.username as string,
      email: this.accountForm.value.email as string,
      masterPassword: this.passwordForm.value.password as string,
      passwordHint: this.passwordForm.value.passwordHint as string || undefined,
      securityQuestions: this.securityForm.value.securityQuestions as any[]
    };

    this.registeredUsername = req.username;
    this.registeredEmail = req.email;

    this.authService.register(req).subscribe({
      next: () => {
        this.isLoading = false;
        this.successMessage = `A verification code has been sent to ${this.registeredEmail}`;
        this.currentStep = 4;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Registration failed. Please try again.';
        this.isLoading = false;
      }
    });
  }

  onVerifyEmail() {
    if (this.otpForm.invalid) return;

    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    const code = this.otpForm.value.otpCode as string;

    this.http.post(`${environment.apiBaseUrl}/api/auth/verify-email`, null, {
      params: { username: this.registeredUsername, code },
      responseType: 'json'
    }).subscribe({
      next: () => {
        this.isLoading = false;
        this.successMessage = 'Email verified successfully! Redirecting to login...';
        setTimeout(() => this.router.navigate(['/login']), 2000);
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Invalid verification code. Please try again.';
        this.isLoading = false;
      }
    });
  }

  onResendOtp() {
    this.errorMessage = '';
    this.successMessage = '';
    this.isLoading = true;

    this.http.post(`${environment.apiBaseUrl}/api/auth/resend-verification-otp`, null, {
      params: { username: this.registeredUsername },
      responseType: 'json'
    }).subscribe({
      next: () => {
        this.isLoading = false;
        this.successMessage = `A new verification code has been sent to ${this.registeredEmail}`;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to resend code. Please try again.';
        this.isLoading = false;
      }
    });
  }
}
