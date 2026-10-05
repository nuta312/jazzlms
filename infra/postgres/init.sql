-- Выполняется при первом старте контейнера PostgreSQL.
-- У каждого сервиса СВОЯ база данных (database-per-service):
-- user-service не может сделать JOIN на таблицы course-service — только через API/gRPC/события.
CREATE DATABASE users_db;
CREATE DATABASE courses_db;
