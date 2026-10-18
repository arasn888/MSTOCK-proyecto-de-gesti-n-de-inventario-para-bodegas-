/**
 * Angular habla SIEMPRE con el api-gateway (puerto 8080), nunca directo con los servicios.
 * El gateway ya tiene CORS para http://localhost:4200.
 */
export const environment = {
  apiUrl: 'http://localhost:8080',
};
