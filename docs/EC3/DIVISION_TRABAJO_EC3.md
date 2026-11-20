# División de Trabajo — EC3

**Proyecto:** Gestor de Tareas Personales — Hard Coders  
**Rama:** `Refactor-E2`  
**Fuente:** `docs/EC3/ANALISIS_EC3.md` (sección 17.1 Fases de implementación y 17.2 Desglose detallado de actividades)  
**Estado:** Planificación. No se implementó código.

---

## Resumen de la distribución

- **2 personas** con **1 fase cada una** (Fase 1 y Fase 7).
- **3 personas** con las **5 fases restantes**, donde una persona toma **1 fase compleja** (Fase 5) y las otras 2 personas se reparten **2 fases cada una** (Fases 2+3 y 4+6).

---

## Asignación por persona

### Persona 1 — Fase 1 (1 fase)

| Fase | Contenido |
|------|-----------|
| **Fase 1 — Preparación y correcciones funcionales** | Resolver inconsistencias detectadas. Alinear validaciones. Corregir problemas identificados en el proceso principal. Determinar los requisitos técnicos del entorno de pruebas. |

### Persona 2 — Fase 7 (1 fase)

| Fase | Contenido |
|------|-----------|
| **Fase 7 — Documentación y evidencias** | Preparar reportes. Capturar evidencias. Documentar comandos. Preparar el informe académico final. |

### Persona 3 — Fase 5 (1 fase compleja)

| Fase | Contenido |
|------|-----------|
| **Fase 5 — Automatización E2E** | Configurar Playwright si falta. Automatizar formularios principales. Automatizar los dos CREATE complementarios. Automatizar escenarios negativos. Validar la interfaz y persistencia. |

### Persona 4 — Fases 2 y 3 (2 fases)

| Fase | Contenido |
|------|-----------|
| **Fase 2 — Formularios y funcionalidades complementarias** | Resolver brechas de creación identificadas. Definir los flujos CREATE de las dos tablas complementarias. Especificar formularios, endpoints y persistencia necesarios. |
| **Fase 3 — Pruebas de diseño** | Preparar los casos EP. Preparar los casos BVA3. Preparar la cobertura de estados y transiciones. Preparar la tabla de decisión. |

### Persona 5 — Fases 4 y 6 (2 fases)

| Fase | Contenido |
|------|-----------|
| **Fase 4 — Pruebas unitarias y de integración** | Ampliar pruebas de servicios y reglas de negocio. Probar validaciones y restricciones. Probar persistencia y manejo de errores. |
| **Fase 6 — Cobertura y regresión** | Medir cobertura. Completar ramas y líneas faltantes. Validar los escenarios R1–R9. Verificar actualización reactiva de la interfaz. |

---

## Orden de ejecución (dependencias)

1. **Fase 1 (Persona 1)** — debe completarse primero; desbloquea al resto.
2. **Fases 2, 3, 4, 5** — pueden iniciarse tras Fase 1.
3. **Fase 6 (Persona 5)** — depende de Fase 4 y Fase 5.
4. **Fase 7 (Persona 2)** — última fase; depende de todo lo anterior.

---

## Notas importantes

- Esta distribución se basa exclusivamente en el contenido de `docs/EC3/ANALISIS_EC3.md`.
- **No se implementa nada hasta que se revise y apruebe el informe de análisis.**
- El único archivo nuevo autorizado en esta fase es `docs/EC3/ANALISIS_EC3.md`. Este documento de división de trabajo es una propuesta organizativa para el equipo.
- No modificar código fuente, configuraciones, pruebas ni base de datos hasta completar la fase de análisis.
