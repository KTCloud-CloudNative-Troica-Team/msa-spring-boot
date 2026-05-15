# 0008 — gRPC plaintext + mesh-terminated mTLS

## Status
Accepted — 2026-04

## Context
Audit BV-7 flagged that all gRPC clients are configured `negotiation-type: plaintext`.

## Decision
- In-process gRPC stays plaintext.
- mTLS is **expected at the Service Mesh layer** (Istio sidecar / Linkerd
  proxy / Cilium). Application code never touches certificates.
- Local dev runs without a mesh — `localhost` traffic is plaintext by design.

## Consequences
- Application code is mesh-agnostic; switching mesh vendor doesn't touch Spring config.
- Deploying to a cluster *without* a mesh is unsafe — that has to be enforced
  at the deployment-platform layer (NetworkPolicy denying non-mesh traffic).
- Re-evaluate this ADR if we ever need to run a service outside a mesh
  (then add `grpc.client.<name>.security.client-auth-enabled` and mount certs).
