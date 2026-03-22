import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { VaultService } from '../../../core/api/api/vault.service';
import { VaultEntryDetailResponse } from '../../../core/api/model/vaultEntryDetailResponse';
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-master-password-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './master-password-modal.component.html',
  styleUrl: './master-password-modal.component.css'
})
export class MasterPasswordModalComponent {
  private vaultService = inject(VaultService);

  @Input() entryId!: number;

  /**
   * 'copy'  → emits passwordDecrypted(string) — existing "Copy Password" flow
   * 'edit'  → emits entryUnlocked(VaultEntryDetailResponse) — new "Unlock for Edit" flow
   */
  @Input() mode: 'copy' | 'edit' = 'copy';

  /** Emitted in 'copy' mode with just the decrypted password string. */
  @Output() passwordDecrypted = new EventEmitter<string>();

  /** Emitted in 'edit' mode with the full decrypted entry so the form can be patched. */
  @Output() entryUnlocked = new EventEmitter<VaultEntryDetailResponse>();

  @Output() modalCancel = new EventEmitter<void>();

  masterPassword = '';
  otpToken = '';
  isSubmitting = false;
  errorMessage = '';
  showOtpField = false;

  onSubmit() {
    if (!this.masterPassword.trim()) {
      this.errorMessage = 'Master password is required';
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';

    const request = {
      masterPassword: this.masterPassword,
      ...(this.otpToken ? { otpToken: this.otpToken } : {})
    };

    this.vaultService.accessSensitiveEntry(this.entryId, request).subscribe({
      next: (response: VaultEntryDetailResponse) => {
        this.isSubmitting = false;

        if (this.mode === 'edit') {
          // Emit the full entry so the edit form can patch all fields
          this.entryUnlocked.emit(response);
        } else {
          // Legacy 'copy' mode: emit just the password
          if (response && response.password && response.password !== '******') {
            this.passwordDecrypted.emit(response.password);
          } else {
            this.errorMessage = 'Failed to decrypt password. Invalid response.';
          }
        }
      },
      error: (err: any) => {
        this.isSubmitting = false;
        const msg = err.error?.message || 'Access denied or server error.';

        if (msg.toLowerCase().includes('otp is required')) {
          if (!this.showOtpField) {
            // First time they are told OTP is required. Show the field, but hide the scary red error.
            this.showOtpField = true;
            this.errorMessage = '';
          } else {
            // They submitted without an OTP when the field was already visible, show the error.
            this.errorMessage = 'Please enter your Authenticator Code.';
          }
        } else if (msg.toLowerCase().includes('invalid otp')) {
          this.showOtpField = true;
          this.errorMessage = 'Invalid Authenticator Code. Please try again.';
        } else {
          this.errorMessage = msg;
        }
      }
    });
  }

  onCancel() {
    this.modalCancel.emit();
  }
}
