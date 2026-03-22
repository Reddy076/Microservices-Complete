import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { importProvidersFrom } from '@angular/core';
import { ImportComponent, ImportStep } from './import.component';
import { BackupControllerService } from '../../../core/api/api/backupController.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';
import { ImportResult } from '../../../core/api/model/importResult';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { By } from '@angular/platform-browser';
import { LucideAngularModule, UploadCloud, CheckCircle, AlertTriangle, FileJson, Download, Vault, HelpCircle, Info, AlertCircle } from 'lucide-angular';
import { provideRouter } from '@angular/router';

describe('ImportComponent', () => {
  let component: ImportComponent;
  let fixture: ComponentFixture<ImportComponent>;
  let backupService: jasmine.SpyObj<BackupControllerService>;
  let snackBar: jasmine.SpyObj<MatSnackBar>;

  const mockImportResult: ImportResult = {
    totalProcessed: 10,
    successCount: 8,
    failCount: 2,
    message: 'Import completed with some errors'
  };

  beforeEach(waitForAsync(() => {
    const backupServiceSpy = jasmine.createSpyObj('BackupControllerService', ['importVault']);
    const snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

    TestBed.configureTestingModule({
      imports: [
        ImportComponent,
        NoopAnimationsModule,
        MatCardModule,
        MatButtonModule,
        MatProgressSpinnerModule
      ],
      providers: [
        provideRouter([]),
        { provide: BackupControllerService, useValue: backupServiceSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
        importProvidersFrom(LucideAngularModule.pick({
          UploadCloud, CheckCircle, AlertTriangle, FileJson, Download, Vault, HelpCircle, Info, AlertCircle
        }))
      ]
    })
      .overrideComponent(ImportComponent, {
        remove: { imports: [MatSnackBarModule] }
      })
      .compileComponents();

    backupService = TestBed.inject(BackupControllerService) as jasmine.SpyObj<BackupControllerService>;
    snackBar = TestBed.inject(MatSnackBar) as jasmine.SpyObj<MatSnackBar>;
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ImportComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Initial State', () => {
    it('should start in SELECT_FILE step', () => {
      expect(component.currentStep).toBe(ImportStep.SELECT_FILE);
    });

    it('should have no selected file initially', () => {
      expect(component.selectedFile).toBeNull();
    });

    it('should not be dragging initially', () => {
      expect(component.isDragging).toBeFalse();
    });
  });

  describe('File Validation', () => {
    it('should reject non-JSON files immediately', () => {
      const file = new File(['content'], 'test.txt', { type: 'text/plain' });

      component.handleFileSelection(file);

      expect(snackBar.open).toHaveBeenCalledWith(
        'Please select a valid JSON or encrypted export file',
        'Dismiss',
        { duration: 5000, panelClass: 'error-snackbar' }
      );
      expect(component.selectedFile).toBeNull();
    });

    it('should reject files larger than 10MB immediately', () => {
      const largeContent = 'x'.repeat(11 * 1024 * 1024);
      const file = new File([largeContent], 'large.json', { type: 'application/json' });

      component.handleFileSelection(file);

      expect(snackBar.open).toHaveBeenCalledWith(
        jasmine.stringContaining('File size exceeds'),
        'Dismiss',
        { duration: 5000, panelClass: 'error-snackbar' }
      );
      expect(component.selectedFile).toBeNull();
    });

    it('should accept valid JSON file and move to validating step', () => {
      const file = new File(['[{ "title": "Test" }]'], 'test.json', { type: 'application/json' });

      component.handleFileSelection(file);

      expect(component.currentStep).toBe(ImportStep.VALIDATING);
      expect(component.selectedFile).toBe(file);
    });
  });

  describe('Drag and Drop', () => {
    it('should set isDragging on drag over', () => {
      const event = new DragEvent('dragover');
      spyOn(event, 'preventDefault');
      spyOn(event, 'stopPropagation');

      component.onDragOver(event);

      expect(component.isDragging).toBeTrue();
      expect(event.preventDefault).toHaveBeenCalled();
    });

    it('should clear isDragging on drag leave', () => {
      const event = new DragEvent('dragleave');
      spyOn(event, 'preventDefault');
      spyOn(event, 'stopPropagation');

      component.isDragging = true;
      component.onDragLeave(event);

      expect(component.isDragging).toBeFalse();
    });

    it('should handle file drop', () => {
      const file = new File(['[{ "title": "Test" }]'], 'test.json', { type: 'application/json' });
      const dataTransfer = new DataTransfer();
      dataTransfer.items.add(file);
      const event = new DragEvent('drop', { dataTransfer });
      spyOn(event, 'preventDefault');
      spyOn(event, 'stopPropagation');

      component.onDrop(event);

      expect(event.preventDefault).toHaveBeenCalled();
    });
  });

  describe('Import Process', () => {
    beforeEach(() => {
      component.currentStep = ImportStep.PREVIEW;
      component.fileContent = '[{ "title": "Test" }]';
      component.selectedFile = new File(['[{ "title": "Test" }]'], 'test.json', { type: 'application/json' });
      component.validationResult = {
        isValid: true,
        entriesFound: 1,
        validEntries: 1,
        invalidEntries: 0
      };
    });

    it('should call importVault when onImport is called', () => {
      backupService.importVault.and.returnValue(of(mockImportResult) as any);

      component.onImport();

      expect(backupService.importVault).toHaveBeenCalledWith({
        data: component.fileContent,
        format: 'JSON'
      });
    });

    it('should transition to RESULTS step on successful import', () => {
      backupService.importVault.and.returnValue(of(mockImportResult) as any);

      component.onImport();

      expect(component.currentStep).toBe(ImportStep.RESULTS);
      expect(component.importResult).toEqual(mockImportResult);
    });

    it('should show success message on import completion', () => {
      backupService.importVault.and.returnValue(of(mockImportResult) as any);

      component.onImport();

      expect(snackBar.open).toHaveBeenCalledWith(
        'Successfully imported 8 entries',
        'Dismiss',
        { duration: 5000, panelClass: 'success-snackbar' }
      );
    });

    it('should handle import error with message', () => {
      const error = { error: { message: 'Import failed' } };
      backupService.importVault.and.returnValue(throwError(() => error));

      component.onImport();

      expect(component.currentStep).toBe(ImportStep.PREVIEW);
      expect(snackBar.open).toHaveBeenCalledWith(
        'Import failed',
        'Dismiss',
        { duration: 5000, panelClass: 'error-snackbar' }
      );
    });

    it('should show default error message when no error message provided', () => {
      backupService.importVault.and.returnValue(throwError(() => ({})));

      component.onImport();

      expect(snackBar.open).toHaveBeenCalledWith(
        'Import failed. Please try again.',
        'Dismiss',
        { duration: 5000, panelClass: 'error-snackbar' }
      );
    });
  });

  describe('Reset and Cancel', () => {
    beforeEach(() => {
      component.currentStep = ImportStep.PREVIEW;
      component.selectedFile = new File(['content'], 'test.json', { type: 'application/json' });
      component.fileContent = 'test content';
      component.validationResult = { isValid: true, entriesFound: 1, validEntries: 1, invalidEntries: 0 };
    });

    it('should reset state on cancel', () => {
      component.onCancel();

      expect(component.currentStep).toBe(ImportStep.SELECT_FILE);
      expect(component.selectedFile).toBeNull();
      expect(component.fileContent).toBe('');
      expect(component.validationResult).toBeNull();
    });

    it('should reset state on import another', () => {
      component.currentStep = ImportStep.RESULTS;
      component.importResult = mockImportResult;

      component.onImportAnother();

      expect(component.currentStep).toBe(ImportStep.SELECT_FILE);
      expect(component.selectedFile).toBeNull();
      expect(component.fileContent).toBe('');
      expect(component.importResult).toBeNull();
    });
  });

  describe('Utility Methods', () => {
    it('should format file size correctly', () => {
      expect(component.formatFileSize(0)).toBe('0 Bytes');
      expect(component.formatFileSize(1024)).toBe('1 KB');
      expect(component.formatFileSize(1024 * 1024)).toBe('1 MB');
      expect(component.formatFileSize(1024 * 1024 * 1024)).toBe('1 GB');
    });

    it('should return file size for selected file', () => {
      const file = new File(['content'], 'test.json', { type: 'application/json' });
      component.selectedFile = file;

      expect(component.fileSize).toBe(component.formatFileSize(file.size));
    });

    it('should return empty string when no file selected', () => {
      component.selectedFile = null;

      expect(component.fileSize).toBe('');
    });
  });

  describe('Template Rendering', () => {
    it('should show drop zone in SELECT_FILE step', () => {
      component.currentStep = ImportStep.SELECT_FILE;
      fixture.detectChanges();

      const dropZone = fixture.debugElement.query(By.css('.drop-zone'));
      expect(dropZone).toBeTruthy();
    });

    it('should show progress spinner in VALIDATING step', () => {
      component.currentStep = ImportStep.VALIDATING;
      fixture.detectChanges();

      const progressContainer = fixture.debugElement.query(By.css('.progress-container'));
      expect(progressContainer).toBeTruthy();
    });

    it('should show preview in PREVIEW step', () => {
      component.currentStep = ImportStep.PREVIEW;
      component.selectedFile = new File(['content'], 'test.json', { type: 'application/json' });
      component.validationResult = {
        isValid: true,
        entriesFound: 5,
        validEntries: 4,
        invalidEntries: 1
      };
      fixture.detectChanges();

      const previewStats = fixture.debugElement.query(By.css('.preview-stats'));
      expect(previewStats).toBeTruthy();
    });

    it('should show results in RESULTS step', () => {
      component.currentStep = ImportStep.RESULTS;
      component.importResult = mockImportResult;
      fixture.detectChanges();

      const resultsHeader = fixture.debugElement.query(By.css('.results-header'));
      expect(resultsHeader).toBeTruthy();
    });

    it('should show importing spinner in IMPORTING step', () => {
      component.currentStep = ImportStep.IMPORTING;
      fixture.detectChanges();

      const progressContainer = fixture.debugElement.query(By.css('.progress-container'));
      expect(progressContainer).toBeTruthy();
    });
  });
});
