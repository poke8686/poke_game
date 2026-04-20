FROM node:20-alpine
WORKDIR /app
COPY . .
EXPOSE 3000
CMD sh -c 'cd /app && npm install --production && node src/server.js'
