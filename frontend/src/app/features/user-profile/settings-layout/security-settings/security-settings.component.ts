import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { UserControllerService, AuthenticationService, TwoFactorAuthControllerService, SecurityQuestionDTO, SetPasswordHintRequest, UpdateSecurityQuestionsRequest } from '../../../../core/api';
import { CommonModule } from '@angular/common';
import { NotificationEventService } from '../../../../core/services/notification-event.service';
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-security-settings',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule, LucideAngularModule],
  templateUrl: './security-settings.component.html',
  styleUrl: './security-settings.component.css'
})
export class SecuritySettingsComponent implements OnInit {
  private userService = inject(UserControllerService);
  private authService = inject(AuthenticationService);
  private twoFactorService = inject(TwoFactorAuthControllerService);
  private notificationEventService = inject(NotificationEventService);
  private fb = inject(FormBuilder);

  successMessage = '';
  errorMessage = '';

  // Security Hint & Questions
  currentHint = '';
  hasSecurityQuestions = false;
  isLoadingSecurity = true;

  hintForm = this.fb.group({
    hint: ['', [Validators.maxLength(100)]],
    masterPassword: ['', [Validators.required]]
  });

  duressPasswordForm = this.fb.group({
    duressPassword: ['', [Validators.required, Validators.minLength(6)]],
    confirmPassword: ['', [Validators.required]],
    masterPassword: ['', [Validators.required]]
  });

  securityQuestionsForm = this.fb.group({
    q1: ['', [Validators.required]],
    a1: ['', [Validators.required]],
    q2: ['', [Validators.required]],
    a2: ['', [Validators.required]],
    q3: ['', [Validators.required]],
    a3: ['', [Validators.required]],
    masterPassword: ['', [Validators.required]]
  });

  // 2FA State
  is2FAEnabled = false;
  isLoading2FA = true;
  showSetupStep = false;
  showVerifyStep = false;
  showBackupCodes = false;
  qrCodeUrl = '';
  secretKey = '';
  backupCodes: string[] = [];

