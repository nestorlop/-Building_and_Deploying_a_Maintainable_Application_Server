# Building and Deploying a Maintainable Application Server

Laboratorio de Ingeniería de Sistemas — Escuela Colombiana de Ingeniería Julio Garavito

## Descripción

En este laboratorio se tomó un servidor HTTP básico desarrollado anteriormente en Java y se fue mejorando hasta convertirlo en un pequeño framework web.

La idea principal fue dejar de tener las rutas directamente dentro del servidor y crear una estructura que permita registrar endpoints de una forma más sencilla, por ejemplo usando lambdas:

```java
framework.get("/hello", (req, resp) -> {
    // lógica del endpoint
});
```

Además, el servidor puede manejar archivos estáticos, parámetros de consulta, variables de entorno y un apagado controlado.

El servidor funciona de manera **secuencial**, es decir, no utiliza múltiples hilos ni concurrencia, ya que esto hace parte de los requisitos del laboratorio.

### Tecnologías utilizadas

* Java 17
* Maven
* JDK estándar
* Sockets TCP
* HTML, CSS y JavaScript para la interfaz de prueba
* AWS para el despliegue

No se utilizó Spring Boot ni ningún otro framework web externo.

---

## Inicio rápido

### Requisitos

Para ejecutar el proyecto se necesita:

* Java 17 o superior
* Maven 3.8 o superior

### Compilar el proyecto

Desde la carpeta principal:

```bash
mvn clean package
```

Esto genera el JAR dentro de la carpeta `target/`.

### Ejecutar

```bash
java -jar target/httpserver-1.0-SNAPSHOT.jar
```

Por defecto, el servidor queda disponible en:

```text
http://localhost:8080
```

El puerto puede cambiarse mediante la variable de entorno `PORT`.

---

## Variables de entorno

El proyecto utiliza algunas variables de entorno para poder cambiar su comportamiento sin modificar el código.

| Variable          | Valor por defecto | Uso                              |
| ----------------- | ----------------- | -------------------------------- |
| `PORT`            | `8080`            | Puerto donde escucha el servidor |
| `APP_ENV`         | `development`     | Define el ambiente de ejecución  |
| `GREETING_PREFIX` | `Hello`           | Prefijo utilizado en `/hello`    |

Por ejemplo:

### Cambiar el puerto

```bash
PORT=9090 java -jar target/httpserver-1.0-SNAPSHOT.jar
```

### Ejecutar en producción

```bash
APP_ENV=production java -jar target/httpserver-1.0-SNAPSHOT.jar
```

En este ambiente el endpoint `/shutdown` no se registra.

### Cambiar el saludo

```bash
GREETING_PREFIX=Hola java -jar target/httpserver-1.0-SNAPSHOT.jar
```

Con esto:

```text
/hello?name=Nestor
```

responde:

```text
Hola Nestor
```

También se pueden combinar las variables:

```bash
PORT=8080 APP_ENV=production GREETING_PREFIX=Hola java -jar target/httpserver-1.0-SNAPSHOT.jar
```

---

# Arquitectura

La estructura del proyecto se separó en varias clases para que cada una tenga una responsabilidad específica.

```text
                         HttpServer
                             |
                             v
                       WebFramework
                             |
              +--------------+--------------+
              |              |              |
              v              v              v
            Router     StaticFileService  Lifecycle
              |
       +------+------+
       |             |
       v             v
    /hello           /pi
    lambda          lambda
```

El flujo general es:

1. `HttpServer` abre el `ServerSocket`.
2. Espera una conexión mediante `accept()`.
3. Recibe y analiza el request HTTP.
4. `WebFramework` busca una ruta registrada.
5. Si existe una ruta dinámica, ejecuta su lambda.
6. Si no existe, intenta buscar un archivo estático.
7. Si tampoco existe el recurso, devuelve `404 Not Found`.
8. La respuesta se construye mediante `Response`.

---

## Clases principales

