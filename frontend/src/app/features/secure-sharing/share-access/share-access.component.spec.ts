import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ShareAccessComponent } from './share-access.component';
import { importProvidersFrom } from '@angular/core';
import { LucideAngularModule, ShieldCheck, Eye, EyeOff, Copy, Check, AlertTriangle } from 'lucide-angular';

describe('ShareAccessComponent', () => {
  let component: ShareAccessComponent;
  let fixture: ComponentFixture<ShareAccessComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ShareAccessComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        importProvidersFrom(LucideAngularModule.pick({ ShieldCheck, Eye, EyeOff, Copy, Check, AlertTriangle }))
      ]
    })
      .compileComponents();

    fixture = TestBed.createComponent(ShareAccessComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
