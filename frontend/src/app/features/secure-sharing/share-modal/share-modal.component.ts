import { Component, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { SecureSharingService } from '../../../core/api/api/secureSharing.service';
import { CreateShareRequest } from '../../../core/api/model/createShareRequest';
import { ShareLinkResponse } from '../../../core/api/model/shareLinkResponse';

@Component({
  selector: 'app-share-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, LucideAngularModule],
  templateUrl: './share-modal.component.html',
  styleUrl: './share-modal.component.css'
})
export class ShareModalComponent implements OnInit {
  private fb = inject(FormBuilder);
  private sharingService = inject(SecureSharingService);

  @Input() entryId!: number;
  @Input() entryTitle: string = '';

  @Output() shareCreated = new EventEmitter<ShareLinkResponse>();
  @Output() cancelled = new EventEmitter<void>();

  shareForm!: FormGroup;
  isSubmitting = false;
  errorMessage: string | null = null;

  // Expiry options in hours
  expiryPresets = [
    { label: '1h', value: 1 },
    { label: '24h', value: 24 },
    { label: '7d', value: 168 },
    { label: 'Custom', value: null }
  ];
  selectedExpiryPreset: number | null = 24;

  // Max views options
  viewsPresets = [
    { label: '1', value: 1 },
    { label: '5', value: 5 },
    { label: '10', value: 10 },
    { label: 'Unlimited', value: 100 }
  ];
  selectedViewsPreset: number | null = 1;

  // Permission options
  permissionOptions = [
    { value: 'VIEW_ONCE', label: 'View Once', description: 'Password can be viewed one time only' },
    { value: 'VIEW_MULTIPLE', label: 'View Multiple', description: 'Password can be viewed multiple times until expiry' },
    { value: 'TEMPORARY_ACCESS', label: 'Temporary Access', description: 'Full temporary access to the entry' }
  ];

  ngOnInit(): void {
    this.initForm();
  }

  private initForm(): void {
    this.shareForm = this.fb.group({
      recipientEmail: ['', [Validators.email]],
      expiryHours: [24, [Validators.required, Validators.min(1), Validators.max(168)]],
      maxViews: [1, [Validators.required, Validators.min(1), Validators.max(100)]],
      permission: ['VIEW_ONCE', Validators.required]
    });
  }

  // Expiry slider handler
  onExpirySliderChange(event: Event): void {
    const value = parseInt((event.target as HTMLInputElement).value, 10);
    this.shareForm.patchValue({ expiryHours: value });
    this.selectedExpiryPreset = null; // Clear preset when manually adjusted
  }

  onExpiryPresetClick(preset: { label: string; value: number | null }): void {
    if (preset.value !== null) {
      this.shareForm.patchValue({ expiryHours: preset.value });
      this.selectedExpiryPreset = preset.value;
    }
  }

  get expiryHoursDisplay(): string {
    const hours = this.shareForm?.get('expiryHours')?.value || 24;
    if (hours < 24) {
      return `${hours} hour${hours > 1 ? 's' : ''}`;
    } else if (hours === 24) {
      return '1 day';
    } else {
      const days = Math.floor(hours / 24);
      const remainingHours = hours % 24;
      if (remainingHours === 0) {
        return `${days} day${days > 1 ? 's' : ''}`;
      }
      return `${days}d ${remainingHours}h`;
    }
  }

  // Max views slider handler
  onViewsSliderChange(event: Event): void {
    const value = parseInt((event.target as HTMLInputElement).value, 10);
    this.shareForm.patchValue({ maxViews: value });
    this.selectedViewsPreset = null;
  }

  onViewsPresetClick(preset: { label: string; value: number | null }): void {
    if (preset.value !== null) {
      this.shareForm.patchValue({ maxViews: preset.value });
      this.selectedViewsPreset = preset.value;
    }
  }

  get maxViewsDisplay(): string {
    const views = this.shareForm?.get('maxViews')?.value || 1;
    if (views >= 100) {
      return 'Unlimited';
    }
    return `${views} view${views > 1 ? 's' : ''}`;
  }

  onSubmit(): void {
    if (this.shareForm.invalid || this.isSubmitting) {
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = null;

    const request: CreateShareRequest = {
      vaultEntryId: this.entryId,
      recipientEmail: this.shareForm.value.recipientEmail || undefined,
      expiryHours: this.shareForm.value.expiryHours,
      maxViews: this.shareForm.value.maxViews,
      permission: this.shareForm.value.permission
    };

    this.sharingService.createShare(request).subscribe({
      next: (response) => {
        this.isSubmitting = false;
        this.shareCreated.emit(response);
      },
      error: (error) => {
        this.isSubmitting = false;
        this.errorMessage = error.error?.message || 'Failed to create share. Please try again.';
      }
    });
  }

  onCancel(): void {
    this.cancelled.emit();
  }

  get recipientEmailInvalid(): boolean {
    const control = this.shareForm.get('recipientEmail');
    return control?.invalid && control?.touched || false;
  }

  get selectedPermissionDescription(): string {
    const permission = this.shareForm?.get('permission')?.value;
    const option = this.permissionOptions.find(o => o.value === permission);
    return option?.description || '';
  }
}
