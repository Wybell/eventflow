# GitHub CI and Manual Production Deployment

`CI` runs automatically for every push to `main`, pull request targeting `main`, or manual workflow run. It checks the frontend lint, types, tests and build, plus backend Checkstyle, tests and packaging.

`Deploy Production` is deliberately separate. It has no push trigger. Open GitHub Actions, select **Deploy Production**, choose `frontend`, `backend`, or `all`, select the `main` branch, check `confirm`, and click **Run workflow**. The workflow refuses to proceed unless the selected `main` revision has successful frontend and backend CI checks.

## One-time Server Setup

The workflow expects the server checkout at `/opt/eventflow`, on the `main` branch, with no uncommitted changes. Its `deploy/.env` remains on the server and is never uploaded to GitHub. Docker Compose v2, Git, Curl and Bash must be installed for the SSH user.

Create a dedicated deploy key locally, then allow its public key to log in to the server account that owns `/opt/eventflow`:

```bash
ssh-keygen -t ed25519 -f ~/.ssh/eventflow-actions-deploy -C "eventflow-github-actions"
ssh-copy-id -i ~/.ssh/eventflow-actions-deploy.pub <server-user>@<server-host>
```

If `ssh-copy-id` is unavailable, append the public-key line to `~/.ssh/authorized_keys` for that server user. The key is only for GitHub Actions. Do not reuse a personal SSH key.

## GitHub Secrets

In the EventFlow repository, open **Settings -> Secrets and variables -> Actions** and create these repository secrets:

| Secret | Value |
| --- | --- |
| `EVENTFLOW_SSH_HOST` | Tencent Cloud public IP or hostname |
| `EVENTFLOW_SSH_PORT` | SSH port, normally `22` |
| `EVENTFLOW_SSH_USER` | Server user that owns `/opt/eventflow` and can run Docker |
| `EVENTFLOW_SSH_PRIVATE_KEY` | Complete contents of `~/.ssh/eventflow-actions-deploy` |
| `EVENTFLOW_SSH_KNOWN_HOSTS` | Output of `ssh-keyscan -H <server-host>` collected from a trusted network |

Never commit `deploy/.env`, SSH private keys, host keys, backups, or production passwords.

## Release Behavior

For `backend` and `all`, the server first runs `deploy/backup.sh`, then builds and restarts only the selected service scope. The workflow checks `http://127.0.0.1:EVENTFLOW_HTTP_PORT/actuator/health` and fails if it is not HTTP `200`. A failed deployment leaves the current Compose status and the last backend/frontend logs in the GitHub Actions output.

For a frontend-only release, no database backup or backend rebuild occurs. Flyway migrations run as part of the backend startup; database rollback must use a forward migration, not a destructive schema rollback.
