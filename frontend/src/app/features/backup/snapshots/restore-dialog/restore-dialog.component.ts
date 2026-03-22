import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { LucideAngularModule } from 'lucide-angular';

export interface RestoreDialogData {
  snapshotId: number;
  snapshotDate: string;
}

@Component({
  selector: 'app-restore-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    LucideAngularModule
  ],
  templateUrl: './restore-dialog.component.html',
  styleUrls: ['./restore-dialog.component.css']
})
export class RestoreDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<RestoreDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: RestoreDialogData
  ) { }

  onCancel(): void {
    this.dialogRef.close(false);
  }

  onConfirm(): void {
    this.dialogRef.close(true);
  }
}
