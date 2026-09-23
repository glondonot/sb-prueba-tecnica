"""
Benchmark del Módulo 3: optimización de la consulta de pedidos de clientes de 'México'.

Genera 500.000 clientes y 10.000.000 de pedidos en PostgreSQL 16 y mide, con
EXPLAIN (ANALYZE, BUFFERS), el efecto de cada estrategia de optimización:

  E0  Línea base: solo llaves primarias (la FK orders.customer_id NO tiene índice).
  E1  Estadísticas: estimación del planificador antes y después de ANALYZE.
  E2  Índices compuestos/cubrientes en customers y orders (index-only scans).
  E3  Vista materializada con índice por país.
  E4  Particionamiento por rango de order_date + filtro de fecha (partition pruning).
  E5  Paginación: OFFSET vs keyset sobre el índice de la vista materializada.

Uso:
    docker compose up -d                       # PostgreSQL de benchmark en el puerto 55432
    python -m venv .venv && .venv/Scripts/pip install -r requirements.txt   (Linux/macOS: .venv/bin/pip)
    .venv/Scripts/python benchmark.py          # ~5-10 min; resultados en ./resultados

Todos los tiempos reportados son medidos (mediana de N ejecuciones en caliente).
"""

from __future__ import annotations

import argparse
import json
import platform
import statistics
import time
from dataclasses import asdict, dataclass
from datetime import datetime
from pathlib import Path

import psycopg

DSN_POR_DEFECTO = "postgresql://bench:bench@localhost:55432/benchmark"
RESULTADOS = Path(__file__).parent / "resultados"

CONSULTA_ORIGINAL = """
SELECT o.order_id, o.order_date, c.customer_name, o.total_amount
FROM orders o
JOIN customers c ON o.customer_id = c.customer_id
WHERE c.country = 'México'
"""


@dataclass
class Medicion:
    escenario: str
    descripcion: str
    sql: str
    ejecuciones_ms: list[float]
    filas: int
    buffers_hit: int
    buffers_read: int
    nodos: list[str]

    @property
    def mediana_ms(self) -> float:
        return statistics.median(self.ejecuciones_ms)


def log(msg: str) -> None:
    print(f"[{datetime.now():%H:%M:%S}] {msg}", flush=True)


def ejecutar(conn: psycopg.Connection, sql: str) -> float:
    inicio = time.perf_counter()
    conn.execute(sql)
    return time.perf_counter() - inicio


def recorrer_nodos(plan: dict) -> list[str]:
    nombre = plan["Node Type"]
    if "Relation Name" in plan:
        nombre += f" on {plan['Relation Name']}"
    if "Index Name" in plan:
        nombre += f" using {plan['Index Name']}"
    nodos = [nombre]
    for hijo in plan.get("Plans", []):
        nodos.extend(recorrer_nodos(hijo))
    return nodos


def estimaciones(conn: psycopg.Connection) -> tuple[float, float, list[str]]:
    """Filas estimadas por el planificador (sin paralelismo, para que la estimación sea total y no por worker)."""
    conn.execute("SET max_parallel_workers_per_gather = 0")
    try:
        clientes = conn.execute("EXPLAIN (FORMAT JSON) SELECT customer_id FROM customers WHERE country = 'México'"
                                ).fetchone()[0][0]["Plan"]["Plan Rows"]
        plan = conn.execute(f"EXPLAIN (FORMAT JSON) {CONSULTA_ORIGINAL}").fetchone()[0][0]["Plan"]
        return clientes, plan["Plan Rows"], recorrer_nodos(plan)
    finally:
        conn.execute("RESET max_parallel_workers_per_gather")