| Clase               | Función                                                                                                          |
| ------------------- | ---------------------------------------------------------------------------------------------------------------- |
| `HttpServer`        | Abre el `ServerSocket` y maneja el ciclo principal del servidor.                                                 |
| `WebFramework`      | Es la parte principal del framework y permite registrar rutas, archivos estáticos e iniciar/detener el servidor. |
| `Router`            | Guarda y busca las rutas registradas.                                                                            |
| `StaticFileService` | Busca y sirve archivos dentro de `webroot`. También controla el acceso a rutas fuera de esa carpeta.             |
| `Request`           | Procesa la petición HTTP y permite obtener parámetros, headers, método y ruta.                                   |
| `Response`          | Construye la respuesta HTTP, incluyendo status, headers, tipo de contenido y body.                               |
| `WebService`        | Interfaz funcional utilizada para poder registrar handlers mediante lambdas.                                     |
| `Application`       | Punto de entrada de la aplicación. Aquí se configuran las rutas y se inicia el servidor.                         |

---

# API del framework

La idea del framework es poder registrar rutas sin tener que modificar directamente el funcionamiento interno del servidor.

Por ejemplo:

```java
WebFramework framework = new WebFramework();

framework.staticfiles("/webroot");

framework.get("/hello", (req, resp) -> {
    String name = req.getValue("name");

    if (name == null) {
        name = "world";
    }

    String prefix = System.getenv()
            .getOrDefault("GREETING_PREFIX", "Hello");

    resp.setContentType("text/plain; charset=utf-8");
    resp.setBody(prefix + " " + name);
    resp.send();
});

framework.get("/pi", (req, resp) -> {
    resp.setContentType("text/plain; charset=utf-8");
    resp.setBody(String.valueOf(Math.PI));
    resp.send();
});

framework.start();
```

También existe el método:

```java
framework.stop();
```

para detener el servidor de forma controlada.

---

# Endpoints

## Endpoints dinámicos

### `GET /hello`

Devuelve un saludo.

Sin parámetro:

```text
GET /hello
```

Respuesta:

```text
Hello world
```

Con un nombre:

```text
GET /hello?name=Nestor
```

Respuesta:

```text
Hello Nestor
```

El valor de `GREETING_PREFIX` también se tiene en cuenta.

---

### `GET /pi`

Devuelve el valor de π:

```text
GET /pi
```

Respuesta:

```text
3.141592653589793
```

---

### `GET /shutdown`

Permite detener el servidor de manera controlada.

Este endpoint solamente está disponible cuando:

```text
APP_ENV != production
```

Por defecto el ambiente es `development`.

En producción, la ruta no se registra y por eso responde:

```text
404 Not Found
```

---

# Query Parameters

La clase `Request` permite obtener parámetros enviados en la URL mediante:

```java
req.getValue("name")
```

Por ejemplo:

```text
/hello?name=Nestor
```

devuelve:

```text
Nestor
```

También se soportan varios parámetros:

```text
/hello?name=Nestor&language=es
```

Y si un mismo parámetro aparece varias veces, `getValue()` devuelve el primer valor:

```text
/hello?name=A&name=B
```

Resultado:

```text
A
```

Los parámetros también se decodifican automáticamente.

Por ejemplo:

```text
Juan%20Perez
```

se convierte en:

```text
Juan Perez
```

---

# Archivos estáticos

Cuando una ruta no corresponde a un endpoint dinámico, el framework intenta buscar el recurso dentro de:

```text
src/main/resources/webroot/
```

Por ejemplo:

```text
/
```

carga:

```text
webroot/index.html
```

Otros ejemplos:

```text
/styles.css
/app.js
/images/logo.png
```

El servidor también identifica diferentes tipos MIME, entre ellos:

* `.html`
* `.css`
* `.js`
* `.png`
* `.jpg`
* `.jpeg`
* `.gif`
* `.txt`
* `.ico`
* `.svg`
* `.json`
* `.woff`
* `.woff2`
* `.ttf`
* `.eot`

---

# Orden de resolución de las rutas

Cuando llega una petición, el framework sigue este orden:

```text
1. Ruta dinámica
        ↓
2. Archivo estático
        ↓
3. 404 Not Found
```

Esto permite que una ruta registrada mediante `framework.get()` tenga prioridad sobre un posible archivo estático con el mismo nombre.

---

# Pruebas realizadas

## 1. Archivos estáticos

```bash
curl -v http://localhost:8080/

curl -v http://localhost:8080/index.html

curl -v http://localhost:8080/styles.css

curl -v http://localhost:8080/app.js

curl -v http://localhost:8080/images/logo.png
```

