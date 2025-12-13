-- ============================================
-- BASE DE DONNÉES PICKNDROP USSD
-- ============================================

-- ============================================
-- CRÉATION DES TYPES ENUM
-- ============================================

DROP TYPE IF EXISTS user_type;
CREATE TYPE user_type AS ENUM ('CLIENT', 'FREELANCE', 'AGENCY', 'DELIVERY_PERSON');

DROP TYPE IF EXISTS transport_mode;
CREATE TYPE transport_mode AS ENUM ('TRUCK', 'TRICYCLE', 'MOTORCYCLE', 'BICYCLE', 'CAR');

DROP TYPE IF EXISTS delivery_type;
CREATE TYPE delivery_type AS ENUM ('STANDARD', 'EXPRESS_48H', 'EXPRESS_24H');

DROP TYPE IF EXISTS payment_method;
CREATE TYPE payment_method AS ENUM ('CASH', 'MOBILE_MONEY', 'ORANGE_MONEY', 'PAID_BY_RECIPIENT');

DROP TYPE IF EXISTS shipment_status;
CREATE TYPE shipment_status AS ENUM ('PENDING', 'CONFIRMED', 'IN_TRANSIT', 'DELIVERED', 'CANCELLED');


-- ============================================
-- TABLE: user (classe abstraite)
-- ============================================
CREATE TABLE "user" (
    id SERIAL PRIMARY KEY,
    role user_type NOT NULL,

    name VARCHAR(120) NOT NULL,
    phone VARCHAR(20) UNIQUE NOT NULL,
    email VARCHAR(150) UNIQUE,
    password VARCHAR(255) NOT NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- TABLE: client
-- ============================================
CREATE TABLE IF NOT EXISTS client (
    id SERIAL PRIMARY KEY REFERENCES "user"(id) ON DELETE CASCADE,

    city VARCHAR(100) NOT NULL,
    address TEXT NOT NULL
);

-- ============================================
-- TABLE: recipient
-- ============================================
CREATE TABLE recipient (
    id SERIAL PRIMARY KEY,

    name VARCHAR(120) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(150),

    city VARCHAR(100) NOT NULL,
    address TEXT NOT NULL,

    sender_id SERIAL NOT NULL REFERENCES "client"(id) ON DELETE CASCADE
);

-- ============================================
-- TABLE: package
-- ============================================
CREATE TABLE package (
    id SERIAL PRIMARY KEY,

    description TEXT NOT NULL,
    weight NUMERIC(6,2) NOT NULL CHECK (weight > 0),

    fragile BOOLEAN NOT NULL,
    perishable BOOLEAN NOT NULL,
    liquid BOOLEAN NOT NULL,

    insured BOOLEAN NOT NULL,
    declared_value NUMERIC(10,2), 

    sender_id SERIAL NOT NULL REFERENCES "client"(id) ON DELETE CASCADE
);


-- ============================================
-- TABLE: shipment
-- ============================================
CREATE TABLE shipment (
    id SERIAL PRIMARY KEY,

    sender_id SERIAL NOT NULL REFERENCES client(id),
    recipient_id SERIAL NOT NULL REFERENCES recipient(id),
    package_id SERIAL NOT NULL REFERENCES package(id),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    tracking_id VARCHAR(100) NOT NULL UNIQUE,
    status shipment_status NOT NULL DEFAULT 'PENDING',

    total_price DECIMAL(10, 2) NOT NULL,

    pickup_address TEXT NOT NULL,
    delivery_address TEXT NOT NULL
);

-- ============================================
-- TABLE: delivery
-- ============================================
CREATE TABLE delivery (
    id SERIAL PRIMARY KEY,

    shipment_id SERIAL NOT NULL UNIQUE REFERENCES shipment(id),
    delivery_person_id SERIAL REFERENCES "user"(id),

    transport transport_mode NOT NULL,
    type delivery_type NOT NULL
);


-- ============================================
-- TABLE: payment
-- ============================================
CREATE TABLE payment (
    id SERIAL PRIMARY KEY,
    shipment_id INTEGER NOT NULL UNIQUE REFERENCES shipment(id),
    mode payment_method NOT NULL,
    amount NUMERIC(10,2) NOT NULL
);


-- ============================================
-- INDEXES POUR OPTIMISATION DES PERFORMANCES
-- ============================================

CREATE INDEX IF NOT EXISTS idx_user_role ON "user"(role);
CREATE INDEX IF NOT EXISTS idx_user_phone ON "user"(phone);
CREATE INDEX IF NOT EXISTS idx_user_email ON "user"(email);

CREATE INDEX IF NOT EXISTS idx_client_city ON client(city);

CREATE INDEX IF NOT EXISTS idx_recipient_sender ON recipient(sender_id);
CREATE INDEX IF NOT EXISTS idx_recipient_phone ON recipient(phone);

CREATE INDEX IF NOT EXISTS idx_package_sender ON package(sender_id);

CREATE INDEX IF NOT EXISTS idx_shipment_sender ON shipment(sender_id);
CREATE INDEX IF NOT EXISTS idx_shipment_status ON shipment(status);
CREATE INDEX IF NOT EXISTS idx_shipment_created_at ON shipment(created_at);

CREATE INDEX IF NOT EXISTS idx_delivery_person ON delivery(delivery_person_id);
CREATE INDEX IF NOT EXISTS idx_delivery_transport ON delivery(transport);

CREATE INDEX IF NOT EXISTS idx_payment_shipment ON payment(shipment_id);
CREATE INDEX IF NOT EXISTS idx_payment_mode ON payment(mode);
