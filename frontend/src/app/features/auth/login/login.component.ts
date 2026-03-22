import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { environment } from '../../../../environments/environment';
import { LucideAngularModule } from 'lucide-angular';
import { RecaptchaModule } from 'ng-recaptcha';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, CommonModule, LucideAngularModule, RecaptchaModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  loginForm = this.fb.group({
    username: ['', [Validators.required]],
    password: ['', [Validators.required]]
  });

  otpForm = this.fb.group({
    otpCode: ['', [Validators.required]]
  });

  errorMessage = '';
  successMessage = '';
  isLoading = false;
  showOtpStep = false;
  useBackupCode = false;
  pendingUsername = '';
  hintMessage = '';
  showHint = false;
  isLoadingHint = false;

  // CAPTCHA
  recaptchaSiteKey = environment.recaptchaSiteKey;
  captchaToken: string | null = null;
  showCaptcha = false;
  failedAttempts = 0;

  onSubmit() {
    if (this.loginForm.invalid) {
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    // Check if CAPTCHA is required after 3 failed attempts
    if (this.failedAttempts >= 3 && !this.captchaToken) {
      this.showCaptcha = true;
      this.errorMessage = 'Please complete the CAPTCHA verification';
      this.isLoading = false;
      return;
    }

    const body: any = {
      username: this.loginForm.value.username as string,
      masterPassword: this.loginForm.value.password as string,
    };

    // Add CAPTCHA token if available
    if (this.captchaToken) {
      body.captchaToken = this.captchaToken;
    }

    // Use HttpClient directly with responseType: 'json' to avoid the auto-generated
    // service setting responseType to 'blob' (caused by Accept: '*/*' failing isJsonMime).
    this.http.post<any>(
      `${environment.apiBaseUrl}/api/auth/login`,
      body,
      {
        headers: new HttpHeaders({ 'Content-Type': 'application/json', 'Accept': 'application/json' }),
        responseType: 'json'
      }
    ).subscribe({
      next: (response: any) => {
        // Clear duress mode on normal login
        localStorage.removeItem('duress_mode');

        const is2FaRequired = Object.keys(response).some(
          key => key.toLowerCase().includes('requires2fa') && (response[key] === true || String(response[key]).toLowerCase() === 'true')
        );

        if (is2FaRequired) {
          this.pendingUsername = response.username || body.username;
          this.showOtpStep = true;
          this.successMessage = 'Enter the 6-digit code sent to your email.';
          this.isLoading = false;
          return;
        }

        const token = response.accessToken || response.access_token;
        const refToken = response.refreshToken || response.refresh_token;

        if (token) {
          localStorage.setItem('access_token', token);
          if (refToken) {
            localStorage.setItem('refresh_token', refToken);
          }
          this.router.navigate(['/dashboard']);
        } else {
          console.error('Login response missing token. Response object:', response);
          this.errorMessage = 'Login failed. Please try again.';
        }
        this.isLoading = false;
      },
      error: (err: any) => {
        // Track failed attempts for CAPTCHA
        this.failedAttempts++;
        if (this.failedAttempts >= 3) {
          this.showCaptcha = true;
        }
        // Try duress login if regular login fails
        this.tryDuressLogin(body);
      }
    });
  }

  private tryDuressLogin(body: { username: string; masterPassword: string }) {
    this.http.post<any>(
      `${environment.apiBaseUrl}/api/auth/duress-login`,
      body,
      {
        headers: new HttpHeaders({ 'Content-Type': 'application/json', 'Accept': 'application/json' }),
        responseType: 'json'
      }
    ).subscribe({
      next: (response: any) => {
        const token = response.accessToken || response.access_token;
        if (token) {
          localStorage.setItem('access_token', token);
          localStorage.setItem('duress_mode', 'true');
          this.router.navigate(['/dashboard']);
        } else {
          this.errorMessage = 'Login failed. Please try again.';
          this.isLoading = false;
        }
      },
      error: (err: any) => {
        this.errorMessage = err.error?.message || 'Invalid username or password';
        this.isLoading = false;
      }
    });
  }

  onSubmitOtp() {
    if (this.otpForm.invalid) return;

    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    let code = (this.otpForm.get('otpCode')?.value || '').toString().trim();
    // Optional: Strip any spaces
    code = code.replaceAll(/\s+/g, '');

    if (!code) {
      this.errorMessage = 'Please enter the verification code.';
      this.isLoading = false;
      return;
    }

    this.http.post<any>(
      `${environment.apiBaseUrl}/api/auth/verify-otp?username=${encodeURIComponent(this.pendingUsername)}&code=${encodeURIComponent(code)}`,
      null,
      {
        headers: new HttpHeaders({ 'Accept': 'application/json' }),
        responseType: 'json'
      }
    ).subscribe({
      next: (response: any) => {
        const token = response.accessToken || response.access_token;
        const refToken = response.refreshToken || response.refresh_token;

        if (token) {
          localStorage.setItem('access_token', token);
          if (refToken) {
            localStorage.setItem('refresh_token', refToken);
          }
          this.router.navigate(['/dashboard']);
        } else {
          console.error('OTP Verification missing token. Response object:', response);
          this.errorMessage = 'Verification failed. No token received.';
        }
        this.isLoading = false;
      },
      error: (err: any) => {
        this.errorMessage = err.error?.message || 'Invalid verification code. Please try again.';
        this.isLoading = false;
      }
    });
  }

  onResendOtp() {
    this.errorMessage = '';
    this.successMessage = '';
    this.isLoading = true;

    this.http.post<any>(`${environment.apiBaseUrl}/api/auth/resend-otp`, null, {
      params: { username: this.pendingUsername }
    }).subscribe({
      next: () => {
        this.successMessage = 'A new verification code has been sent.';
        this.isLoading = false;
      },
      error: (err: any) => {
        this.errorMessage = err.error?.message || 'Failed to resend code. Please try again.';
        this.isLoading = false;
      }
    });
  }

  backToLogin() {
    this.showOtpStep = false;
    this.useBackupCode = false;
    this.pendingUsername = '';
    this.errorMessage = '';
    this.successMessage = '';
    this.otpForm.reset();
  }

  toggleBackupCode() {
    this.useBackupCode = !this.useBackupCode;
    this.otpForm.reset();
    this.errorMessage = '';
  }

  onShowHint() {
    const username = this.loginForm.value.username;
    if (!username) {
      this.errorMessage = 'Please enter your username first';
      return;
    }

    this.isLoadingHint = true;
    this.errorMessage = '';
    this.hintMessage = '';

    this.http.get<any>(
      `${environment.apiBaseUrl}/api/auth/password-hint/${encodeURIComponent(username)}`,
      {
        headers: new HttpHeaders({ 'Accept': 'application/json' }),
        responseType: 'json'
      }
    ).subscribe({
      next: (response: any) => {
        this.hintMessage = response.hint || 'No hint set';
        this.showHint = true;
        this.isLoadingHint = false;
      },
      error: (err: any) => {
        this.errorMessage = err.error?.message || 'Failed to retrieve hint';
        this.isLoadingHint = false;
      }
    });
  }

  hideHint() {
    this.showHint = false;
    this.hintMessage = '';
  }

  // CAPTCHA handlers
  onCaptchaResolved(token: string | null) {
    this.captchaToken = token;
    if (token) {
      this.errorMessage = '';
    }
  }

  onCaptchaExpired() {
    this.captchaToken = null;
  }
}