  setupCodeForm = this.fb.group({
    verificationCode: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(6)]]
  });

  ngOnInit() {
    this.loadProfileForUsername();
    this.load2FAStatus();
  }

  // Need username to fetch the hint
  private loadProfileForUsername() {
    this.userService.getProfile().subscribe({
      next: (profile) => {
        if (profile.username) {
          this.loadSecurityData(profile.username);
        }
      },
      error: () => {
        this.isLoadingSecurity = false;
      }
    });
  }

  loadSecurityData(username: string) {
    this.authService.getPasswordHint(username).subscribe({
      next: (res) => {
        this.currentHint = res.hint || 'No hint set';
        this.hintForm.patchValue({ hint: this.currentHint === 'No hint set' ? '' : this.currentHint });
      },
      error: () => { }
    });

    this.userService.getSecurityQuestions().subscribe({
      next: (questions) => {
        if (questions && questions.length >= 3) {
          this.hasSecurityQuestions = true;
          this.securityQuestionsForm.patchValue({
            q1: questions[0].question,
            q2: questions[1].question,
            q3: questions[2].question
          });
        }
        this.isLoadingSecurity = false;
      },
      error: () => {
        this.isLoadingSecurity = false;
      }
    });
  }

  onUpdateHintSubmit() {
    if (this.hintForm.invalid) return;

    this.errorMessage = '';
    this.successMessage = '';

    const req: SetPasswordHintRequest = {
      hint: this.hintForm.value.hint as string || '',
      masterPassword: this.hintForm.value.masterPassword as string
    };

    this.authService.setPasswordHint(req).subscribe({
      next: (res) => {
        this.successMessage = res.message || 'Password hint updated!';
        this.currentHint = this.hintForm.value.hint as string || 'No hint set';
        this.hintForm.patchValue({ masterPassword: '' });
        this.notificationEventService.triggerRefresh();
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to update hint. Check your master password.';
      }
    });
  }

  onSetDuressPasswordSubmit() {
    if (this.duressPasswordForm.invalid) return;

    this.errorMessage = '';
    this.successMessage = '';

    const duressPassword = this.duressPasswordForm.value.duressPassword as string;
    const confirmPassword = this.duressPasswordForm.value.confirmPassword as string;

    if (duressPassword !== confirmPassword) {
      this.errorMessage = 'Duress passwords do not match';
      return;
    }

    const req = {
      duressPassword: duressPassword
    };

    this.authService.setDuressPassword(req).subscribe({
      next: (res) => {
        this.successMessage = res.message || 'Duress password set successfully!';
        this.duressPasswordForm.reset();
        this.notificationEventService.triggerRefresh();
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to set duress password.';
      }
    });
  }

  onUpdateSecurityQuestionsSubmit() {
    if (this.securityQuestionsForm.invalid) return;

    this.errorMessage = '';
    this.successMessage = '';

    const questions: SecurityQuestionDTO[] = [
      { question: this.securityQuestionsForm.value.q1 as string, answer: this.securityQuestionsForm.value.a1 as string },
      { question: this.securityQuestionsForm.value.q2 as string, answer: this.securityQuestionsForm.value.a2 as string },
      { question: this.securityQuestionsForm.value.q3 as string, answer: this.securityQuestionsForm.value.a3 as string }
    ];

    const req: UpdateSecurityQuestionsRequest = {
      securityQuestions: questions,
      masterPassword: this.securityQuestionsForm.value.masterPassword as string
    };

    this.userService.updateSecurityQuestions(req).subscribe({
      next: () => {
        this.successMessage = 'Security questions updated successfully!';
        this.hasSecurityQuestions = true;
        this.securityQuestionsForm.patchValue({ a1: '', a2: '', masterPassword: '' });
        this.notificationEventService.triggerRefresh();
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to update security questions. Check your master password.';
      }
    });
  }

  // --- 2FA Methods ---

  load2FAStatus() {
    this.twoFactorService.getStatus().subscribe({
      next: (status) => {
        this.is2FAEnabled = status.enabled || false;
        this.isLoading2FA = false;
      },
      error: () => {
        this.isLoading2FA = false;
      }
    });
  }

  onStartSetup() {
    this.errorMessage = '';
    this.successMessage = '';
    this.showSetupStep = true;
    this.showVerifyStep = false;

    this.twoFactorService.setup2FA().subscribe({
      next: (response) => {
        this.qrCodeUrl = response.qrCodeUrl || '';
        this.secretKey = response.secretKey || '';
        this.showVerifyStep = true;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to initialize 2FA setup.';
        this.showSetupStep = false;
      }
    });
  }

  onVerifySetup() {
    if (this.setupCodeForm.invalid) return;

    this.errorMessage = '';
    this.successMessage = '';

    const code = this.setupCodeForm.value.verificationCode as string;

    this.twoFactorService.verifySetup(code).subscribe({
      next: (response) => {
        this.is2FAEnabled = true;
        this.backupCodes = response.backupCodes || [];
        this.showSetupStep = false;
        this.showVerifyStep = false;
        this.showBackupCodes = true;
        this.setupCodeForm.reset();
        this.successMessage = '2FA has been enabled successfully! Save your backup codes.';
        this.notificationEventService.triggerRefresh();
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Invalid verification code. Please try again.';
      }
    });
  }

  onCancelSetup() {
    this.showSetupStep = false;
    this.showVerifyStep = false;
    this.qrCodeUrl = '';
    this.secretKey = '';
    this.setupCodeForm.reset();
  }

  onDisable2FA() {
    this.errorMessage = '';
    this.successMessage = '';

    this.twoFactorService.disable2FA().subscribe({
      next: () => {
        this.is2FAEnabled = false;
        this.backupCodes = [];
        this.showBackupCodes = false;
        this.successMessage = '2FA has been disabled.';
        this.notificationEventService.triggerRefresh();
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to disable 2FA.';
      }
    });
  }

  onViewBackupCodes() {
    this.errorMessage = '';

    this.twoFactorService.getBackupCodes().subscribe({
      next: (codes) => {
        this.backupCodes = codes;
        this.showBackupCodes = true;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to load backup codes.';
      }
    });
  }

  onRegenerateCodes() {
    this.errorMessage = '';
    this.successMessage = '';

    this.twoFactorService.regenerateCodes().subscribe({
      next: (response) => {
        this.backupCodes = response.backupCodes || [];
        this.showBackupCodes = true;
        this.successMessage = 'Backup codes regenerated successfully! Save your new codes.';
        this.notificationEventService.triggerRefresh();
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to regenerate backup codes.';
      }
    });
  }

  onHideBackupCodes() {
    this.showBackupCodes = false;
  }

  copyBackupCodes() {
    const codesText = this.backupCodes.join('\n');
    navigator.clipboard.writeText(codesText).then(() => {
      this.successMessage = 'Backup codes copied to clipboard!';
    });
  }
}
