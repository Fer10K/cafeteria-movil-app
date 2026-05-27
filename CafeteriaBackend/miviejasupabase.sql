SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

-- Extensiones necesarias para generar UUIDs de forma nativa
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ==========================================
-- 1. TABLAS BASE (SIN DEPENDENCIAS)
-- ==========================================

CREATE TABLE public.categorias (
    id SERIAL PRIMARY KEY,
    nombre character varying(50) NOT NULL UNIQUE,
    creado_en timestamp with time zone DEFAULT now()
);

CREATE TABLE public.grupos_opciones (
    id SERIAL PRIMARY KEY,
    nombre character varying(100) NOT NULL,
    min_seleccion integer DEFAULT 0,
    max_seleccion integer DEFAULT 1,
    creado_en timestamp with time zone DEFAULT now()
);

CREATE TABLE public.logros (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    codigo text NOT NULL UNIQUE,
    nombre text NOT NULL,
    descripcion text NOT NULL,
    icono_url text,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE public.usuarios (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    nombre text NOT NULL,
    correo text NOT NULL UNIQUE,
    contrasena text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    rol character varying(20) DEFAULT 'cliente'::character varying
);

-- ==========================================
-- 2. TABLAS CON DEPENDENCIAS DIRECTAS
-- ==========================================

CREATE TABLE public.productos (
    id SERIAL PRIMARY KEY,
    categoria_id integer REFERENCES public.categorias(id) ON DELETE SET NULL,
    nombre character varying(100) NOT NULL,
    descripcion text,
    precio numeric(10,2) NOT NULL,
    imagen_url text,
    disponible boolean DEFAULT true,
    creado_en timestamp with time zone DEFAULT now()
);

CREATE TABLE public.opciones_extras (
    id SERIAL PRIMARY KEY,
    grupo_id integer REFERENCES public.grupos_opciones(id) ON DELETE CASCADE,
    nombre character varying(100) NOT NULL,
    precio_adicional numeric(10,2) DEFAULT 0.00,
    disponible boolean DEFAULT true
);

CREATE TABLE public.pedidos (
    pedido_id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    usuario_id uuid REFERENCES public.usuarios(id) ON DELETE CASCADE,
    metodo_pago character varying(20) NOT NULL,
    estado character varying(30) DEFAULT 'PENDIENTE_PAGO'::character varying,
    monto_total numeric(10,2) DEFAULT 0.0 NOT NULL,
    fecha_creacion timestamp with time zone DEFAULT now()
);

CREATE TABLE public.perfiles_gamificacion (
    usuario_id uuid PRIMARY KEY REFERENCES public.usuarios(id) ON DELETE CASCADE,
    xp_total integer DEFAULT 0 NOT NULL,
    nivel integer DEFAULT 1 NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    avatar_url text DEFAULT 'https://vuestrasubase.supabase.co/storage/v1/object/public/avatares/default.png'::text
);

-- ==========================================
-- 3. TABLAS RELACIONALES / DETALLES
-- ==========================================

CREATE TABLE public.producto_grupo_opciones (
    producto_id integer REFERENCES public.productos(id) ON DELETE CASCADE,
    grupo_id integer REFERENCES public.grupos_opciones(id) ON DELETE CASCADE,
    PRIMARY KEY (producto_id, grupo_id)
);

CREATE TABLE public.carrito_items (
    id SERIAL PRIMARY KEY,
    usuario_id uuid REFERENCES public.usuarios(id) ON DELETE CASCADE,
    producto_id integer REFERENCES public.productos(id) ON DELETE CASCADE,
    cantidad integer DEFAULT 1,
    creado_en timestamp with time zone DEFAULT now()
);

CREATE TABLE public.carrito_item_extras (
    id SERIAL PRIMARY KEY,
    carrito_item_id integer REFERENCES public.carrito_items(id) ON DELETE CASCADE,
    opcion_extra_id integer REFERENCES public.opciones_extras(id) ON DELETE CASCADE
);

CREATE TABLE public.pedido_items (
    id BIGSERIAL PRIMARY KEY,
    pedido_id uuid REFERENCES public.pedidos(pedido_id) ON DELETE CASCADE,
    producto_id integer NOT NULL,
    nombre_producto character varying(100) NOT NULL,
    cantidad integer NOT NULL,
    precio_unitario_base numeric(10,2) NOT NULL
);

CREATE TABLE public.pedido_item_extras (
    id BIGSERIAL PRIMARY KEY,
    pedido_item_id bigint REFERENCES public.pedido_items(id) ON DELETE CASCADE,
    extra_id integer NOT NULL,
    nombre_extra character varying(100) NOT NULL,
    precio_adicional numeric(10,2) NOT NULL
);

CREATE TABLE public.usuario_logros (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    usuario_id uuid REFERENCES public.perfiles_gamificacion(usuario_id) ON DELETE CASCADE,
    logro_id uuid REFERENCES public.logros(id) ON DELETE CASCADE,
    desbloqueado_en timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT usuario_logros_usuario_id_logro_id_key UNIQUE (usuario_id, logro_id)
);