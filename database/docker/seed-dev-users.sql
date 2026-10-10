-- SOLO DESARROLLO: usuarios de prueba para la Épica 1 (Jefe / Encargado / Personal).
-- Contraseña de todos: Gestor2026!   (hash BCrypt). No usar en entornos compartidos.
-- Docker lo ejecuta después de init-dev-db.sql en una base nueva. En una base existente:
--   docker compose exec -T db psql -U gestor_tareas_owner -d gestor_tareas -f /docker-entrypoint-initdb.d/zz-seed-dev-users.sql

\connect gestor_tareas gestor_tareas_owner

INSERT INTO task_manager.app_user (email, password_hash, given_names, family_names, is_chief, is_active)
VALUES
    ('jefe@gestor.local',       '$2a$10$8ZuR4anF/bgLtSLQ28BsYuuvRO2tGnQONdL.2bSk3hZnz2vFgbJQC', 'Julia', 'Mendoza', true,  true),
    ('encargado1@gestor.local', '$2a$10$8ZuR4anF/bgLtSLQ28BsYuuvRO2tGnQONdL.2bSk3hZnz2vFgbJQC', 'Ana',   'Paz',     false, true),
    ('encargado2@gestor.local', '$2a$10$8ZuR4anF/bgLtSLQ28BsYuuvRO2tGnQONdL.2bSk3hZnz2vFgbJQC', 'Luis',  'Vaca',    false, true),
    ('personal@gestor.local',   '$2a$10$8ZuR4anF/bgLtSLQ28BsYuuvRO2tGnQONdL.2bSk3hZnz2vFgbJQC', 'Pedro', 'Rojas',   false, true),
    ('inactivo@gestor.local',   '$2a$10$8ZuR4anF/bgLtSLQ28BsYuuvRO2tGnQONdL.2bSk3hZnz2vFgbJQC', 'Iván',  'Soto',    false, false)
ON CONFLICT (email) DO NOTHING;
