import { Component, EventEmitter, Output, inject, ViewEncapsulation, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { PasswordGeneratorControllerService } from '../../../core/api/api/passwordGeneratorController.service';
import { PasswordGeneratorRequest } from '../../../core/api/model/passwordGeneratorRequest';

@Component({
  selector: 'app-password-generator-widget',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './password-generator-widget.component.html',
  styleUrl: './password-generator-widget.component.css',
  encapsulation: ViewEncapsulation.None
})
export class PasswordGeneratorWidgetComponent implements OnInit {
  private generatorService = inject(PasswordGeneratorControllerService);
  private fb = inject(FormBuilder);
  private cdr = inject(ChangeDetectorRef);

  @Output() passwordGenerated = new EventEmitter<string>();

  optionsForm: FormGroup;

  constructor() {
    this.optionsForm = this.fb.group({
      length: [16],
      includeUppercase: [true],
      includeLowercase: [true],
      includeNumbers: [true],
      includeSpecial: [true],
      excludeSimilar: [false],
      excludeAmbiguous: [false]
    });
  }

  ngOnInit(): void {
    // Ensure values are set and trigger change detection
    this.optionsForm.patchValue({
      length: 16,
      includeUppercase: true,
      includeLowercase: true,
      includeNumbers: true,
      includeSpecial: true,
      excludeSimilar: false,
      excludeAmbiguous: false
    });
    this.cdr.detectChanges();
  }

  generatePassword() {
    const request: PasswordGeneratorRequest = {
      ...this.optionsForm.value,
      count: 1
    };

    this.generatorService.generatePassword(request).subscribe({
      next: (response) => {
        if (response && response.password) {
          this.passwordGenerated.emit(response.password);
        }
      },
      error: (err) => {
        console.error('Failed to generate password', err);
      }
    });
  }
}
