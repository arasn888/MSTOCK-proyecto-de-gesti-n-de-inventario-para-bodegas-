# Frontend Angular: se compila en una etapa con Node y se sirve con nginx.
#   docker build -t mstock/web frontend/mstock-web

# ---------- Etapa 1: compilar ----------
FROM node:22-alpine AS construir
WORKDIR /web
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npx ng build --configuration production \
 && if [ -d dist/mstock-web/browser ]; then mv dist/mstock-web/browser /salida; else mv dist/mstock-web /salida; fi

# ---------- Etapa 2: nginx ----------
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=construir /salida /usr/share/nginx/html
EXPOSE 80