def medir(conn: psycopg.Connection, escenario: str, descripcion: str, sql: str,
          repeticiones: int, calentamiento: int = 1) -> Medicion:
    explain = f"EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) {sql}"
    for _ in range(calentamiento):
        conn.execute(explain).fetchone()
    tiempos, ultimo = [], None
    for _ in range(repeticiones):
        ultimo = conn.execute(explain).fetchone()[0][0]
        tiempos.append(round(ultimo["Execution Time"], 2))
    raiz = ultimo["Plan"]
    plan_texto = "\n".join(
        fila[0] for fila in conn.execute(f"EXPLAIN (ANALYZE, BUFFERS, COSTS OFF, TIMING OFF) {sql}").fetchall())
    (RESULTADOS / "planes").mkdir(parents=True, exist_ok=True)
    (RESULTADOS / "planes" / f"{escenario}.txt").write_text(
        f"-- {descripcion}\n{sql.strip()}\n\n{plan_texto}\n", encoding="utf-8")
    medicion = Medicion(
        escenario=escenario,
        descripcion=descripcion,
        sql=sql.strip(),
        ejecuciones_ms=tiempos,
        filas=raiz["Actual Rows"],
        buffers_hit=raiz.get("Shared Hit Blocks", 0),
        buffers_read=raiz.get("Shared Read Blocks", 0),
        nodos=recorrer_nodos(raiz),
    )
    log(f"{escenario}: mediana {medicion.mediana_ms:,.1f} ms · {medicion.filas:,} filas · {medicion.nodos[:4]}")
    return medicion


def tiempo_cliente(conn: psycopg.Connection, sql: str, repeticiones: int = 3) -> float:
    """Tiempo de extremo a extremo trayendo TODAS las filas al cliente (red + serialización)."""
    tiempos = []
    for _ in range(repeticiones):
        inicio = time.perf_counter()
        with conn.cursor() as cur:
            cur.execute(sql)
            cur.fetchall()
        tiempos.append((time.perf_counter() - inicio) * 1000)
    return round(statistics.median(tiempos), 1)


