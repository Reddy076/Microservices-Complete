import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { BackupControllerService } from '../../../core/api/api/backupController.service';
import { SnapshotResponse } from '../../../core/api/model/snapshotResponse';
import { finalize } from 'rxjs/operators';
import { RestoreDialogComponent, RestoreDialogData } from './restore-dialog/restore-dialog.component';

interface SnapshotViewModel extends SnapshotResponse {
  formattedDate: string;
}

@Component({
  selector: 'app-snapshot-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDialogModule,
    MatTableModule,
    LucideAngularModule
  ],
  templateUrl: './snapshot-list.component.html',
  styleUrls: ['./snapshot-list.component.css']
})
export class SnapshotListComponent implements OnInit {
  snapshots: SnapshotViewModel[] = [];
  isLoading = false;
  restoringId: number | null = null;
  displayedColumns: string[] = ['date', 'title', 'actions'];

  constructor(
    private backupService: BackupControllerService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) { }

  ngOnInit(): void {
    this.loadSnapshots();
  }

  loadSnapshots(): void {
    this.isLoading = true;
    this.backupService.getAllSnapshots()
      .pipe(finalize(() => this.isLoading = false))
      .subscribe({
        next: (snapshots) => {
          this.snapshots = snapshots
            .map(s => this.toViewModel(s))
            .sort((a, b) => new Date(b.changedAt!).getTime() - new Date(a.changedAt!).getTime());
        },
        error: (error) => {
          this.showError('Failed to load snapshots: ' + (error.message || 'Unknown error'));
        }
      });
  }

  onRestore(snapshot: SnapshotViewModel): void {
    const dialogRef = this.dialog.open(RestoreDialogComponent, {
      width: '500px',
      data: {
        snapshotId: snapshot.id,
        snapshotDate: snapshot.formattedDate
      } as RestoreDialogData
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result === true) {
        this.performRestore(snapshot.id!);
      }
    });
  }

  private performRestore(snapshotId: number): void {
    this.restoringId = snapshotId;
    this.backupService.restoreSnapshot(snapshotId)
      .pipe(finalize(() => this.restoringId = null))
      .subscribe({
        next: () => {
          this.showSuccess('Vault restored successfully');
        },
        error: (error) => {
          this.showError('Restore failed: ' + (error.message || 'Unknown error'));
        }
      });
  }

  private toViewModel(snapshot: SnapshotResponse): SnapshotViewModel {
    return {
      ...snapshot,
      formattedDate: this.formatDate(snapshot.changedAt)
    };
  }

  private countEntries(passwordData: string | undefined): number {
    if (!passwordData) return 0;
    try {
      const parsed = JSON.parse(passwordData);
      if (Array.isArray(parsed)) {
        return parsed.length;
      }
      if (parsed.entries && Array.isArray(parsed.entries)) {
        return parsed.entries.length;
      }
      return 1;
    } catch {
      return 0;
    }
  }

  private formatDate(dateString: string | undefined): string {
    if (!dateString) return 'Unknown';
    const date = new Date(dateString);
    return date.toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  private showSuccess(message: string): void {
    this.snackBar.open(message, 'Dismiss', {
      duration: 5000,
      panelClass: 'success-snackbar'
    });
  }

  private showError(message: string): void {
    this.snackBar.open(message, 'Dismiss', {
      duration: 8000,
      panelClass: 'error-snackbar'
    });
  }
}
