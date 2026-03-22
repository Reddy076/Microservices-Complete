import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { SnapshotListComponent } from './snapshot-list.component';
import { BackupControllerService } from '../../../core/api/api/backupController.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialogModule } from '@angular/material/dialog';
import { MatTableModule } from '@angular/material/table';
import { LucideAngularModule } from 'lucide-angular';

describe('SnapshotListComponent', () => {
  let component: SnapshotListComponent;
  let fixture: ComponentFixture<SnapshotListComponent>;
  let backupService: jasmine.SpyObj<BackupControllerService>;
  let snackBar: jasmine.SpyObj<MatSnackBar>;
  let dialog: jasmine.SpyObj<MatDialog>;

  beforeEach(waitForAsync(() => {
    const backupServiceSpy = jasmine.createSpyObj('BackupControllerService', ['getAllSnapshots', 'restoreSnapshot']);
    const snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    // MatDialog.open() internally pushes to openDialogs[], so we need to provide that array
    const dialogMock = {
      open: jasmine.createSpy('open'),
      openDialogs: []
    };

    TestBed.configureTestingModule({
      imports: [
        SnapshotListComponent,
        NoopAnimationsModule,
        MatCardModule,
        MatButtonModule,
        MatProgressSpinnerModule,
        MatSnackBarModule,
        MatDialogModule,
        MatTableModule,
        LucideAngularModule
      ],
      providers: [
        { provide: BackupControllerService, useValue: backupServiceSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
        { provide: MatDialog, useValue: dialogMock }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .overrideComponent(SnapshotListComponent, {
        remove: { imports: [MatSnackBarModule, MatDialogModule] }
      })
      .compileComponents();

    backupService = TestBed.inject(BackupControllerService) as jasmine.SpyObj<BackupControllerService>;
    snackBar = TestBed.inject(MatSnackBar) as jasmine.SpyObj<MatSnackBar>;
    dialog = TestBed.inject(MatDialog) as jasmine.SpyObj<MatDialog>;
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SnapshotListComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have correct columns', () => {
    expect(component.displayedColumns).toEqual(['date', 'title', 'actions']);
  });

  it('should load snapshots', () => {
    const mockSnapshots = [
      { id: 1, password: '[{"title":"Test1"}]', changedAt: '2024-01-15T14:30:00Z' },
      { id: 2, password: '[]', changedAt: '2024-01-10T09:15:00Z' }
    ];
    backupService.getAllSnapshots.and.returnValue(of(mockSnapshots) as any);

    component.loadSnapshots();

    expect(component.snapshots.length).toBe(2);
  });

  it('should handle error', () => {
    backupService.getAllSnapshots.and.returnValue(throwError(() => new Error('Failed')));

    component.loadSnapshots();

    expect(snackBar.open).toHaveBeenCalled();
  });

  it('should format date safely', () => {
    const mockSnapshots = [
      { id: 1, entryTitle: 'Test1', changedAt: '2024-01-15T14:30:00Z' },
      { id: 2, entryTitle: 'Test2', changedAt: '2024-01-10T09:15:00Z' }
    ];
    backupService.getAllSnapshots.and.returnValue(of(mockSnapshots) as any);

    component.loadSnapshots();

    expect(component.snapshots[0].entryTitle).toBe('Test1');
    expect(component.snapshots[1].entryTitle).toBe('Test2');
  });

  it('should sort by date', () => {
    const mockSnapshots = [
      { id: 3, password: '[]', changedAt: '2024-01-05T16:45:00Z' },
      { id: 1, password: '[]', changedAt: '2024-01-15T14:30:00Z' },
      { id: 2, password: '[]', changedAt: '2024-01-10T09:15:00Z' }
    ];
    backupService.getAllSnapshots.and.returnValue(of(mockSnapshots) as any);

    component.loadSnapshots();

    expect(component.snapshots[0].id).toBe(1);
    expect(component.snapshots[2].id).toBe(3);
  });

  it('should format date', () => {
    const mockSnapshots = [{ id: 1, password: '[]', changedAt: '2024-01-15T14:30:00Z' }];
    backupService.getAllSnapshots.and.returnValue(of(mockSnapshots) as any);

    component.loadSnapshots();

    expect(component.snapshots[0].formattedDate).toContain('2024');
  });

  it('should map invalid data gracefully', () => {
    const mockSnapshots = [{ id: 1, password: 'invalid', entryTitle: 'Unknown', changedAt: '2024-01-15T14:30:00Z' }];
    backupService.getAllSnapshots.and.returnValue(of(mockSnapshots) as any);

    component.loadSnapshots();

    expect(component.snapshots[0].entryTitle).toBe('Unknown');
  });

  it('should open dialog on restore', () => {
    const snapshot = { id: 1, formattedDate: 'Test', entryCount: 5 };
    dialog.open.and.returnValue({ afterClosed: () => of(false) } as any);

    component.onRestore(snapshot as any);

    expect(dialog.open).toHaveBeenCalled();
  });

  it('should restore on confirm', () => {
    const snapshot = { id: 1, formattedDate: 'Test', entryCount: 5 };
    dialog.open.and.returnValue({ afterClosed: () => of(true) } as any);
    backupService.restoreSnapshot.and.returnValue(of({}) as any);

    component.onRestore(snapshot as any);

    expect(backupService.restoreSnapshot).toHaveBeenCalledWith(1);
  });

  it('should handle restore error', () => {
    const snapshot = { id: 1, formattedDate: 'Test', entryCount: 5 };
    dialog.open.and.returnValue({ afterClosed: () => of(true) } as any);
    backupService.restoreSnapshot.and.returnValue(throwError(() => new Error('Failed')));

    component.onRestore(snapshot as any);

    expect(snackBar.open).toHaveBeenCalled();
  });

  it('should handle empty snapshots', () => {
    backupService.getAllSnapshots.and.returnValue(of([]) as any);

    component.loadSnapshots();

    expect(component.snapshots.length).toBe(0);
  });
});
