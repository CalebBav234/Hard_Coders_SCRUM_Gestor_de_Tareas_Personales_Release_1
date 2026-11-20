import { formatBob, formatMinutes } from './project';

describe('formatBob', () => {
  it('formats null as zero', () => {
    expect(formatBob(null)).toBe('Bs 0,00');
  });

  it('formats undefined as zero', () => {
    expect(formatBob(undefined)).toBe('Bs 0,00');
  });

  it('formats zero', () => {
    expect(formatBob(0)).toBe('Bs 0,00');
  });

  it('formats positive amount', () => {
    expect(formatBob(1500.5)).toBe('Bs 1.500,50');
  });
});

describe('formatMinutes', () => {
  it('formats null as zero minutes', () => {
    expect(formatMinutes(null)).toBe('0 min');
  });

  it('formats undefined as zero minutes', () => {
    expect(formatMinutes(undefined)).toBe('0 min');
  });

  it('formats zero minutes', () => {
    expect(formatMinutes(0)).toBe('0 min');
  });

  it('formats minutes only (under 1 hour)', () => {
    expect(formatMinutes(30)).toBe('30 min');
  });

  it('formats exact hours', () => {
    expect(formatMinutes(60)).toBe('1 h');
  });

  it('formats hours and minutes', () => {
    expect(formatMinutes(90)).toBe('1 h 30 min');
  });

  it('formats large values', () => {
    expect(formatMinutes(150)).toBe('2 h 30 min');
  });
});
