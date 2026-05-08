# Marketly 🛒

Marketplace de productos digitales desarrollado como Trabajo de Fin de Grado (TFG) del ciclo de Desarrollo de Aplicaciones Multiplataforma (DAM).

## Descripción

Marketly es una aplicación Android que permite a los usuarios comprar y vender productos digitales como música, software, ebooks, fotografías y más. Cuenta con un sistema de retención de pagos que protege tanto al comprador como al vendedor, un chat en tiempo real y un panel de moderación para resolver disputas.

## Funcionalidades

- Registro e inicio de sesión con email o nombre de usuario
- Publicación de productos digitales con imágenes y archivo descargable
- Sistema de compra con retención de saldo hasta confirmar la recepción
- Chat en tiempo real entre comprador y vendedor
- Sistema de incidencias y moderación de disputas
- Valoraciones entre usuarios
- Cartera virtual con historial de transacciones
- Panel de moderación para admins y moderadores
- Perfil de usuario con productos y valoraciones
- Cierre automático de pedidos a las 48h

## Tecnologías

- **Kotlin** — Lenguaje principal
- **Android Studio** — Entorno de desarrollo
- **Supabase** — Backend (base de datos, autenticación, storage y realtime)
- **PostgreSQL** — Base de datos relacional
- **Glide** — Carga de imágenes
- **Material Design** — Componentes de interfaz
- **ViewPager2** — Carrusel de imágenes

## Estructura del proyecto
app/src/main/java/com/ramos/marketly/
├── controller/       # Activities
├── model/            # Clases de datos
├── adapter/          # Adaptadores RecyclerView
└── utils/            # Utilidades (Supabase, SessionManager)

## Base de datos

El proyecto utiliza las siguientes tablas en Supabase:

- `users` — Usuarios de la plataforma
- `products` — Productos publicados
- `orders` — Órdenes de compra
- `messages` — Mensajes del chat
- `incidences` — Incidencias abiertas
- `ratings` — Valoraciones entre usuarios
- `wallet_transactions` — Historial de transacciones

## Configuración

1. Clona el repositorio
```bash
git clone https://github.com/JoelXD18/Marketly.git
```

2. Crea el archivo `local.properties` en la raíz del proyecto y añade tus credenciales de Supabase:
SUPABASE_URL= consula al administrador (JoelXD18).
SUPABASE_KEY= consula al administrador (JoelXD18).

3. Abre el proyecto en Android Studio y sincroniza las dependencias con Gradle.

4. Ejecuta la aplicación en un emulador o dispositivo físico con Android API 36 o superior.

## Requisitos

- Android Studio Hedgehog o superior
- Android API 36 o superior
- Cuenta en Supabase con las tablas configuradas

## Autor

**Joel Ramos Lázaro**
TFG — Ciclo DAM
