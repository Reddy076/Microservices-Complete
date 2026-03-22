import { Component, Input, Output, EventEmitter, forwardRef, ElementRef, HostListener, ViewEncapsulation, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';

export interface SelectOption {
  value: string | number;
  label: string;
}

@Component({
  selector: 'app-custom-select',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './custom-select.component.html',
  styleUrl: './custom-select.component.css',
  encapsulation: ViewEncapsulation.None,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CustomSelectComponent),
      multi: true
    }
  ]
})
export class CustomSelectComponent implements ControlValueAccessor, OnDestroy {
  @Input() options: SelectOption[] = [];
  @Input() placeholder = 'Select...';
  @Output() isOpenChange = new EventEmitter<boolean>();

  isOpen = false;
  selectedValue: string | number = '';
  isDisabled = false;
  dropdownStyle: { [key: string]: string } = {};

  /** Tracks how many CustomSelect instances currently have their dropdown open */
  private static openCount = 0;

  /** Per-instance reference to the scrollable ancestor we locked */
  private lockedScrollParent: HTMLElement | null = null;

  private onChange: (value: string | number) => void = () => { };
  private onTouched: () => void = () => { };

  constructor(private readonly el: ElementRef) { }

  get selectedLabel(): string {
    const found = this.options.find(o => String(o.value) === String(this.selectedValue));
    return found ? found.label : this.placeholder;
  }

  get hasSelection(): boolean {
    return this.selectedValue !== '' && this.selectedValue !== null && this.selectedValue !== undefined;
  }

  /** Walk up the DOM to find the nearest element that actually scrolls */
  private findScrollableParent(el: HTMLElement): HTMLElement {
    let node: HTMLElement | null = el.parentElement;
    while (node && node !== document.documentElement) {
      const style = globalThis.getComputedStyle(node);
      const overflowY = style.overflowY;
      if ((overflowY === 'auto' || overflowY === 'scroll') && node.scrollHeight > node.clientHeight) {
        return node;
      }
      node = node.parentElement;
    }
    // Fallback to body
    return document.body;
  }

  private lockScroll() {
    CustomSelectComponent.openCount++;
    if (CustomSelectComponent.openCount === 1) {
      // Only find + lock on the first open
      this.lockedScrollParent = this.findScrollableParent(this.el.nativeElement);
      this.lockedScrollParent.style.overflow = 'hidden';
    }
  }

  private unlockScroll() {
    if (CustomSelectComponent.openCount > 0) {
      CustomSelectComponent.openCount--;
    }
    if (CustomSelectComponent.openCount === 0) {
      if (this.lockedScrollParent) {
        this.lockedScrollParent.style.overflow = '';
        this.lockedScrollParent = null;
      }
    }
  }

  toggle() {
    if (this.isDisabled) return;
    if (this.isOpen) {
      this.unlockScroll();
    } else {
      // Calculate position relative to viewport so fixed positioning works through overflow:hidden
      const rect = this.el.nativeElement.querySelector('.custom-select-trigger').getBoundingClientRect();
      this.dropdownStyle = {
        position: 'fixed',
        top: `${rect.bottom + 4}px`,
        left: `${rect.left}px`,
        width: `${rect.width}px`,
        zIndex: '9999'
      };
      this.lockScroll();
    }
    this.isOpen = !this.isOpen;
    this.isOpenChange.emit(this.isOpen);
  }

  select(option: SelectOption) {
    this.selectedValue = option.value;
    this.onChange(option.value);
    this.onTouched();
    if (this.isOpen) {
      this.isOpen = false;
      this.unlockScroll();
      this.isOpenChange.emit(this.isOpen);
    }
  }

  selectPlaceholder() {
    this.selectedValue = '';
    this.onChange('');
    this.onTouched();
    if (this.isOpen) {
      this.isOpen = false;
      this.unlockScroll();
      this.isOpenChange.emit(this.isOpen);
    }
  }

  isSelected(option: SelectOption): boolean {
    return String(this.selectedValue) === String(option.value);
  }

  @HostListener('document:click', ['$event'])
  onClickOutside(event: Event) {
    if (this.el.nativeElement.contains(event.target)) {
      return;
    }
    if (this.isOpen) {
      this.isOpen = false;
      this.unlockScroll();
      this.isOpenChange.emit(this.isOpen);
    }
  }

  // ControlValueAccessor
  writeValue(value: string | number): void {
    this.selectedValue = value ?? '';
  }

  registerOnChange(fn: (value: string | number) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.isDisabled = isDisabled;
  }

  ngOnDestroy(): void {
    if (this.isOpen) {
      this.isOpen = false;
      this.unlockScroll();
    }
  }
}
