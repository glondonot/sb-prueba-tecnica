# Resultados del benchmark – Módulo 3

- Fecha: 2026-09-23T10:28:32 · PostgreSQL 16.15 (Docker) · Windows 11 · Intel64 Family 6 Model 154 Stepping 3, GenuineIntel
- Volumen: 500,000 clientes · 10,000,000 pedidos
  (orders: 712 MB, customers: 39 MB)
- Tiempo = mediana de 5 ejecuciones de `EXPLAIN (ANALYZE, BUFFERS)` en caliente (tiempo en servidor).
- "Mejora" se calcula contra la línea base E0 (en E4/E5 la consulta cambia; comparar entre sí a y b).

| Escenario | Descripción | Mediana (ms) | Filas | Buffers | Mejora vs E0 |
|---|---|---:|---:|---:|---:|
| E0_linea_base | Línea base: solo PKs, FK sin índice, estadísticas al día | 447.8 | 390,043 | 67,502 | 1.0x |
| E2_indices | Índices cubrientes: customers(country) INCLUDE (...) y orders(customer_id) INCLUDE (...) | 91.7 | 390,043 | 119,635 | 4.9x |
| E3_vista_materializada | Vista materializada con índice (country, order_date, order_id) | 51.0 | 390,043 | 9,501 | 8.8x |
| E4a_fecha_sin_particion | Consulta con filtro de fecha (último año) sobre orders con índices de E2 | 88.2 | 51,372 | 125,675 | 5.1x |
| E4b_fecha_con_particion | Misma consulta sobre orders_part: partition pruning (solo lee 2026) | 106.9 | 51,372 | 8,538 | 4.2x |
| E5a_pagina_offset | Página profunda con OFFSET 100,000 LIMIT 50 (lee y descarta 100,000 filas) | 13.8 | 50 | 2,438 | 32.4x |
| E5b_pagina_keyset | Misma página con keyset: WHERE (order_date, order_id) < (último visto) LIMIT 50 | 0.1 | 50 | 7 | 8,956.4x |

## Estadísticas del planificador (E1)

| | Estimado sin `ANALYZE` | Estimado con `ANALYZE` | Real |
|---|---:|---:|---:|
| Clientes de México | 347 | 19,833 | 19,557 |
| Filas del resultado | 43,332 | 396,665 | 390,043 |

- Plan elegido sin estadísticas: `Hash Join → Seq Scan on orders → Hash → Seq Scan on customers`
- Plan elegido con estadísticas: `Hash Join → Seq Scan on orders → Hash → Seq Scan on customers`

## Costos asociados

| Concepto | Valor |
|---|---|
| Crear índices de E2 | 17.8 s (índice de orders: 473 MB) |
| Crear vista materializada + índices | 50.2 s |
| `REFRESH MATERIALIZED VIEW CONCURRENTLY` | 56.9 s |
| Crear tabla particionada + copia + índice | 24.8 s |
| Traer todas las filas al cliente – línea base | 567.3 ms |
| Traer todas las filas al cliente – con índices | 484.6 ms |

Los planes de ejecución completos de cada escenario están en `planes/`.
