# =================================================================
# TukangBaik Backend — Makefile
# =================================================================
# Cara pakai:
#   make dev          → jalankan semua service (dev)
#   make prod         → jalankan semua service (production)
#   make down         → stop semua container
#   make build-dev    → build ulang image (dev)
#   make build-prod   → build ulang image (production)
#   make logs         → lihat log semua service
#   make logs s=auth-service  → log service spesifik
#   make ps           → status semua container
# =================================================================

COMPOSE_BASE = docker compose -f docker-compose.yml
COMPOSE_PROD = $(COMPOSE_BASE) -f docker-compose.prod.yml

.PHONY: dev prod down build-dev build-prod logs ps restart-dev restart-prod

# ── Development ───────────────────────────────────────────────────

dev:
	$(COMPOSE_BASE) up -d

build-dev:
	$(COMPOSE_BASE) build

restart-dev:
	$(COMPOSE_BASE) up -d --build

# ── Production ────────────────────────────────────────────────────

prod:
	@test -f .env.prod || (echo "ERROR: .env.prod tidak ditemukan! Copy dari .env.prod.example dan isi nilainya." && exit 1)
	$(COMPOSE_PROD) up -d

build-prod:
	$(COMPOSE_PROD) build

restart-prod:
	$(COMPOSE_PROD) up -d --build

# ── Shared ────────────────────────────────────────────────────────

down:
	$(COMPOSE_BASE) down

logs:
	$(COMPOSE_BASE) logs -f $(s)

ps:
	$(COMPOSE_BASE) ps
