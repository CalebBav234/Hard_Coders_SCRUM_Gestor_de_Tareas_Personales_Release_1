import { expect, describe, it, test, beforeEach, afterEach, beforeAll, afterAll } from 'vitest';

declare global {
  const describe: typeof describe;
  const it: typeof it;
  const test: typeof test;
  const expect: typeof expect;
  const beforeEach: typeof beforeEach;
  const afterEach: typeof afterEach;
  const beforeAll: typeof beforeAll;
  const afterAll: typeof afterAll;
}

export {};
