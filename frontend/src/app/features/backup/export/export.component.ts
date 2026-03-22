import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { BackupControllerService } from '../../../core/api/api/backupController.service';
import { ExportResponse } from '../../../core/api/model/exportResponse';
import { finalize } from 'rxjs/operators';

@Component({
  selector: 'app-export',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    LucideAngularModule
  ],
  templateUrl: './export.component.html',
  styleUrls: ['./export.component.css']
})
export class ExportComponent implements OnInit {
  exportForm!: FormGroup;
  isLoading = false;
  showPasswordField = false;

  exportFormats = [
    { value: 'JSON', label: 'JSON (Unencrypted)', description: 'Standard JSON format for backups and transfers' },
    { value: 'ENCRYPTED', label: 'Encrypted (JSON)', description: 'Password-protected encrypted JSON file for maximum security' }
  ];

  constructor(
    private fb: FormBuilder,
    private backupService: BackupControllerService,
    private snackBar: MatSnackBar
  ) { }

  ngOnInit(): void {
    this.initForm();
  }

  private initForm(): void {
    this.exportForm = this.fb.group({
      format: ['JSON', Validators.required],
      password: ['']
    });

    // Subscribe to format changes to toggle password field validation
    this.exportForm.get('format')?.valueChanges.subscribe((format) => {
      this.showPasswordField = format === 'ENCRYPTED';
      const passwordControl = this.exportForm.get('password');

      if (this.showPasswordField) {
        passwordControl?.setValidators([Validators.required, Validators.minLength(8)]);
      } else {
        passwordControl?.clearValidators();
        passwordControl?.setValue('');
      }
      passwordControl?.updateValueAndValidity();
    });
  }

  onExport(): void {
    if (this.exportForm.invalid) {
      this.markFormGroupTouched(this.exportForm);
      return;
    }

    const format: string = this.exportForm.value.format;
    const password = format === 'ENCRYPTED' ? this.exportForm.value.password : undefined;

    this.isLoading = true;

    this.backupService.exportVault(format, password || undefined)
      .pipe(finalize(() => this.isLoading = false))
      .subscribe({
        next: (response: ExportResponse) => {
          this.handleExportSuccess(response);
        },
        error: (error) => {
          this.handleExportError(error);
        }
      });
  }

  private handleExportSuccess(response: ExportResponse): void {
    if (response.data && response.fileName) {
      this.downloadFile(response.data, response.fileName);
      this.snackBar.open(
        `Successfully exported ${response.entryCount || 0} entries`,
        'Dismiss',
        { duration: 5000, panelClass: 'success-snackbar' }
      );
    } else {
      this.snackBar.open(
        'Export completed but no data received',
        'Dismiss',
        { duration: 5000, panelClass: 'warning-snackbar' }
      );
    }
  }

  private handleExportError(error: any): void {
    let errorMessage = 'Export failed. Please try again.';

    if (error.error?.message) {
      errorMessage = error.error.message;
    } else if (error.message) {
      errorMessage = error.message;
    }

    this.snackBar.open(errorMessage, 'Dismiss', {
      duration: 5000,
      panelClass: 'error-snackbar'
    });
  }

  private downloadFile(data: string, fileName: string): void {
    try {
      let blob: Blob;

      // Check if data is base64 encoded
      if (this.isBase64(data)) {
        // Decode base64 to binary
        const byteCharacters = atob(data);
        const byteNumbers = new Array(byteCharacters.length);

        for (let i = 0; i < byteCharacters.length; i++) {
          byteNumbers[i] = byteCharacters.charCodeAt(i);
        }

        const byteArray = new Uint8Array(byteNumbers);
        blob = new Blob([byteArray], { type: 'application/octet-stream' });
      } else {
        // Data is plain text (JSON), create blob directly
        blob = new Blob([data], { type: 'application/json' });
      }

      // Create download link
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = fileName;

      // Trigger download
      document.body.appendChild(link);
      link.click();

      // Cleanup
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (error) {
      console.error('Error downloading file:', error);
      this.snackBar.open(
        'Error downloading file. Please try again.',
        'Dismiss',
        { duration: 5000, panelClass: 'error-snackbar' }
      );
    }
  }

  private isBase64(str: string): boolean {
    try {
      // Check if string looks like base64 (length is multiple of 4, contains only valid chars)
      const base64Pattern = /^[A-Za-z0-9+/]*={0,2}$/;
      if (!base64Pattern.test(str) || str.length % 4 !== 0) {
        return false;
      }
      // Try to decode
      atob(str);
      return true;
    } catch (e) {
      return false;
    }
  }

  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.values(formGroup.controls).forEach(control => {
      control.markAsTouched();
      if ((control as any).controls) {
        this.markFormGroupTouched(control as FormGroup);
      }
    });
  }

  get formatControl() {
    return this.exportForm.get('format');
  }

  get passwordControl() {
    return this.exportForm.get('password');
  }
}