Se verifica que los recursos respondan con:

```text
200 OK
```

y que tengan su `Content-Type` y `Content-Length` correspondientes.

---

## 2. Endpoints dinámicos

```bash
curl -v http://localhost:8080/hello

curl -v "http://localhost:8080/hello?name=Nestor"

curl -v "http://localhost:8080/hello?name=Nestor&language=es"

curl -v http://localhost:8080/pi
```

---

## 3. Rutas inexistentes

```bash
curl -v http://localhost:8080/unknown

curl -v http://localhost:8080/archivo-inexistente.txt
```

En estos casos se espera:

```text
404 Not Found
```

---

## 4. Path Traversal

También se probaron diferentes formas de intentar acceder a archivos fuera de `webroot`.

```bash
curl -v "http://localhost:8080/../pom.xml"

curl -v "http://localhost:8080/..%2Fpom.xml"

curl -v "http://localhost:8080/%2e%2e%2fpom.xml"

curl -v "http://localhost:8080/%2e%2e%5cpom.xml"
```

Estos intentos no deben permitir acceder al `pom.xml` ni a ningún otro archivo fuera de `webroot`.

El servidor responde con:

```text
404 Not Found
```

---

## 5. Request malformado

Se probó también qué sucede cuando llega una petición que no tiene el formato HTTP esperado:

```bash
echo -e "GARBAGE REQUEST\r\n\r\n" | nc localhost 8080
```

El resultado esperado es:

```text
400 Bad Request
```

Lo importante en esta prueba es que el error de una petición no termine el proceso completo del servidor.

Después de la prueba se puede verificar que continúa funcionando:

```bash
curl -v http://localhost:8080/pi
```

---

## 6. Variables de entorno

### `GREETING_PREFIX`

```bash
GREETING_PREFIX=Hola java -jar target/httpserver-1.0-SNAPSHOT.jar
```

Luego:

```bash
curl "http://localhost:8080/hello?name=Nestor"
```

Respuesta:

```text
Hola Nestor
```

### `PORT`

```bash
PORT=9090 java -jar target/httpserver-1.0-SNAPSHOT.jar
```

Y se prueba:

```bash
curl http://localhost:9090/pi
```

---

## 7. Shutdown

En desarrollo:

```bash
curl -v http://localhost:8080/shutdown
```

Respuesta:

```text
200 OK
Server shutting down...
```

Después de enviar la respuesta, el servidor cierra el `ServerSocket` y termina su ciclo principal.

En producción:

```bash
APP_ENV=production java -jar target/httpserver-1.0-SNAPSHOT.jar
```

Al intentar:

```bash
curl -v http://localhost:8080/shutdown
```

se obtiene:

```text
404 Not Found
```

---

# Frontend de prueba

El proyecto incluye una pequeña interfaz dentro de `webroot/`.

Se puede abrir desde:

```text
http://localhost:8080/
```

La página permite probar algunas de las funcionalidades del servidor desde el navegador.

Por ejemplo:

* Probar `/hello`
* Probar `/pi`
* Enviar parámetros mediante `fetch()`
* Verificar que se cargue el CSS
* Verificar el JavaScript
* Cargar imágenes
* Mostrar las respuestas del servidor

Esto también sirve para comprobar que los archivos estáticos y los endpoints dinámicos funcionan juntos.

---

# Deployment en AWS

El servidor está preparado para ejecutarse en servicios de AWS como EC2, Elastic Beanstalk o ECS.

Primero se genera el JAR:

```bash
mvn clean package
```

El archivo generado es:

```text
target/httpserver-1.0-SNAPSHOT.jar
```

### Arquitectura en AWS (EC2)

![Despliegue en EC2](Image/EC2%20.png)

En el servidor cloud se deben configurar las variables de entorno correspondientes.

Por ejemplo:

```text
PORT=5000
APP_ENV=production
GREETING_PREFIX=Hola
```

El puerto debe utilizar el valor que proporcione el entorno de ejecución.

### Health check

Se puede utilizar:

```text
GET /
```

o:

```text
GET /pi
```

para comprobar que la aplicación está respondiendo correctamente.

