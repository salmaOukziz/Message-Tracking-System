FROM golang:1.19-alpine

WORKDIR /app

# Installer curl pour healthcheck
RUN apk add --no-cache curl

# Copier les fichiers modules d'abord
COPY go.mod ./
RUN go mod download

# Copier le code source
COPY *.go ./

# Builder l'application
RUN go build -o message-api .

EXPOSE 8080

# Healthcheck
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/api/v1/health || exit 1

CMD ["./message-api"]