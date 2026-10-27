import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'durationFormat',
  standalone: true
})
export class DurationFormatPipe implements PipeTransform {
  transform(days: number | undefined | null): string {
    if (days == null || days <= 0) return '0 días';

    const months = Math.floor(days / 30);
    const remainderAfterMonths = days % 30;
    const weeks = Math.floor(remainderAfterMonths / 7);
    const remainingDays = remainderAfterMonths % 7;

    const parts = [];

    if (months > 0) {
      parts.push(months === 1 ? '1 mes' : `${months} meses`);
    }
    if (weeks > 0) {
      parts.push(weeks === 1 ? '1 semana' : `${weeks} semanas`);
    }
    if (remainingDays > 0) {
      parts.push(remainingDays === 1 ? '1 día' : `${remainingDays} días`);
    }

    if (parts.length > 1) {
      const lastPart = parts.pop();
      return `${parts.join(', ')} y ${lastPart}`;
    }

    return parts[0];
  }
}
