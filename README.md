# Backend Suster — Microservice Architecture

Backend untuk aplikasi SusterKu, dibangun dengan Spring Boot + Docker.

## Services

| Service        | Port | Deskripsi                          |
|----------------|------|------------------------------------|
| api-gateway    | 8080 | Entry point, routing semua request |
| auth-service   | 8081 | Register, login, JWT               |
| nurse-service  | 8082 | Data suster, search, filter        |
| review-service | 8083 | Ulasan & rating suster             |
| PostgreSQL      | 5432 | Database (3 schema terpisah)       |

## Cara Menjalankan

Pastikan Docker Desktop aktif, lalu:

```bash
docker compose up --build
```

> Build pertama ~5-10 menit karena Maven mendownload dependencies di dalam Docker.

## API Endpoints

```
POST   /api/auth/login
POST   /api/auth/register

GET    /api/nurses?search=&available=
GET    /api/nurses/{id}

GET    /api/reviews/nurse/{nurseId}
POST   /api/reviews
```
