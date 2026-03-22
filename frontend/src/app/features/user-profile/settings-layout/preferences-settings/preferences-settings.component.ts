import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { UserSettingsControllerService, UserControllerService, UserSettingsResponse, UserResponse, AccountDeletionRequest } from '../../../../core/api';
import { CommonModule } from '@angular/common';
import { NotificationEventService } from '../../../../core/services/notification-event.service';
import { LucideAngularModule } from 'lucide-angular';
import { Router } from '@angular/router';

@Component({
  selector: 'app-preferences-settings',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule, LucideAngularModule],
  templateUrl: './preferences-settings.component.html',
  styleUrl: './preferences-settings.component.css'
})
export class PreferencesSettingsComponent implements OnInit {
  private settingsService = inject(UserSettingsControllerService);
  private userService = inject(UserControllerService);
  private notificationEventService = inject(NotificationEventService);
  private fb = inject(FormBuilder);
  private router = inject(Router);

  userSettings: UserSettingsResponse | null = null;
  userProfile: UserResponse | null = null;

  isLoadingSettings = true;
  successMessage = '';
  errorMessage = '';

  readOnlyForm = this.fb.group({
    readOnlyMode: [false]
  });

  // Account Deletion State
  showDeleteModal = false;
  isDeletionPending = false;
  deletionDate: string | null = null;
  isDeletingAccount = false;
  isCancellingDeletion = false;

  deleteForm = this.fb.group({
    masterPassword: ['', [Validators.required]],
    confirmation: [false, [Validators.requiredTrue]]
  });

  ngOnInit() {
    this.loadSettings();
    this.loadProfileForDeletionStatus();
  }

  loadSettings() {
    this.settingsService.getSettings().subscribe({
      next: (settings) => {
        this.userSettings = settings;
        this.readOnlyForm.patchValue({
          readOnlyMode: settings.readOnlyMode || false
        });
        this.isLoadingSettings = false;
      },
      error: () => {
        this.errorMessage = 'Failed to load user settings';
        this.isLoadingSettings = false;
      }
    });
  }

  private loadProfileForDeletionStatus() {
    this.userService.getProfile().subscribe({
      next: (profile) => {
        this.userProfile = profile;
        this.checkDeletionStatus();
      },
      error: () => { }
    });
  }

  private checkDeletionStatus() {
    if (this.userProfile && this.userProfile.deletionScheduledAt) {
      this.isDeletionPending = true;
      const date = new Date(this.userProfile.deletionScheduledAt);
      this.deletionDate = date.toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' });
    } else {
      this.isDeletionPending = false;
      this.deletionDate = null;
    }
  }

  onToggleReadOnly() {
    this.errorMessage = '';
    this.successMessage = '';

    const newStatus = this.readOnlyForm.value.readOnlyMode as boolean;

    this.settingsService.updateSettings({ readOnlyMode: newStatus }).subscribe({
      next: (settings) => {
        this.userSettings = settings;
        this.successMessage = 'Read-only mode updated!';
        this.notificationEventService.triggerRefresh();
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to update read-only mode';
        // Revert toggle
        this.readOnlyForm.patchValue({ readOnlyMode: !newStatus });
      }
    });
  }

  onDeleteAccount() {
    this.showDeleteModal = true;
    this.deleteForm.reset();
    this.errorMessage = '';
    this.successMessage = '';
  }

  onCancelDeleteModal() {
    this.showDeleteModal = false;
    this.deleteForm.reset();
  }

  onConfirmDelete() {
    if (this.deleteForm.invalid) return;

    this.isDeletingAccount = true;
    this.errorMessage = '';
    this.successMessage = '';

    const request: AccountDeletionRequest = {
      masterPassword: this.deleteForm.value.masterPassword as string,
      confirmation: true
    };

    this.userService.deleteAccount(request).subscribe({
      next: (response) => {
        this.isDeletingAccount = false;
        this.showDeleteModal = false;
        this.successMessage = response.message || 'Account scheduled for deletion in 30 days.';
        this.loadProfileForDeletionStatus();
        this.notificationEventService.triggerRefresh();
      },
      error: (err) => {
        this.isDeletingAccount = false;
        this.errorMessage = err.error?.message || 'Failed to delete account. Please check your password.';
      }
    });
  }

  onCancelDeletion() {
    this.isCancellingDeletion = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.userService.cancelDeletion().subscribe({
      next: (response) => {
        this.isCancellingDeletion = false;
        this.successMessage = response.message || 'Account deletion cancelled.';
        this.loadProfileForDeletionStatus();
        this.notificationEventService.triggerRefresh();
      },
      error: (err) => {
        this.isCancellingDeletion = false;
        this.errorMessage = err.error?.message || 'Failed to cancel deletion.';
      }
    });
  }
}
