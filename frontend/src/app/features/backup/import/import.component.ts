import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { BackupControllerService } from '../../../core/api/api/backupController.service';
import { ImportResult } from '../../../core/api/model/importResult';
import { finalize } from 'rxjs/operators';

export enum ImportStep {
  SELECT_FILE = 'select',
  VALIDATING = 'validating',
  PREVIEW = 'preview',
  IMPORTING = 'importing',
  RESULTS = 'results'
}

interface ValidationResult {
  isValid: boolean;
  entriesFound: number;
  validEntries: number;
  invalidEntries: number;
  errorMessage?: string;
}

@Component({
  selector: 'app-import',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    LucideAngularModule
  ],
  templateUrl: './import.component.html',
  styleUrls: ['./import.component.css']
})
export class ImportComponent {
  readonly ImportStep = ImportStep;
  readonly MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

  currentStep: ImportStep = ImportStep.SELECT_FILE;
  selectedFile: File | null = null;
  fileContent: string = '';
  isDragging = false;
  isLoading = false;
  isEncryptedFile = false;
  decryptionPassword = '';

  // Validation results
  validationResult: ValidationResult | null = null;

  // Import results
  importResult: ImportResult | null = null;

  constructor(
    private backupService: BackupControllerService,
    private snackBar: MatSnackBar
  ) { }

  // Drag and drop handlers
  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;

    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      this.handleFileSelection(files[0]);
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.handleFileSelection(input.files[0]);
    }
  }

  handleFileSelection(file: File): void {
    // Reset state
    this.selectedFile = file;
    this.validationResult = null;
    this.importResult = null;

    // Validate file type - accept both JSON and encrypted exports (which also have .json extension)
    if (file.type !== 'application/json' && !file.name.endsWith('.json') && !file.name.endsWith('.enc')) {
      this.showError('Please select a valid JSON or encrypted export file');
      this.selectedFile = null;
      return;
    }

    // Validate file size
    if (file.size > this.MAX_FILE_SIZE) {
      this.showError(`File size exceeds ${this.formatFileSize(this.MAX_FILE_SIZE)} limit`);
      this.selectedFile = null;
      return;
    }

    // Read file content
    this.currentStep = ImportStep.VALIDATING;
    const reader = new FileReader();

    reader.onload = (e) => {
      try {
        const content = e.target?.result as string;
        this.fileContent = content;
        this.isEncryptedFile = false;
        this.decryptionPassword = '';

        // Detect encrypted export: starts with salt:ciphertext (not JSON)
        const trimmed = content.trim();
        if (!trimmed.startsWith('[') && !trimmed.startsWith('{')) {
          const colonIdx = trimmed.indexOf(':');
          if (colonIdx > 0 && colonIdx < 50) {
            // This is an encrypted export file
            this.isEncryptedFile = true;
            this.validationResult = {
              isValid: true,
              entriesFound: 0,
              validEntries: 0,
              invalidEntries: 0
            };
            this.currentStep = ImportStep.PREVIEW;
            return;
          }
          this.showError('Invalid file format. Please upload a JSON or encrypted export file.');
          this.currentStep = ImportStep.SELECT_FILE;
          this.selectedFile = null;
          return;
        }

        // Parse and validate JSON structure
        const parsed = JSON.parse(content);

        // Validate JSON structure - expect an array or object with entries
        let entriesFound = 0;
        let validEntries = 0;

        if (Array.isArray(parsed)) {
          entriesFound = parsed.length;
          validEntries = this.countValidEntries(parsed);
        } else if (parsed.entries && Array.isArray(parsed.entries)) {
          entriesFound = parsed.entries.length;
          validEntries = this.countValidEntries(parsed.entries);
        } else if (parsed.vault && Array.isArray(parsed.vault)) {
          entriesFound = parsed.vault.length;
          validEntries = this.countValidEntries(parsed.vault);
        } else if (typeof parsed === 'object' && parsed !== null) {
          entriesFound = 1;
          validEntries = this.isValidEntry(parsed) ? 1 : 0;
        }

        const invalidEntries = entriesFound - validEntries;

        this.validationResult = {
          isValid: validEntries > 0,
          entriesFound,
          validEntries,
          invalidEntries
        };

        if (this.validationResult.isValid) {
          this.currentStep = ImportStep.PREVIEW;
        } else {
          this.showError('No valid entries found in the file');
          this.currentStep = ImportStep.SELECT_FILE;
          this.selectedFile = null;
        }
      } catch (error) {
        this.showError('Invalid JSON file. Please check the file format.');
        this.currentStep = ImportStep.SELECT_FILE;
        this.selectedFile = null;
      }
    };

    reader.onerror = () => {
      this.showError('Failed to read file');
      this.currentStep = ImportStep.SELECT_FILE;
      this.selectedFile = null;
    };

    reader.readAsText(file);
  }

  private countValidEntries(entries: any[]): number {
    return entries.filter(entry => this.isValidEntry(entry)).length;
  }

  private isValidEntry(entry: any): boolean {
    // A valid entry should have at least a title/name and some credential info
    return entry && (
      entry.title || entry.name || entry.url || entry.username || entry.password || entry.notes
    );
  }

  onImport(): void {
    if (!this.fileContent) {
      this.showError('No file content to import');
      return;
    }

    if (this.isEncryptedFile && !this.decryptionPassword) {
      this.showError('Please enter the decryption password for this encrypted file.');
      return;
    }

    this.currentStep = ImportStep.IMPORTING;
    this.isLoading = true;

    const request: any = {
      data: this.fileContent,
      format: 'JSON'
    };

    if (this.isEncryptedFile) {
      request.password = this.decryptionPassword;
    }

    this.backupService.importVault(request)
      .pipe(finalize(() => this.isLoading = false))
      .subscribe({
        next: (result: ImportResult) => {
          this.importResult = result;
          this.currentStep = ImportStep.RESULTS;
          this.showSuccess(`Successfully imported ${result.successCount || 0} entries`);
        },
        error: (error) => {
          this.handleImportError(error);
        }
      });
  }

  private handleImportError(error: any): void {
    let errorMessage = 'Import failed. Please try again.';

    if (error.error?.message) {
      errorMessage = error.error.message;
    } else if (error.message) {
      errorMessage = error.message;
    }

    this.showError(errorMessage);
    this.currentStep = ImportStep.PREVIEW;
  }

  onCancel(): void {
    this.resetState();
  }

  onImportAnother(): void {
    this.resetState();
  }

  private resetState(): void {
    this.currentStep = ImportStep.SELECT_FILE;
    this.selectedFile = null;
    this.fileContent = '';
    this.validationResult = null;
    this.importResult = null;
    this.isDragging = false;
    this.isLoading = false;
    this.isEncryptedFile = false;
    this.decryptionPassword = '';
  }

  private showError(message: string): void {
    this.snackBar.open(message, 'Dismiss', {
      duration: 5000,
      panelClass: 'error-snackbar'
    });
  }

  private showSuccess(message: string): void {
    this.snackBar.open(message, 'Dismiss', {
      duration: 5000,
      panelClass: 'success-snackbar'
    });
  }

  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
  }

  get fileSize(): string {
    return this.selectedFile ? this.formatFileSize(this.selectedFile.size) : '';
  }
}
