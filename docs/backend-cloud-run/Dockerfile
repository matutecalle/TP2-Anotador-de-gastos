# Imagen del contenedor para Cloud Run (configuración objetivo, no desplegada).
FROM node:20-slim

WORKDIR /app

# Primero solo package.json: si el código cambia pero las dependencias no,
# Docker reutiliza esta capa y el build es más rápido.
COPY package*.json ./
RUN npm install --omit=dev

COPY server.js ./

# No correr como root dentro del contenedor.
USER node

ENV NODE_ENV=production
# Cloud Run inyecta PORT; 8080 es el valor por defecto.
ENV PORT=8080
EXPOSE 8080

CMD ["node", "server.js"]
