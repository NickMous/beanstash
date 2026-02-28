# Fleet Repo Setup for Staging Branches

This document describes how to configure the fleet-infra repo to receive staging branch configurations from this repository.

## Overview

Staging branch configs are **derived from the overlay files** in this repo and committed to the fleet-infra repo by GitHub Actions. The overlay files (`k8s/overlays/staging/`) are the single source of truth — changes to ingress rules, certificates, middlewares, and patches automatically flow to all branch deployments.

The application consists of two deployments: a **Next.js frontend** (port 3000) and a **Spring Boot backend** (port 8080), each with their own service and ingress routes.

## Architecture

```
beanstash repo                      fleet-infra repo
--------------                      ----------------
k8s/                                apps/beanstash/
├── base/           ──(synced)──►   ├── base/           (copy of beanstash k8s/base)
└── overlays/                       └── staging-branches/
    └── staging/                        ├── kustomization.yaml  (root)
        ├── ingress.yaml                └── {branch-slug}/
        ├── certificate.yaml                ├── kustomization.yaml      (from template + envsubst)
        ├── middlewares.yaml                ├── kustomizeconfig.yaml    (static copy)
        ├── patches/                        ├── ingress.yaml            (from overlay + sed)
        │   ├── deployment-patch.yaml       ├── certificate.yaml        (from overlay + sed)
        │   ├── frontend-deployment-patch   ├── middlewares.yaml         (from overlay, as-is)
        │   └── configmap-patch.yaml        ├── secrets.yaml             (from template + envsubst)
        └── generator/                      └── patches/
            ├── kustomizeconfig.yaml            ├── deployment-patch.yaml           (from overlay, as-is)
            ├── kustomization.yaml.tmpl         ├── frontend-deployment-patch.yaml  (from overlay, as-is)
            └── secrets.yaml.tmpl               └── configmap-patch.yaml            (from overlay + sed)
```

### How files are generated

| Generated file | Source | Method |
|---|---|---|
| `kustomization.yaml` | `generator/kustomization.yaml.tmpl` | envsubst (`BRANCH_SLUG`, `COMMIT_SHA`) |
| `kustomizeconfig.yaml` | `generator/kustomizeconfig.yaml` | Static copy |
| `ingress.yaml` | `overlays/staging/ingress.yaml` | sed (hostname replacement) |
| `certificate.yaml` | `overlays/staging/certificate.yaml` | sed (hostname + TLS secretName replacement) |
| `middlewares.yaml` | `overlays/staging/middlewares.yaml` | Copy as-is |
| `secrets.yaml` | `generator/secrets.yaml.tmpl` | envsubst (`BRANCH_SLUG`) |
| `patches/deployment-patch.yaml` | `overlays/staging/patches/deployment-patch.yaml` | Copy as-is |
| `patches/frontend-deployment-patch.yaml` | `overlays/staging/patches/frontend-deployment-patch.yaml` | Copy as-is |
| `patches/configmap-patch.yaml` | `overlays/staging/patches/configmap-patch.yaml` | sed (hostname replacement) |

The sed pattern replaces `staging.beanstash.org` and `staging.api.beanstash.org` with `{branch-slug}.staging.beanstash.org` and `{branch-slug}.staging.api.beanstash.org`. An additional sed replaces the TLS `secretName` since it's referenced by cert-manager (not in the kustomization) and can't be handled by `nameReference`.

### Why nameReference (kustomizeconfig.yaml)

Kustomize's `namePrefix` only updates resource `metadata.name` by default. Traefik CRDs have inline references to Services, Middlewares, Secrets, and TLSOptions that also need updating. The `kustomizeconfig.yaml` teaches kustomize about these CRD-specific name reference fields so `namePrefix` works end-to-end.

## Setup Steps

### 1. Create GitHub Secret and Variable

**Create a Personal Access Token (PAT)** with write access to fleet-infra:

1. Go to GitHub Settings > Developer settings > Personal access tokens > Fine-grained tokens
2. Create new token with:
   - **Repository access:** `NickMous/fleet-infra`
   - **Permissions:** Contents (Read and write), Metadata (Read)
3. Add to beanstash repo as secret: `FLEET_REPO_TOKEN`

**Optional: Enable Flux webhook notifications**

If you have a Flux webhook receiver configured:
1. Add secret `FLUX_WEBHOOK_URL` with your webhook URL
2. Add repository variable `FLUX_WEBHOOK_ENABLED` set to `true`

(Go to repo Settings > Secrets and variables > Actions > Variables tab)

### 2. Create Directory Structure in fleet-infra

```bash
mkdir -p apps/beanstash/staging-branches
mkdir -p apps/beanstash/base  # Will be synced automatically by workflow
```

Note: `apps/beanstash/base/` is automatically synced from beanstash's `k8s/base/` by the GitHub Actions workflow on each push. The `generator/` directory is no longer needed in fleet-infra — templates now live in `k8s/overlays/staging/generator/` in the beanstash repo.

### 3. Create Initial Root Kustomization

Create `apps/beanstash/staging-branches/kustomization.yaml`:

```yaml
# Root kustomization for staging branches
# This file is auto-generated by GitHub Actions
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

resources:
  []
```

### 4. Update Flux Kustomization for Staging

Update (or create) the Flux Kustomization to point to the fleet repo:

```yaml
# clusters/home/beanstash-staging.yaml (or similar)
apiVersion: kustomize.toolkit.fluxcd.io/v1
kind: Kustomization
metadata:
  name: beanstash-staging
  namespace: flux-system
spec:
  interval: 5m
  prune: true
  sourceRef:
    kind: GitRepository
    name: flux-system  # Reference to fleet-infra repo
  path: ./apps/beanstash/staging-branches
  timeout: 3m
```

## How It Works

1. **Push to any branch** in beanstash repo triggers `deploy-staging.yaml`
2. Workflow builds Docker images for both backend and frontend, pushes to GHCR
3. Workflow checks out fleet-infra repo
4. **Derives** branch-specific configs from overlay files (ingress, certificate, middlewares, patches) using sed for hostname replacement
5. **Generates** kustomization and secrets from templates using envsubst
6. Commits configs to `apps/beanstash/staging-branches/{branch-slug}/`
7. Flux detects changes and deploys to cluster

**Key benefit:** Changes to overlay files (e.g. adding an IP to the allowlist, updating TLS options, adding a new middleware) automatically propagate to all branch deployments on next push.

**On branch deletion:**
1. `cleanup-staging.yaml` triggers
2. Removes branch config from fleet-infra repo
3. Flux prunes the resources from cluster

## Post-Migration Cleanup

After merging this change, delete the old directory from fleet-infra:

```bash
cd /path/to/fleet-infra
rm -rf apps/zcdb/
git add -A && git commit -m "chore: remove old zcdb configs (migrated to beanstash)"
git push
```

## Verification

After setup, test by pushing a feature branch:

```bash
# Check fleet-infra for new commit
git -C /path/to/fleet-infra log --oneline -5

# Check Flux reconciliation
flux get kustomization beanstash-staging

# Verify pods
kubectl get pods -n beanstash-staging

# Verify kustomize build works on generated branch dir
kustomize build apps/beanstash/staging-branches/{branch-slug}/
```
