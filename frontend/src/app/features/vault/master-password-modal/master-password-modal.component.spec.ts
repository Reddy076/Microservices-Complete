import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MasterPasswordModalComponent } from './master-password-modal.component';
import { VaultService } from '../../../core/api/api/vault.service';
import { of, throwError } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { VaultEntryDetailResponse } from '../../../core/api/model/vaultEntryDetailResponse';
import { importProvidersFrom } from '@angular/core';
import { LucideAngularModule, Lock, Eye, EyeOff, X, ShieldCheck, AlertTriangle } from 'lucide-angular';

describe('MasterPasswordModalComponent', () => {
  let component: MasterPasswordModalComponent;
  let fixture: ComponentFixture<MasterPasswordModalComponent>;
  let mockVaultService: jasmine.SpyObj<VaultService>;

  beforeEach(async () => {
    mockVaultService = jasmine.createSpyObj('VaultService', ['accessSensitiveEntry']);

    await TestBed.configureTestingModule({
      imports: [MasterPasswordModalComponent, FormsModule],
      providers: [
        importProvidersFrom(LucideAngularModule.pick({ Lock, Eye, EyeOff, X, ShieldCheck, AlertTriangle })),
        { provide: VaultService, useValue: mockVaultService }
      ]
    })
      .compileComponents();

    fixture = TestBed.createComponent(MasterPasswordModalComponent);
    component = fixture.componentInstance;
    component.entryId = 1;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should emit passwordDecrypted on successful verification', () => {
    const mockDetail: VaultEntryDetailResponse = { password: 'decryptedPassword123' };
    mockVaultService.accessSensitiveEntry.and.returnValue(of(mockDetail) as any);
    spyOn(component.passwordDecrypted, 'emit');

    component.masterPassword = 'correctPassword';
    component.onSubmit();

    expect(mockVaultService.accessSensitiveEntry).toHaveBeenCalledWith(1, { masterPassword: 'correctPassword' });
    expect(component.passwordDecrypted.emit).toHaveBeenCalledWith('decryptedPassword123');
    expect(component.errorMessage).toBe('');
  });

  it('should handle verification errors and set errorMessage', () => {
    const errorResponse = new HttpErrorResponse({ status: 401, error: { message: 'Invalid OTP' } });
    mockVaultService.accessSensitiveEntry.and.returnValue(throwError(() => errorResponse));

    component.masterPassword = 'wrongPassword';
    component.onSubmit();

    expect(mockVaultService.accessSensitiveEntry).toHaveBeenCalled();
    expect(component.errorMessage).toBe('Invalid Authenticator Code. Please try again.');
    expect(component.showOtpField).toBeTrue();
  });

  it('should emit modalCancel closely', () => {
    spyOn(component.modalCancel, 'emit');
    component.onCancel();
    expect(component.modalCancel.emit).toHaveBeenCalled();
  });
});
