FROM mcr.microsoft.com/devcontainers/java:1-21-bullseye

USER root
# Remove the base image's Yarn APT entry, whose stale key breaks feature installs.
RUN for source in /etc/apt/sources.list /etc/apt/sources.list.d/*; do \
        [ -f "$source" ] || continue; \
        if grep -q 'dl.yarnpkg.com' "$source"; then \
            rm -f "$source"; \
        fi; \
    done
USER vscode
