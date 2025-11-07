FROM golang:1.21-bookworm

WORKDIR /app

# Install dependencies for ARM64
RUN apt-get update && apt-get install -y \
    gcc \
    musl-dev \
    && rm -rf /var/lib/apt/lists/*

COPY go.mod go.sum ./

# Download all dependencies
RUN go mod download && go mod verify

COPY . .

# Build for ARM64 (remove GOOS=linux GOARCH=arm64 as it's not needed in this context)
RUN go build -o producer

CMD ["./producer"]