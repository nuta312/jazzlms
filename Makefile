# Короткие команды для студентов. `make help` — список.

.PHONY: help infra infra-down build up down logs test clean front

help:
	@echo "make infra      - поднять только инфраструктуру (Postgres, Mongo, Redis, Kafka, Zipkin)"
	@echo "make build      - собрать все Java-сервисы (jar) и прогнать тесты"
	@echo "make up         - собрать и поднять ВСЁ в docker (инфра + сервисы + фронт)"
	@echo "make down       - остановить всё и удалить данные"
	@echo "make logs       - хвост логов всех контейнеров"
	@echo "make test       - только тесты"
	@echo "make front      - запустить фронт в dev-режиме (Vite, localhost:5173)"
	@echo "make grafana    - открыть Grafana с логами и метриками (localhost:3001)"

infra:
	docker compose up -d

infra-down:
	docker compose down

build:
	./gradlew build

test:
	./gradlew test

up: build
	docker compose --profile app up -d --build

down:
	docker compose --profile app down -v

logs:
	docker compose --profile app logs -f --tail=100

front:
	cd frontend && npm install && npm run dev

grafana:
	open http://localhost:3001/d/jazzlms-overview

clean:
	./gradlew clean
	rm -rf frontend/node_modules frontend/dist
