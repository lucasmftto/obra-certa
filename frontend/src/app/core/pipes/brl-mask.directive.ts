import { Directive, ElementRef, forwardRef, inject } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

const NAVIGATION = ['Tab','Enter','Escape','ArrowLeft','ArrowRight','ArrowUp','ArrowDown','Home','End'];

@Directive({
  selector: 'input[brlMask]',
  standalone: true,
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => BrlMaskDirective),
    multi: true,
  }],
  host: {
    'type':        'text',
    'inputmode':   'numeric',
    '(focus)':     'onFocus()',
    '(blur)':      'onBlur()',
    '(keydown)':   'onKeydown($event)',
    '(paste)':     '$event.preventDefault()',  // bloqueia colar texto
  },
})
export class BrlMaskDirective implements ControlValueAccessor {
  private readonly el = inject(ElementRef<HTMLInputElement>);

  private cents = 0;       // valor em centavos (inteiro)
  private resetOnKey = false; // true logo após o focus

  private onChange: (v: number) => void = () => {};
  private onTouched: () => void = () => {};

  // ── ControlValueAccessor ────────────────────────────────────────────────

  writeValue(value: number | null): void {
    this.cents = Math.round((value ?? 0) * 100);
    this.render();
  }

  registerOnChange(fn: (v: number) => void): void  { this.onChange = fn; }
  registerOnTouched(fn: () => void): void           { this.onTouched = fn; }
  setDisabledState(d: boolean): void                { this.el.nativeElement.disabled = d; }

  // ── Eventos ─────────────────────────────────────────────────────────────

  onFocus(): void {
    this.resetOnKey = true;
    // Seleciona tudo para feedback visual
    setTimeout(() => this.el.nativeElement.select(), 0);
  }

  onBlur(): void {
    this.onTouched();
    this.resetOnKey = false;
  }

  onKeydown(event: KeyboardEvent): void {
    // Teclas de navegação passam sem interferência
    if (NAVIGATION.includes(event.key)) {
      this.resetOnKey = false;
      return;
    }

    // Bloqueia tudo que não é dígito, backspace ou delete
    event.preventDefault();

    if (event.key === 'Backspace' || event.key === 'Delete') {
      this.cents = this.resetOnKey ? 0 : Math.floor(this.cents / 10);
      this.resetOnKey = false;
      this.emit();
      return;
    }

    if (!/^\d$/.test(event.key)) return; // ignora Ctrl+A, F5, etc.

    if (this.resetOnKey) {
      this.cents = 0;        // primeiro dígito após focus limpa o campo
      this.resetOnKey = false;
    }

    if (this.cents >= 9_999_999_99) return; // teto: R$ 99.999.999,99

    this.cents = this.cents * 10 + parseInt(event.key, 10);
    this.emit();
  }

  // ── Helpers ──────────────────────────────────────────────────────────────

  private emit(): void {
    this.onChange(this.cents / 100);
    this.render();
  }

  private render(): void {
    const [int, dec] = (this.cents / 100).toFixed(2).split('.');
    this.el.nativeElement.value =
      int.replace(/\B(?=(\d{3})+(?!\d))/g, '.') + ',' + dec;
  }
}