Después del despliegue se pueden verificar:

```text
GET /
GET /hello?name=Cloud
GET /pi
GET /shutdown
```

En producción, `/shutdown` debe devolver `404 Not Found`.

---

# Estructura del proyecto

```text
httpserver/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/
    │   │   └── co/
    │   │       └── edu/
    │   │           └── escuelaing/
    │   │               ├── Application.java
    │   │               ├── HttpServer.java
    │   │               ├── WebFramework.java
    │   │               ├── Router.java
    │   │               ├── StaticFileService.java
    │   │               ├── Request.java
    │   │               ├── Response.java
    │   │               ├── WebService.java
    │   │               ├── EchoServer.java
    │   │               ├── EchoClient.java
    │   │               └── Main.java
    │   │
    │   └── resources/
    │       └── webroot/
    │           ├── index.html
    │           ├── styles.css
    │           ├── app.js
    │           └── images/
    │               └── logo.png
    │
    └── test/
```

Las clases `EchoServer`, `EchoClient` y `Main` corresponden a código utilizado en etapas anteriores del laboratorio y se mantienen como parte del proyecto.

---

# Requisitos del laboratorio

| #  | Requisito                  | Implementación                             |
| -- | -------------------------- | ------------------------------------------ |
| 1  | `staticfiles("/webroot")`  | `WebFramework` + `StaticFileService`       |
| 2  | `get("/ruta", lambda)`     | `Router` + `WebService`                    |
| 3  | `Request.getValue()`       | Lectura de query parameters                |
| 4  | Múltiples parámetros       | Parser mediante `&`                        |
| 5  | Prioridad de rutas         | Dinámicas → estáticos → 404                |
| 6  | HTTP 404                   | Implementado en `Response`                 |
| 7  | Requests inválidos         | Manejo de errores con `400`                |
| 8  | `PORT`                     | Variable de entorno con `8080` por defecto |
| 9  | `GREETING_PREFIX`          | Personalización del endpoint `/hello`      |
| 10 | `/shutdown` en development | Registro condicional                       |
| 11 | Shutdown graceful          | Cierre del `ServerSocket`                  |
| 12 | HTML                       | `index.html`                               |
| 13 | CSS                        | `styles.css`                               |
| 14 | JavaScript                 | `app.js`                                   |
| 15 | Imagen                     | `logo.png`                                 |
| 16 | Dos endpoints lambda       | `/hello` y `/pi`                           |
| 17 | `fetch()`                  | Utilizado desde `app.js`                   |
| 18 | Cloud deployment           | JAR ejecutable + configuración por entorno |
| 19 | `PORT` en cloud            | Lectura mediante variable de entorno       |
| 20 | `APP_ENV=production`       | Configurable                               |
| 21 | Shutdown deshabilitado     | `/shutdown` no se registra en producción   |
| 22 | README                     | Documentación del proyecto                 |

---

# Seguridad

Aunque se trata de un servidor pequeño para el laboratorio, se agregaron algunas validaciones básicas.

### Path Traversal

Los archivos solamente se pueden obtener desde:

```text
classpath:/webroot/
```

Se validan las rutas para evitar intentos de acceder a archivos externos mediante segmentos como:

```text
..
.
%2e
%2e%2e
%2f
%5c
```

### Manejo de errores

El servidor utiliza diferentes códigos HTTP según el problema:

```text
200 OK
400 Bad Request
404 Not Found
500 Internal Server Error
```

Los errores no exponen el stack trace directamente al cliente.

### Servidor secuencial

El servidor mantiene el comportamiento solicitado en el laboratorio: procesa las conexiones de forma secuencial y no implementa concurrencia mediante múltiples threads.

---

# Tecnologías

* **Java 17**
* **Maven**
* **Java Sockets**
* **HTML**
* **CSS**
* **JavaScript**
* **AWS**

Se utilizan principalmente clases del JDK como:

```text
java.net
java.io
java.nio
java.util
```

El objetivo fue construir la funcionalidad principal sin depender de frameworks web externos.

---

# Autor

**Nestor David Lopez Castañeda**

Estudiante de Ingeniería de Sistemas
Escuela Colombiana de Ingeniería Julio Garavito

---

# Licencia

MIT License.
