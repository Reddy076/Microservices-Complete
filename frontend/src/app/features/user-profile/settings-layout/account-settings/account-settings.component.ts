import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { UserControllerService, UserResponse, ChangePasswordRequest } from '../../../../core/api';
import { CommonModule } from '@angular/common';
import { NotificationEventService } from '../../../../core/services/notification-event.service';
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-account-settings',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule, LucideAngularModule],
  templateUrl: './account-settings.component.html',
  styleUrl: './account-settings.component.css'
})
export class AccountSettingsComponent implements OnInit {
  private userService = inject(UserControllerService);
  private notificationEventService = inject(NotificationEventService);
  private fb = inject(FormBuilder);

  userProfile: UserResponse | null = null;
  isLoadingProfile = true;
  isEditingProfile = false;
  successMessage = '';
  errorMessage = '';

  profileForm = this.fb.group({
    name: ['', [Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
    phoneNumber: ['', [Validators.maxLength(20)]]
  });

  passwordForm = this.fb.group({
    currentPassword: ['', [Validators.required]],
    newPassword: ['', [Validators.required, Validators.minLength(8)]]
  });

  ngOnInit() {
    this.loadProfile();
  }

  loadProfile() {
    this.userService.getProfile().subscribe({
      next: (profile) => {
        this.userProfile = profile;
        this.profileForm.patchValue({
          name: profile.name || '',
          email: profile.email || '',
          phoneNumber: profile.phoneNumber || ''
        });
        this.isLoadingProfile = false;
      },
      error: () => {
        this.errorMessage = 'Failed to load user profile';
        this.isLoadingProfile = false;
      }
    });
  }

  onEditProfile() {
    this.isEditingProfile = true;
    this.errorMessage = '';
    this.successMessage = '';
  }

  onCancelEditProfile() {
    this.isEditingProfile = false;
    if (this.userProfile) {
      this.profileForm.patchValue({
        name: this.userProfile.name || '',
        email: this.userProfile.email || '',
        phoneNumber: this.userProfile.phoneNumber || ''
      });
    }
    this.errorMessage = '';
    this.successMessage = '';
  }

  onUpdateProfileSubmit() {
    if (this.profileForm.invalid) return;

    this.errorMessage = '';
    this.successMessage = '';

    const req = {
      name: this.profileForm.value.name as string || undefined,
      email: this.profileForm.value.email as string,
      phoneNumber: this.profileForm.value.phoneNumber as string || undefined
    };

    this.userService.updateProfile(req).subscribe({
      next: (updatedProfile) => {
        this.userProfile = updatedProfile;
        this.isEditingProfile = false;
        this.successMessage = 'Profile updated successfully!';
        this.notificationEventService.triggerRefresh();
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to update profile. Email might be in use.';
      }
    });
  }

  onChangePasswordSubmit() {
    if (this.passwordForm.invalid) return;

    this.errorMessage = '';
    this.successMessage = '';

    const req: ChangePasswordRequest = {
      oldPassword: this.passwordForm.value.currentPassword as string,
      newPassword: this.passwordForm.value.newPassword as string
    };

    this.userService.changePassword(req).subscribe({
      next: () => {
        this.successMessage = 'Password updated successfully!';
        this.passwordForm.reset();
        this.notificationEventService.triggerRefresh();
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to update password';
      }
    });
  }
}
