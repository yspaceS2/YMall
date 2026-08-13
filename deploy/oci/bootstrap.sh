#!/usr/bin/env bash

set -Eeuo pipefail

readonly OCI_CLI_VERSION="3.90.2"

sudo apt-get update
sudo apt-get install -y ca-certificates curl git python3-venv

sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
    -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

. /etc/os-release
architecture="$(dpkg --print-architecture)"
codename="${UBUNTU_CODENAME:-$VERSION_CODENAME}"

sudo tee /etc/apt/sources.list.d/docker.sources > /dev/null <<EOF
Types: deb
URIs: https://download.docker.com/linux/ubuntu
Suites: ${codename}
Components: stable
Architectures: ${architecture}
Signed-By: /etc/apt/keyrings/docker.asc
EOF

sudo apt-get update
sudo apt-get install -y \
    docker-ce \
    docker-ce-cli \
    containerd.io \
    docker-buildx-plugin \
    docker-compose-plugin \
    docker-model-plugin

sudo systemctl enable --now docker
sudo usermod -aG docker "$USER"

git --version
sudo docker version --format 'Docker Engine {{.Server.Version}}'
sudo docker compose version
sudo docker model version

if ! command -v oci > /dev/null 2>&1; then
    sudo python3 -m venv /opt/oci-cli
    sudo /opt/oci-cli/bin/pip install --disable-pip-version-check --no-cache-dir "oci-cli==${OCI_CLI_VERSION}"
    sudo ln -s /opt/oci-cli/bin/oci /usr/local/bin/oci
fi

oci --version

echo "Docker group membership takes effect after reconnecting the SSH session."
