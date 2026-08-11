#!/usr/bin/env bash

set -Eeuo pipefail

readonly PROJECT_DIR="${YMALL_PROJECT_DIR:-/opt/ymall}"

sudo chmod 0755 \
    "${PROJECT_DIR}/deploy/oci/backup.sh" \
    "${PROJECT_DIR}/deploy/oci/verify-backup.sh" \
    "${PROJECT_DIR}/deploy/oci/install-backup-timer.sh"
sudo install -m 0644 \
    "${PROJECT_DIR}/deploy/oci/systemd/ymall-backup.service" \
    "${PROJECT_DIR}/deploy/oci/systemd/ymall-backup.timer" \
    /etc/systemd/system/
sudo install -d -m 0700 /opt/ymall-backups
sudo systemctl daemon-reload
sudo systemctl enable --now ymall-backup.timer
sudo systemctl list-timers ymall-backup.timer --no-pager
