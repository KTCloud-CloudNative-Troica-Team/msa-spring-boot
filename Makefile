SHELL := /bin/bash

GRADLE        := ./gradlew
COMPOSE_FILE  := container-compose.yaml
LOG_DIR       := build/run-logs
PID_DIR       := build/run-pids

SERVICES := auth-service product-service inventory-service order-service user-api-gateway admin-api-gateway

.PHONY: help up down infra-up infra-down generate start stop nuke restart status logs clean-logs $(SERVICES) $(addprefix stop-,$(SERVICES))

help:
	@echo "Targets:"
	@echo "  make up              - Start middleware + generate sources + start every service"
	@echo "  make down            - Stop every service and middleware"
	@echo "  make infra-up        - Start Postgres / Redis / Kafka via docker-compose"
	@echo "  make infra-down      - Stop docker-compose middleware"
	@echo "  make generate        - Run generateProto + kaptKotlin"
	@echo "  make start           - Start every Spring Boot service in background"
	@echo "  make stop            - Stop every Spring Boot service started by 'make start'"
	@echo "  make nuke            - Kill every bootRun / gradle daemon for this repo (emergency)"
	@echo "  make restart         - stop + start"
	@echo "  make status          - Show running services with their PIDs"
	@echo "  make logs            - Tail logs of every running service"
	@echo "  make clean-logs      - Remove $(LOG_DIR) and $(PID_DIR)"
	@echo "  make <service>       - Start a single service in background ($(SERVICES))"
	@echo "  make stop-<service>  - Stop a single service"

up: infra-up generate start status

down: stop infra-down

infra-up:
	docker-compose -f $(COMPOSE_FILE) up -d

infra-down:
	docker-compose -f $(COMPOSE_FILE) down

generate:
	$(GRADLE) generateProto kaptKotlin

$(LOG_DIR) $(PID_DIR):
	@mkdir -p $@

start: $(SERVICES)

$(SERVICES): | $(LOG_DIR) $(PID_DIR)
	@if [ -f $(PID_DIR)/$@.pid ] && kill -0 $$(cat $(PID_DIR)/$@.pid) 2>/dev/null; then \
		echo "[$@] already running (pid $$(cat $(PID_DIR)/$@.pid))"; \
	else \
		echo "[$@] starting -> $(LOG_DIR)/$@.log"; \
		nohup $(GRADLE) :$@:bootRun > $(LOG_DIR)/$@.log 2>&1 & \
		echo $$! > $(PID_DIR)/$@.pid; \
	fi

stop: $(addprefix stop-,$(SERVICES))

stop-%:
	@echo "[$*] stopping (matching :$*:bootRun)"
	@if [ -f $(PID_DIR)/$*.pid ]; then \
		pid=$$(cat $(PID_DIR)/$*.pid); \
		pkill -TERM -P $$pid 2>/dev/null || true; \
		kill $$pid 2>/dev/null || true; \
		rm -f $(PID_DIR)/$*.pid; \
	fi
	@pkill -TERM -f ":$*:bootRun" 2>/dev/null || true
	@pkill -TERM -f "black.$(subst -service,,$*)" 2>/dev/null || true
	@sleep 1
	@pkill -KILL -f ":$*:bootRun" 2>/dev/null || true
	@pkill -KILL -f "black.$(subst -service,,$*)" 2>/dev/null || true

restart: stop start

nuke:
	@echo "killing every bootRun / black.* java process for this repo"
	@pkill -TERM -f "bootRun" 2>/dev/null || true
	@pkill -TERM -f "dev.ktcloud.black" 2>/dev/null || true
	@sleep 1
	@pkill -KILL -f "bootRun" 2>/dev/null || true
	@pkill -KILL -f "dev.ktcloud.black" 2>/dev/null || true
	@rm -f $(PID_DIR)/*.pid 2>/dev/null || true
	@echo "done."

status:
	@for s in $(SERVICES); do \
		if [ -f $(PID_DIR)/$$s.pid ] && kill -0 $$(cat $(PID_DIR)/$$s.pid) 2>/dev/null; then \
			echo "  RUNNING  $$s  (pid $$(cat $(PID_DIR)/$$s.pid))"; \
		else \
			echo "  STOPPED  $$s"; \
		fi; \
	done

logs:
	@if ls $(LOG_DIR)/*.log >/dev/null 2>&1; then \
		tail -F $(LOG_DIR)/*.log; \
	else \
		echo "No logs in $(LOG_DIR)"; \
	fi

clean-logs:
	rm -rf $(LOG_DIR) $(PID_DIR)
