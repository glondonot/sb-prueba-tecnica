# Módulo 3 – Benchmark de optimización SQL

Evidencia reproducible de las estrategias propuestas en el PDF para optimizar:

```sql
SELECT o.order_id, o.order_date, c.customer_name, o.total_amount
FROM orders o JOIN customers c ON o.customer_id = c.customer_id
WHERE c.country = 'México';
```

sobre **10.000.000 de pedidos** y **500.000 clientes** en PostgreSQL 16.

## Cómo ejecutarlo

```bash
cd docs/modulo-3
docker compose up -d                      # PostgreSQL de benchmark en el puerto 55432
python -m venv .venv
.venv/Scripts/pip install -r requirements.txt     # Linux/macOS: .venv/bin/pip
.venv/Scripts/python benchmark.py                 # ~4 minutos
docker compose down -v
```

El script (Python + psycopg) genera los datos dentro de PostgreSQL, aplica cada estrategia y mide con
`EXPLAIN (ANALYZE, BUFFERS)`: mediana de 5 ejecuciones en caliente. Escribe:

- [`resultados/RESULTADOS.md`](resultados/RESULTADOS.md): tabla resumen.
- [`resultados/resultados.json`](resultados/resultados.json): datos crudos.
- [`resultados/planes/`](resultados/planes/): plan de ejecución de cada escenario.

## Resumen de la última ejecución

| Escenario | Mediana | Lectura |
|---|---:|---|
| E0 Línea base (solo PKs, FK sin índice) | 447.8 ms | Parallel Seq Scan de 10M filas + Hash Join |
| E2 Índices cubrientes | 91.7 ms | Nested Loop con dos *index-only scans* (4.9x) |
| E3 Vista materializada indexada | 51.0 ms | Un solo *index-only scan* (8.8x), a cambio de un refresco de ~57 s |
| E4 Particionamiento por fecha | 106.9 ms vs 88.2 ms | Menos buffers leídos, pero **no mejora** esta consulta: los índices ya resuelven el acceso |
| E5 Paginación keyset vs OFFSET | 0.1 ms vs 13.8 ms | Una página de 50 filas es inmediata sin importar su profundidad |
| E1 Estadísticas | 347 → 19.833 estimados (real 19.557) | Sin `ANALYZE` el planificador subestima 56 veces los clientes de México |

> Los números dependen del equipo (aquí: portátil con Windows 11 y Docker Desktop). Lo relevante es la magnitud relativa
> entre escenarios y el cambio en los planes de ejecución.
