import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PasswordGeneratorWidgetComponent } from './password-generator-widget.component';
import { PasswordGeneratorControllerService } from '../../../core/api/api/passwordGeneratorController.service';
import { of } from 'rxjs';
import { ReactiveFormsModule } from '@angular/forms';

describe('PasswordGeneratorWidgetComponent', () => {
  let component: PasswordGeneratorWidgetComponent;
  let fixture: ComponentFixture<PasswordGeneratorWidgetComponent>;
  let mockGeneratorService: jasmine.SpyObj<PasswordGeneratorControllerService>;

  beforeEach(async () => {
    mockGeneratorService = jasmine.createSpyObj('PasswordGeneratorControllerService', ['generatePassword']);
    mockGeneratorService.generatePassword.and.returnValue(of({ password: 'GeneratedP@ss123' }) as any);

    await TestBed.configureTestingModule({
      imports: [PasswordGeneratorWidgetComponent, ReactiveFormsModule],
      providers: [
        { provide: PasswordGeneratorControllerService, useValue: mockGeneratorService }
      ]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(PasswordGeneratorWidgetComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with default options', () => {
    expect(component.optionsForm.value.length).toBe(16);
    expect(component.optionsForm.value.includeUppercase).toBeTrue();
  });

  it('should call generator service and emit password on generatePassword()', () => {
    spyOn(component.passwordGenerated, 'emit');
    component.generatePassword();

    expect(mockGeneratorService.generatePassword).toHaveBeenCalled();
    expect(component.passwordGenerated.emit).toHaveBeenCalledWith('GeneratedP@ss123');
  });
});
