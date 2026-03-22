import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { RestoreDialogComponent, RestoreDialogData } from './restore-dialog.component';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { LucideAngularModule } from 'lucide-angular';

describe('RestoreDialogComponent', () => {
  let component: RestoreDialogComponent;
  let fixture: ComponentFixture<RestoreDialogComponent>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<RestoreDialogComponent>>;

  const mockData: RestoreDialogData = {
    snapshotId: 1,
    snapshotDate: 'Jan 15, 2024, 2:30 PM'
  };

  beforeEach(waitForAsync(() => {
    const dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['close']);

    TestBed.configureTestingModule({
      imports: [
        RestoreDialogComponent,
        NoopAnimationsModule,
        MatDialogModule,
        MatButtonModule,
        LucideAngularModule
      ],
      providers: [
        { provide: MAT_DIALOG_DATA, useValue: mockData },
        { provide: MatDialogRef, useValue: dialogRefSpy }
      ]
    }).compileComponents();

    dialogRef = TestBed.inject(MatDialogRef) as jasmine.SpyObj<MatDialogRef<RestoreDialogComponent>>;
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(RestoreDialogComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should receive data', () => {
    expect(component.data.snapshotId).toBe(1);
    expect(component.data.snapshotDate).toBe('Jan 15, 2024, 2:30 PM');
  });

  it('should cancel', () => {
    component.onCancel();
    expect(dialogRef.close).toHaveBeenCalledWith(false);
  });

  it('should confirm', () => {
    component.onConfirm();
    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });
});
