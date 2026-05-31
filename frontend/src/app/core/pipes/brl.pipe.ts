import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'brl', standalone: true })
export class BrlPipe implements PipeTransform {
  transform(value: number | string | null | undefined): string {
    const n = value == null ? 0 : typeof value === 'string' ? parseFloat(value) : value;
    if (isNaN(n)) return 'R$ 0,00';

    const negative = n < 0;
    const abs = Math.abs(n).toFixed(2);
    const [intPart, decPart] = abs.split('.');
    const formatted = intPart.replace(/\B(?=(\d{3})+(?!\d))/g, '.');

    return `${negative ? '-' : ''}R$ ${formatted},${decPart}`;
  }
}
