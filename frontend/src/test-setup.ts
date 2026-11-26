// Configuración global del entorno de pruebas Angular + Vitest.
// Proporciona el compilador JIT e inicializa TestBed para componentes standalone.
import '@angular/compiler';
import { TestBed } from '@angular/core/testing';
import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';
import { BrowserDynamicTestingModule } from '@angular/platform-browser-dynamic/testing';

TestBed.initTestEnvironment(
  BrowserDynamicTestingModule,
  platformBrowserDynamic()
);
