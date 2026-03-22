import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ShareModalComponent } from './share-modal.component';
import { ReactiveFormsModule } from '@angular/forms';
import { LucideAngularModule, Share2, X, Mail, Clock, Eye, Shield, Settings, Link, Loader2, AlertCircle, Info, ChevronDown, Lock } from 'lucide-angular';
import { SecureSharingService } from '../../../core/api/api/secureSharing.service';
import { of, throwError, Observable } from 'rxjs';
import { ShareLinkResponse } from '../../../core/api/model/shareLinkResponse';
import { importProvidersFrom } from '@angular/core';

describe('ShareModalComponent', () => {
  let component: ShareModalComponent;
  let fixture: ComponentFixture<ShareModalComponent>;
  let mockSharingService: jasmine.SpyObj<SecureSharingService>;

  beforeEach(async () => {
    mockSharingService = jasmine.createSpyObj('SecureSharingService', ['createShare']);

    await TestBed.configureTestingModule({
      imports: [
        ShareModalComponent,
        ReactiveFormsModule,
        LucideAngularModule
      ],
      providers: [
        { provide: SecureSharingService, useValue: mockSharingService },
        importProvidersFrom(LucideAngularModule.pick({
          Share2, X, Mail, Clock, Eye, Shield, Settings, Link, Loader2, AlertCircle, Info, ChevronDown, Lock
        }))
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ShareModalComponent);
    component = fixture.componentInstance;
    component.entryId = 123;
    component.entryTitle = 'Test Entry';
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Form Initialization', () => {
    it('should initialize form with default values', () => {
      expect(component.shareForm.get('recipientEmail')?.value).toBe('');
      expect(component.shareForm.get('expiryHours')?.value).toBe(24);
      expect(component.shareForm.get('maxViews')?.value).toBe(1);
      expect(component.shareForm.get('permission')?.value).toBe('VIEW_ONCE');
    });

    it('should mark form as valid with default values', () => {
      expect(component.shareForm.valid).toBeTrue();
    });
  });

  describe('Email Validation', () => {
    it('should accept empty email (optional field)', () => {
      component.shareForm.patchValue({ recipientEmail: '' });
      expect(component.shareForm.get('recipientEmail')?.valid).toBeTrue();
    });

    it('should validate email format', () => {
      component.shareForm.patchValue({ recipientEmail: 'invalid-email' });
      expect(component.shareForm.get('recipientEmail')?.invalid).toBeTrue();

      component.shareForm.patchValue({ recipientEmail: 'test@example.com' });
      expect(component.shareForm.get('recipientEmail')?.valid).toBeTrue();
    });

    it('should show recipientEmailInvalid when email is invalid and touched', () => {
      component.shareForm.patchValue({ recipientEmail: 'invalid' });
      component.shareForm.get('recipientEmail')?.markAsTouched();
      expect(component.recipientEmailInvalid).toBeTrue();
    });
  });

  describe('Expiry Slider', () => {
    it('should update expiryHours on slider change', () => {
      const mockEvent = { target: { value: '48' } } as unknown as Event;
      component.onExpirySliderChange(mockEvent);
      expect(component.shareForm.get('expiryHours')?.value).toBe(48);
      expect(component.selectedExpiryPreset).toBeNull();
    });

    it('should update expiryHours and preset on preset click', () => {
      component.onExpiryPresetClick({ label: '7d', value: 168 });
      expect(component.shareForm.get('expiryHours')?.value).toBe(168);
      expect(component.selectedExpiryPreset).toBe(168);
    });

    it('should format expiry display correctly for hours', () => {
      component.shareForm.patchValue({ expiryHours: 5 });
      expect(component.expiryHoursDisplay).toBe('5 hours');

      component.shareForm.patchValue({ expiryHours: 1 });
      expect(component.expiryHoursDisplay).toBe('1 hour');
    });

    it('should format expiry display correctly for days', () => {
      component.shareForm.patchValue({ expiryHours: 24 });
      expect(component.expiryHoursDisplay).toBe('1 day');

      component.shareForm.patchValue({ expiryHours: 48 });
      expect(component.expiryHoursDisplay).toBe('2 days');
    });

    it('should format expiry display correctly for mixed days and hours', () => {
      component.shareForm.patchValue({ expiryHours: 36 });
      expect(component.expiryHoursDisplay).toBe('1d 12h');
    });
  });

  describe('Max Views Slider', () => {
    it('should update maxViews on slider change', () => {
      const mockEvent = { target: { value: '10' } } as unknown as Event;
      component.onViewsSliderChange(mockEvent);
      expect(component.shareForm.get('maxViews')?.value).toBe(10);
      expect(component.selectedViewsPreset).toBeNull();
    });

    it('should update maxViews and preset on preset click', () => {
      component.onViewsPresetClick({ label: '5', value: 5 });
      expect(component.shareForm.get('maxViews')?.value).toBe(5);
      expect(component.selectedViewsPreset).toBe(5);
    });

    it('should format max views display correctly', () => {
      component.shareForm.patchValue({ maxViews: 1 });
      expect(component.maxViewsDisplay).toBe('1 view');

      component.shareForm.patchValue({ maxViews: 5 });
      expect(component.maxViewsDisplay).toBe('5 views');
    });

    it('should display Unlimited for 100+ views', () => {
      component.shareForm.patchValue({ maxViews: 100 });
      expect(component.maxViewsDisplay).toBe('Unlimited');
    });
  });

  describe('Form Submission', () => {
    it('should not submit if form is invalid', () => {
      component.shareForm.patchValue({ recipientEmail: 'invalid-email' });
      component.shareForm.get('recipientEmail')?.markAsTouched();
      component.onSubmit();
      expect(mockSharingService.createShare).not.toHaveBeenCalled();
    });

    it('should not submit while already submitting', () => {
      component.isSubmitting = true;
      component.onSubmit();
      expect(mockSharingService.createShare).not.toHaveBeenCalled();
    });

    it('should call createShare with correct parameters', () => {
      const mockResponse: ShareLinkResponse = {
        shareId: 1,
        shareToken: 'test-token',
        shareUrl: 'https://example.com/share/test-token',
        encryptionKey: 'test-key',
        vaultEntryTitle: 'Test Entry'
      };
      mockSharingService.createShare.and.returnValue(of(mockResponse) as any);

      component.shareForm.patchValue({
        recipientEmail: 'test@example.com',
        expiryHours: 48,
        maxViews: 5,
        permission: 'VIEW_MULTIPLE'
      });

      component.onSubmit();

      expect(mockSharingService.createShare).toHaveBeenCalledWith({
        vaultEntryId: 123,
        recipientEmail: 'test@example.com',
        expiryHours: 48,
        maxViews: 5,
        permission: 'VIEW_MULTIPLE'
      });
    });

    it('should emit shareCreated on successful share creation', () => {
      const mockResponse: ShareLinkResponse = {
        shareId: 1,
        shareToken: 'test-token',
        shareUrl: 'https://example.com/share/test-token',
        encryptionKey: 'test-key'
      };
      mockSharingService.createShare.and.returnValue(of(mockResponse) as any);
      spyOn(component.shareCreated, 'emit');

      component.onSubmit();

      expect(component.shareCreated.emit).toHaveBeenCalledWith(mockResponse);
      expect(component.isSubmitting).toBeFalse();
    });

    it('should handle API error and display error message', () => {
      const errorResponse = { error: { message: 'Failed to create share' } };
      mockSharingService.createShare.and.returnValue(throwError(() => errorResponse) as any);

      component.onSubmit();

      expect(component.errorMessage).toBe('Failed to create share');
      expect(component.isSubmitting).toBeFalse();
    });

    it('should display generic error message when no error message provided', () => {
      mockSharingService.createShare.and.returnValue(throwError(() => ({})) as any);

      component.onSubmit();

      expect(component.errorMessage).toBe('Failed to create share. Please try again.');
    });

    it('should omit recipientEmail from request if empty', () => {
      mockSharingService.createShare.and.returnValue(of({}) as any);

      component.shareForm.patchValue({ recipientEmail: '' });
      component.onSubmit();

      const callArgs = mockSharingService.createShare.calls.mostRecent().args[0];
      expect(callArgs.recipientEmail).toBeUndefined();
    });
  });

  describe('Cancel', () => {
    it('should emit cancelled event when onCancel is called', () => {
      spyOn(component.cancelled, 'emit');
      component.onCancel();
      expect(component.cancelled.emit).toHaveBeenCalled();
    });
  });
});
