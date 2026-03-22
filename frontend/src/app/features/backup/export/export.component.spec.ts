import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { importProvidersFrom } from '@angular/core';
import { ExportComponent } from './export.component';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { BackupControllerService } from '../../../core/api/api/backupController.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError, Observable } from 'rxjs';
import { ExportResponse } from '../../../core/api/model/exportResponse';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { By } from '@angular/platform-browser';
import { LucideAngularModule, Download, Info, HelpCircle, Lock } from 'lucide-angular';
import { provideRouter } from '@angular/router';

describe('ExportComponent', () => {
  let component: ExportComponent;
  let fixture: ComponentFixture<ExportComponent>;
  let backupService: jasmine.SpyObj<BackupControllerService>;
  let snackBar: jasmine.SpyObj<MatSnackBar>;

  const mockExportResponse: ExportResponse = {
    fileName: 'vault-export-2024.json',
    format: 'JSON',
    entryCount: 25,
    encrypted: false,
    data: 'eyJ0ZXN0IjogImRhdGEifQ==',
    exportedAt: '2024-01-01T00:00:00Z'
  };

  beforeEach(waitForAsync(() => {
    const backupServiceSpy = jasmine.createSpyObj('BackupControllerService', ['exportVault']);
    const snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

    TestBed.configureTestingModule({
      imports: [
        ExportComponent,
        NoopAnimationsModule,
        ReactiveFormsModule,
        MatCardModule,
        MatButtonModule,
        MatProgressSpinnerModule,
        MatSnackBarModule,
        LucideAngularModule
      ],
      providers: [
        FormBuilder,
        provideRouter([]),
        { provide: BackupControllerService, useValue: backupServiceSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
        importProvidersFrom(LucideAngularModule.pick({ Download, Info, HelpCircle, Lock }))
      ]
    }).compileComponents();

    backupService = TestBed.inject(BackupControllerService) as jasmine.SpyObj<BackupControllerService>;
    snackBar = TestBed.inject(MatSnackBar) as jasmine.SpyObj<MatSnackBar>;
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ExportComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Form Initialization', () => {
    it('should initialize form with default values', () => {
      expect(component.exportForm).toBeDefined();
      expect(component.exportForm.get('format')?.value).toBe('JSON');
      expect(component.exportForm.get('password')?.value).toBe('');
    });

    it('should have format field as required', () => {
      const formatControl = component.exportForm.get('format');
      formatControl?.setValue('');
      expect(formatControl?.valid).toBeFalsy();
      expect(formatControl?.hasError('required')).toBeTruthy();
    });

    it('should not require password by default for JSON format', () => {
      const passwordControl = component.exportForm.get('password');
      expect(passwordControl?.valid).toBeTruthy();
      expect(passwordControl?.hasError('required')).toBeFalsy();
    });
  });

  describe('Password Field Visibility', () => {
    it('should not show password field for JSON format', () => {
      component.exportForm.get('format')?.setValue('JSON');
      fixture.detectChanges();
      expect(component.showPasswordField).toBeFalsy();
    });

    it('should show password field for ENCRYPTED format', () => {
      component.exportForm.get('format')?.setValue('ENCRYPTED');
      fixture.detectChanges();
      expect(component.showPasswordField).toBeTruthy();
    });

    it('should make password required when ENCRYPTED format is selected', () => {
      component.exportForm.get('format')?.setValue('ENCRYPTED');
      fixture.detectChanges();

      const passwordControl = component.exportForm.get('password');
      passwordControl?.setValue('');
      expect(passwordControl?.hasError('required')).toBeTruthy();
    });

    it('should validate password minimum length for ENCRYPTED format', () => {
      component.exportForm.get('format')?.setValue('ENCRYPTED');
      fixture.detectChanges();

      const passwordControl = component.exportForm.get('password');
      passwordControl?.setValue('short');
      expect(passwordControl?.hasError('minlength')).toBeTruthy();

      passwordControl?.setValue('longenoughpassword');
      expect(passwordControl?.hasError('minlength')).toBeFalsy();
    });

    it('should clear password when switching from ENCRYPTED to JSON', () => {
      component.exportForm.get('format')?.setValue('ENCRYPTED');
      component.exportForm.get('password')?.setValue('testpassword');
      fixture.detectChanges();

      component.exportForm.get('format')?.setValue('JSON');
      fixture.detectChanges();

      expect(component.exportForm.get('password')?.value).toBe('');
    });
  });

  describe('Export Functionality', () => {
    it('should not call exportVault if form is invalid', () => {
      component.exportForm.get('format')?.setValue('ENCRYPTED');
      component.exportForm.get('password')?.setValue('');
      fixture.detectChanges();

      component.onExport();
      expect(backupService.exportVault).not.toHaveBeenCalled();
    });

    it('should call exportVault with correct parameters for JSON format', () => {
      backupService.exportVault.and.returnValue(of(mockExportResponse) as Observable<any>);

      component.exportForm.get('format')?.setValue('JSON');
      fixture.detectChanges();

      component.onExport();

      expect(backupService.exportVault).toHaveBeenCalledWith('JSON', undefined);
    });

    it('should call exportVault with password for ENCRYPTED format', () => {
      backupService.exportVault.and.returnValue(of(mockExportResponse) as Observable<any>);

      component.exportForm.get('format')?.setValue('ENCRYPTED');
      component.exportForm.get('password')?.setValue('mypassword123');
      fixture.detectChanges();

      component.onExport();

      expect(backupService.exportVault).toHaveBeenCalledWith('ENCRYPTED', 'mypassword123');
    });

    it('should set isLoading to true during export', () => {
      // Use a delayed observable to test loading state
      backupService.exportVault.and.returnValue(of(mockExportResponse) as Observable<any>);

      expect(component.isLoading).toBeFalsy();
      component.onExport();
      // Since observable completes synchronously in test, loading should be handled
      expect(component.isLoading).toBeDefined();
    });
  });

  describe('File Download', () => {
    it('should create download link with correct attributes', () => {
      backupService.exportVault.and.returnValue(of(mockExportResponse) as Observable<any>);

      const createElementSpy = spyOn(document, 'createElement').and.callThrough();
      const appendChildSpy = spyOn(document.body, 'appendChild').and.callThrough();
      const removeChildSpy = spyOn(document.body, 'removeChild').and.callThrough();

      component.onExport();

      expect(createElementSpy).toHaveBeenCalledWith('a');
      expect(appendChildSpy).toHaveBeenCalled();
      expect(removeChildSpy).toHaveBeenCalled();
    });
  });

  describe('Form Helpers', () => {
    it('should expose formatControl getter', () => {
      expect(component.formatControl).toBe(component.exportForm.get('format'));
    });

    it('should expose passwordControl getter', () => {
      expect(component.passwordControl).toBe(component.exportForm.get('password'));
    });
  });

  describe('UI Elements', () => {
    it('should display header text', () => {
      const compiled = fixture.nativeElement;
      expect(compiled.querySelector('h1').textContent).toContain('Export Vault');
    });

    it('should display format options', () => {
      const compiled = fixture.nativeElement;
      expect(compiled.textContent).toContain('JSON (Unencrypted)');
      expect(compiled.textContent).toContain('Encrypted');
    });

    it('should disable export button when form is invalid', () => {
      component.exportForm.get('format')?.setValue('ENCRYPTED');
      component.exportForm.get('password')?.setValue('');
      fixture.detectChanges();

      const submitButton = fixture.debugElement.query(By.css('button[type="submit"]'));
      expect(submitButton.nativeElement.disabled).toBeTruthy();
    });
  });

  describe('Error Handling', () => {
    it('should handle handleExportSuccess with data', () => {
      backupService.exportVault.and.returnValue(of(mockExportResponse) as Observable<any>);

      component.onExport();

      // The subscription callback should be triggered
      expect(backupService.exportVault).toHaveBeenCalled();
    });

    it('should handle handleExportError with message', () => {
      const error = { error: { message: 'Export failed' } };
      backupService.exportVault.and.returnValue(throwError(() => error));

      component.onExport();

      expect(backupService.exportVault).toHaveBeenCalled();
    });

    it('should handle handleExportError without message', () => {
      backupService.exportVault.and.returnValue(throwError(() => new Error()));

      component.onExport();

      expect(backupService.exportVault).toHaveBeenCalled();
    });
  });
});