def generar_datos(conn: psycopg.Connection, clientes: int, pedidos: int) -> dict:
    log("Creando esquema (solo PKs; FK sin índice, como en muchos esquemas reales)")
    conn.execute("""
        DROP MATERIALIZED VIEW IF EXISTS mv_orders_by_country;
        DROP TABLE IF EXISTS orders_part, orders, customers CASCADE;
        CREATE TABLE customers (
            customer_id   integer PRIMARY KEY,
            customer_name varchar(120) NOT NULL,
            country       varchar(60)  NOT NULL
        ) WITH (autovacuum_enabled = false);
        CREATE TABLE orders (
            order_id     bigint PRIMARY KEY,
            customer_id  integer       NOT NULL,
            order_date   date          NOT NULL,
            total_amount numeric(12,2) NOT NULL
        ) WITH (autovacuum_enabled = false);
    """)
    tiempos = {}
    log(f"Generando {clientes:,} clientes (≈4 % de México)")
    conn.execute("SELECT setseed(0.42)")
    tiempos["clientes_s"] = ejecutar(conn, f"""
        INSERT INTO customers
        SELECT g,
               'Cliente ' || g,
               CASE
                   WHEN r < 0.04 THEN 'México'
                   WHEN r < 0.34 THEN 'Colombia'
                   WHEN r < 0.49 THEN 'Perú'
                   WHEN r < 0.62 THEN 'Chile'
                   WHEN r < 0.74 THEN 'Argentina'
                   WHEN r < 0.84 THEN 'Ecuador'
                   WHEN r < 0.92 THEN 'Panamá'
                   ELSE 'España'
               END
        FROM (SELECT g, random() AS r FROM generate_series(1, {clientes}) g) s
    """)
    log(f"Generando {pedidos:,} pedidos (2021-2026)")
    tiempos["pedidos_s"] = ejecutar(conn, f"""
        INSERT INTO orders
        SELECT g,
               1 + floor(random() * {clientes})::int,
               DATE '2021-01-01' + floor(random() * 2100)::int,
               round((10 + random() * 4990)::numeric, 2)
        FROM generate_series(1, {pedidos}) g
    """)
    log("Agregando FK orders.customer_id -> customers (PostgreSQL NO crea índice para la FK)")
    tiempos["fk_s"] = ejecutar(conn, """
        ALTER TABLE orders ADD CONSTRAINT fk_orders_customer
            FOREIGN KEY (customer_id) REFERENCES customers (customer_id)
    """)
    return {k: round(v, 1) for k, v in tiempos.items()}


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--dsn", default=DSN_POR_DEFECTO)
    parser.add_argument("--clientes", type=int, default=500_000)
    parser.add_argument("--pedidos", type=int, default=10_000_000)
    parser.add_argument("--repeticiones", type=int, default=5)
    args = parser.parse_args()

    RESULTADOS.mkdir(exist_ok=True)
    mediciones: list[Medicion] = []
    extra: dict = {}

    with psycopg.connect(args.dsn, autocommit=True) as conn:
        version = conn.execute("SHOW server_version").fetchone()[0]
        extra["carga"] = generar_datos(conn, args.clientes, args.pedidos)

        # ---------------- E1: estadísticas ----------------
        log("E1: estimación del planificador SIN estadísticas (tablas recién cargadas)")
        sin_stats = estimaciones(conn)
        conn.execute("VACUUM (ANALYZE) customers")
        conn.execute("VACUUM (ANALYZE) orders")
        con_stats = estimaciones(conn)
        reales = conn.execute(f"SELECT (SELECT count(*) FROM customers WHERE country = 'México'), "
                              f"(SELECT count(*) FROM ({CONSULTA_ORIGINAL}) t)").fetchone()
        extra["estadisticas"] = {
            "clientes_mexico": {"sin_analyze": sin_stats[0], "con_analyze": con_stats[0], "real": reales[0]},
            "filas_resultado": {"sin_analyze": sin_stats[1], "con_analyze": con_stats[1], "real": reales[1]},
            "plan_sin_analyze": sin_stats[2],
            "plan_con_analyze": con_stats[2],
        }
        log(f"E1: clientes MX estimados sin/con ANALYZE={sin_stats[0]:,.0f}/{con_stats[0]:,.0f} (real {reales[0]:,}) · "
            f"filas resultado={sin_stats[1]:,.0f}/{con_stats[1]:,.0f} (real {reales[1]:,})")

        tamanos = conn.execute("""
            SELECT pg_size_pretty(pg_total_relation_size('orders')),
                   pg_size_pretty(pg_total_relation_size('customers'))
        """).fetchone()
        extra["tamano_orders"], extra["tamano_customers"] = tamanos

        # ---------------- E0: línea base ----------------
        mediciones.append(medir(conn, "E0_linea_base",
                                "Línea base: solo PKs, FK sin índice, estadísticas al día",
                                CONSULTA_ORIGINAL, args.repeticiones))
        extra["cliente_ms_linea_base"] = tiempo_cliente(conn, CONSULTA_ORIGINAL)

        # ---------------- E2: índices ----------------
        log("E2: creando índices cubrientes")
        t_idx = ejecutar(conn, """
            CREATE INDEX ix_customers_country ON customers (country) INCLUDE (customer_id, customer_name);
            CREATE INDEX ix_orders_customer_id ON orders (customer_id) INCLUDE (order_id, order_date, total_amount);
        """)
        conn.execute("VACUUM (ANALYZE) orders")  # visibility map al día -> index-only scans
        conn.execute("VACUUM (ANALYZE) customers")
        extra["indices_creacion_s"] = round(t_idx, 1)
        extra["tamano_ix_orders"] = conn.execute(
            "SELECT pg_size_pretty(pg_relation_size('ix_orders_customer_id'))").fetchone()[0]
        mediciones.append(medir(conn, "E2_indices",
                                "Índices cubrientes: customers(country) INCLUDE (...) y orders(customer_id) INCLUDE (...)",
                                CONSULTA_ORIGINAL, args.repeticiones))
        extra["cliente_ms_indices"] = tiempo_cliente(conn, CONSULTA_ORIGINAL)

        # ---------------- E3: vista materializada ----------------
        log("E3: creando vista materializada")
        t_mv = ejecutar(conn, """
            CREATE MATERIALIZED VIEW mv_orders_by_country AS
            SELECT o.order_id, o.order_date, o.total_amount, c.customer_name, c.country
            FROM orders o JOIN customers c ON o.customer_id = c.customer_id;
            CREATE UNIQUE INDEX ux_mv_orders_order_id ON mv_orders_by_country (order_id);
            CREATE INDEX ix_mv_orders_country_fecha ON mv_orders_by_country (country, order_date DESC, order_id DESC)
                INCLUDE (customer_name, total_amount);
        """)
        conn.execute("VACUUM (ANALYZE) mv_orders_by_country")
        extra["mv_creacion_s"] = round(t_mv, 1)
        consulta_mv = """
        SELECT order_id, order_date, customer_name, total_amount
        FROM mv_orders_by_country
        WHERE country = 'México'
        """
        mediciones.append(medir(conn, "E3_vista_materializada",
                                "Vista materializada con índice (country, order_date, order_id)",
                                consulta_mv, args.repeticiones))
        log("E3: midiendo REFRESH MATERIALIZED VIEW CONCURRENTLY (costo de mantenerla)")
        extra["mv_refresh_concurrently_s"] = round(
            ejecutar(conn, "REFRESH MATERIALIZED VIEW CONCURRENTLY mv_orders_by_country"), 1)

        # ---------------- E4: particionamiento ----------------
        log("E4: creando orders_part particionada por año de order_date")
        particiones = "\n".join(
            f"CREATE TABLE orders_p{a} PARTITION OF orders_part "
            f"FOR VALUES FROM ('{a}-01-01') TO ('{a + 1}-01-01');" for a in range(2021, 2027))
        t_part = ejecutar(conn, f"""
            CREATE TABLE orders_part (LIKE orders INCLUDING DEFAULTS) PARTITION BY RANGE (order_date);
            {particiones}
            INSERT INTO orders_part SELECT * FROM orders;
            CREATE INDEX ix_orders_part_customer ON orders_part (customer_id) INCLUDE (order_id, order_date, total_amount);
        """)
        conn.execute("VACUUM (ANALYZE) orders_part")
        extra["particion_creacion_s"] = round(t_part, 1)
        filtro_fecha = "AND o.order_date >= DATE '2026-01-01'"
        mediciones.append(medir(conn, "E4a_fecha_sin_particion",
                                "Consulta con filtro de fecha (último año) sobre orders con índices de E2",
                                CONSULTA_ORIGINAL + filtro_fecha, args.repeticiones))
        mediciones.append(medir(conn, "E4b_fecha_con_particion",
                                "Misma consulta sobre orders_part: partition pruning (solo lee 2026)",
                                CONSULTA_ORIGINAL.replace("FROM orders o", "FROM orders_part o") + filtro_fecha,
                                args.repeticiones))

        # ---------------- E5: paginación ----------------
        total_mx = conn.execute(f"SELECT count(*) FROM ({consulta_mv}) t").fetchone()[0]
        offset = min(100_000, total_mx // 2)
        orden = " ORDER BY order_date DESC, order_id DESC"
        mediciones.append(medir(conn, "E5a_pagina_offset",
                                f"Página profunda con OFFSET {offset:,} LIMIT 50 (lee y descarta {offset:,} filas)",
                                consulta_mv + orden + f" OFFSET {offset} LIMIT 50", args.repeticiones))
        ancla = conn.execute(consulta_mv + orden + f" OFFSET {offset - 1} LIMIT 1").fetchone()
        mediciones.append(medir(conn, "E5b_pagina_keyset",
                                "Misma página con keyset: WHERE (order_date, order_id) < (último visto) LIMIT 50",
                                consulta_mv + f" AND (order_date, order_id) < (DATE '{ancla[1]}', {ancla[0]})"
                                + orden + " LIMIT 50", args.repeticiones))

    resumen = {
        "fecha": datetime.now().isoformat(timespec="seconds"),
        "postgres": version,
        "equipo": f"{platform.system()} {platform.release()} · {platform.processor()}",
        "volumen": {"clientes": args.clientes, "pedidos": args.pedidos},
        "repeticiones": args.repeticiones,
        "extra": extra,
        "mediciones": [asdict(m) | {"mediana_ms": m.mediana_ms} for m in mediciones],
    }
    (RESULTADOS / "resultados.json").write_text(json.dumps(resumen, indent=2, ensure_ascii=False), encoding="utf-8")
    escribir_markdown(resumen)
    log(f"Listo. Resultados en {RESULTADOS}")


def escribir_markdown(r: dict) -> None:
    base = r["mediciones"][0]["mediana_ms"]
    filas = []
    for m in r["mediciones"]:
        mejora = base / m["mediana_ms"] if m["mediana_ms"] else float("inf")
        filas.append(f"| {m['escenario']} | {m['descripcion']} | {m['mediana_ms']:,.1f} | {m['filas']:,} "
                     f"| {m['buffers_hit'] + m['buffers_read']:,} | {mejora:,.1f}x |")
    e = r["extra"]
    est = e["estadisticas"]
    md = f"""# Resultados del benchmark – Módulo 3

- Fecha: {r['fecha']} · PostgreSQL {r['postgres']} (Docker) · {r['equipo']}
- Volumen: {r['volumen']['clientes']:,} clientes · {r['volumen']['pedidos']:,} pedidos
  (orders: {e['tamano_orders']}, customers: {e['tamano_customers']})
- Tiempo = mediana de {r['repeticiones']} ejecuciones de `EXPLAIN (ANALYZE, BUFFERS)` en caliente (tiempo en servidor).
- "Mejora" se calcula contra la línea base E0 (en E4/E5 la consulta cambia; comparar entre sí a y b).

| Escenario | Descripción | Mediana (ms) | Filas | Buffers | Mejora vs E0 |
|---|---|---:|---:|---:|---:|
{chr(10).join(filas)}

## Estadísticas del planificador (E1)

| | Estimado sin `ANALYZE` | Estimado con `ANALYZE` | Real |
|---|---:|---:|---:|
| Clientes de México | {est['clientes_mexico']['sin_analyze']:,.0f} | {est['clientes_mexico']['con_analyze']:,.0f} | {est['clientes_mexico']['real']:,} |
| Filas del resultado | {est['filas_resultado']['sin_analyze']:,.0f} | {est['filas_resultado']['con_analyze']:,.0f} | {est['filas_resultado']['real']:,} |

- Plan elegido sin estadísticas: `{' → '.join(est['plan_sin_analyze'])}`
- Plan elegido con estadísticas: `{' → '.join(est['plan_con_analyze'])}`

## Costos asociados

| Concepto | Valor |
|---|---|
| Crear índices de E2 | {e['indices_creacion_s']} s (índice de orders: {e['tamano_ix_orders']}) |
| Crear vista materializada + índices | {e['mv_creacion_s']} s |
| `REFRESH MATERIALIZED VIEW CONCURRENTLY` | {e['mv_refresh_concurrently_s']} s |
| Crear tabla particionada + copia + índice | {e['particion_creacion_s']} s |
| Traer todas las filas al cliente – línea base | {e['cliente_ms_linea_base']:,.1f} ms |
| Traer todas las filas al cliente – con índices | {e['cliente_ms_indices']:,.1f} ms |

Los planes de ejecución completos de cada escenario están en `planes/`.
"""
    (RESULTADOS / "RESULTADOS.md").write_text(md, encoding="utf-8")


if __name__ == "__main__":
    main()
