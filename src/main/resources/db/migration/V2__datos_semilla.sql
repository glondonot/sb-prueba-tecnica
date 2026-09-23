-- Datos de ejemplo para probar el API de inmediato (Swagger / requests.http).
-- Sin ids explícitos para no desalinear las secuencias IDENTITY.

INSERT INTO polizas (numero, tipo, estado, tomador_documento, tomador_nombre, fecha_inicio_vigencia,
                     fecha_fin_vigencia, meses_vigencia, valor_canon, valor_prima, creado_en, actualizado_en)
VALUES ('IND-2026-0001', 'INDIVIDUAL', 'ACTIVA', '1020304050', 'Laura Gómez Rincón',
        DATE '2026-01-01', DATE '2027-01-01', 12, 1800000.00, 21600000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('IND-2026-0002', 'INDIVIDUAL', 'ACTIVA', '79888777', 'Andrés Felipe Torres',
        DATE '2026-03-15', DATE '2026-09-15', 6, 2500000.00, 15000000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('COL-2026-0001', 'COLECTIVA', 'ACTIVA', '900123456', 'Inmobiliaria Los Andes S.A.S.',
        DATE '2026-02-01', DATE '2027-02-01', 12, 3200000.00, 38400000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('COL-2025-0007', 'COLECTIVA', 'RENOVADA', '901555222', 'Administración Conjunto Parque Real P.H.',
        DATE '2026-07-01', DATE '2027-07-01', 12, 2080000.00, 24960000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('IND-2025-0099', 'INDIVIDUAL', 'CANCELADA', '52444333', 'Martha Lucía Pérez',
        DATE '2025-05-01', DATE '2026-05-01', 12, 1500000.00, 18000000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

UPDATE polizas SET fecha_cancelacion = CURRENT_TIMESTAMP WHERE estado = 'CANCELADA';

INSERT INTO riesgos (poliza_id, direccion_inmueble, ciudad, arrendatario_documento, arrendatario_nombre,
                     arrendador_documento, arrendador_nombre, estado, creado_en, actualizado_en)
SELECT p.id, r.direccion, r.ciudad, r.arr_doc, r.arr_nom, r.ador_doc, r.ador_nom, r.estado,
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (VALUES
        ('IND-2026-0001', 'Calle 93 # 15-20 Apto 402', 'Bogotá', '1020304050', 'Laura Gómez Rincón', '19333444', 'Carlos Méndez', 'ACTIVO'),
        ('IND-2026-0002', 'Carrera 43A # 1-50 Apto 1101', 'Medellín', '79888777', 'Andrés Felipe Torres', '43222111', 'Beatriz Uribe', 'ACTIVO'),
        ('COL-2026-0001', 'Avenida 19 # 120-35 Apto 301', 'Bogotá', '1015000111', 'Juan Pablo Ríos', '80111222', 'Ricardo Salazar', 'ACTIVO'),
        ('COL-2026-0001', 'Calle 140 # 11-45 Casa 12', 'Bogotá', '1015000222', 'Diana Carolina Ruiz', '80111333', 'Gloria Castaño', 'ACTIVO'),
        ('COL-2026-0001', 'Carrera 7 # 72-10 Oficina 501', 'Bogotá', '900777888', 'Soluciones Digitales S.A.S.', '80111444', 'Hernán Vargas', 'ACTIVO'),
        ('COL-2025-0007', 'Calle 5 # 38-25 Torre 2 Apto 804', 'Cali', '1144000555', 'Sebastián Mora', '31999888', 'Luz Marina Ortiz', 'ACTIVO'),
        ('COL-2025-0007', 'Calle 5 # 38-25 Torre 1 Apto 210', 'Cali', '1144000666', 'Valentina Cárdenas', '31999777', 'Jorge Iván Rojas', 'CANCELADO'),
        ('IND-2025-0099', 'Carrera 15 # 85-12 Apto 602', 'Bogotá', '52444333', 'Martha Lucía Pérez', '19555666', 'Fernando Díaz', 'CANCELADO')
     ) AS r (numero, direccion, ciudad, arr_doc, arr_nom, ador_doc, ador_nom, estado)
JOIN polizas p ON p.numero = r.numero
ORDER BY p.id, r.direccion;

UPDATE riesgos SET fecha_cancelacion = CURRENT_TIMESTAMP WHERE estado = 'CANCELADO';
