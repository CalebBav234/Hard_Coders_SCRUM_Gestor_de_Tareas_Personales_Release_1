import { DurationFormatPipe } from './duration-format.pipe';

describe('DurationFormatPipe', () => {
  const pipe = new DurationFormatPipe();

  it('returns 0 días for null', () => {
    expect(pipe.transform(null)).toBe('0 días');
  });

  it('returns 0 días for undefined', () => {
    expect(pipe.transform(undefined)).toBe('0 días');
  });

  it('returns 0 días for zero', () => {
    expect(pipe.transform(0)).toBe('0 días');
  });

  it('returns 0 días for negative', () => {
    expect(pipe.transform(-5)).toBe('0 días');
  });

  it('formats 1 day', () => {
    expect(pipe.transform(1)).toBe('1 día');
  });

  it('formats multiple days', () => {
    expect(pipe.transform(5)).toBe('5 días');
  });

  it('formats 1 week', () => {
    expect(pipe.transform(7)).toBe('1 semana');
  });

  it('formats 2 weeks', () => {
    expect(pipe.transform(14)).toBe('2 semanas');
  });

  it('formats 1 month', () => {
    expect(pipe.transform(30)).toBe('1 mes');
  });

  it('formats 2 months', () => {
    expect(pipe.transform(60)).toBe('2 meses');
  });

  it('formats 1 month and 1 day', () => {
    expect(pipe.transform(31)).toBe('1 mes y 1 día');
  });

  it('formats 1 month and 2 days', () => {
    expect(pipe.transform(32)).toBe('1 mes y 2 días');
  });

  it('formats 1 week and 1 day', () => {
    expect(pipe.transform(8)).toBe('1 semana y 1 día');
  });

  it('formats 1 month, 1 week and 1 day', () => {
    expect(pipe.transform(38)).toBe('1 mes, 1 semana y 1 día');
  });

  it('formats 2 months, 3 weeks and 4 days', () => {
    expect(pipe.transform(30 * 2 + 21 + 4)).toBe('2 meses, 3 semanas y 4 días');
  });

  it('formats exactly 1 month', () => {
    expect(pipe.transform(30)).toBe('1 mes');
  });

  it('formats exactly 1 week', () => {
    expect(pipe.transform(7)).toBe('1 semana');
  });
});
