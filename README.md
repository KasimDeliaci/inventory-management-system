# 📦 Inventory Management System

A full-stack, modular **Inventory & Planning platform** featuring an **Angular frontend**, **Spring Boot microservices**, a **Python forecasting service**, and **PostgreSQL databases**.  
It covers daily stock operations (products, suppliers, customers, orders, campaigns), time-series forecasting, and replenishment planning — with optional **LLM-assisted insights via Ollama**.

Repo: [inventory-management-system](https://github.com/KasimDeliaci/inventory-management-system)

---

## 🚀 Features

- **Products / Suppliers / Customers / Orders / Campaigns**
- **Stock Movements & Current Stock Snapshots**
- **Sales Reporting** (daily, range-based)
- **Forecast Service** (7-day / 14-day demand forecasts)
- **Planning Service** with orchestration and recommendations
- **Optional Ollama LLM** for natural-language stock planning
- **Modern Angular UI** with CRUD forms, tables, and status indicators

---

## 🏗️ Architecture

![Architecture](docs/mimari_tasarım.png)

- **Frontend** → Angular app (requests through API Gateway)
- **API Gateway** → Entry point, routes to internal services
- **Inventory Service (Spring Boot)** → CRUD, stock ops, reporting
- **Forecast Service (Python)** → Historical data, ML/forecasting
- **Planning Service (Spring Boot)** → Combines current state + predictions → recommendations
- **Databases** → PostgreSQL (Inventory, Forecasting, Planning)

---

## 🗂️ Database Design

```mermaid
erDiagram
  PRODUCTS {
    bigint      product_id PK
    varchar     product_name
    text        description
    varchar     category
    varchar     unit_of_measure
    numeric     safety_stock
    numeric     reorder_point
    numeric     current_price
    timestampz  created_at
    timestampz  updated_at
    timestampz  deleted_at
  }
  SUPPLIERS {
    bigint      supplier_id PK
    varchar     supplier_name
    varchar     email
    varchar     phone
    varchar     city
    timestampz  created_at
    timestampz  updated_at
    timestampz  deleted_at
  }
  PRODUCT_SUPPLIERS {
    bigint     product_supplier_id PK
    bigint     product_id FK
    bigint     supplier_id FK
    numeric    min_order_quantity
    boolean    is_preferred
    boolean    active
    numeric    avg_lead_time_days
    numeric    avg_delay_days
    integer    total_orders_count
    integer    delayed_orders_count
    date       last_delivery_date
    timestampz created_at
    timestampz updated_at
  }
  CUSTOMERS {
    bigint     customer_id PK
    varchar    customer_name
    varchar    customer_segment
    varchar    email
    varchar    phone
    varchar    city
    timestampz created_at
    timestampz updated_at
    timestampz deleted_at
  }
  CUSTOMER_SPECIAL_OFFERS {
    bigint      special_offer_id PK
    bigint      customer_id FK
    numeric     percent_off
    date        start_date
    date        end_date
    timestampz  created_at
    timestampz  updated_at
  }
  CAMPAIGNS {
    bigint      campaign_id PK
    text        campaign_name
    text        campaign_type
    numeric     discount_percentage
    integer     buy_qty
    integer     get_qty
    date        start_date
    date        end_date
    timestampz  created_at
    timestampz  updated_at
  }
  CAMPAIGN_PRODUCTS {
    bigint  campaign_id FK
    bigint  product_id  FK
  }
  SALES_ORDERS {
    bigint              sales_order_id PK
    bigint              customer_id FK
    date                order_date
    date                delivery_date
    timestampz          delivered_at
    sales_order_status  status
    bigint              customer_special_offer_id FK
    numeric             customer_discount_pct_applied
    timestampz          created_at
    timestampz          updated_at
  }
  SALES_ORDER_ITEMS {
    bigint      sales_order_item_id PK
    bigint      sales_order_id FK
    bigint      product_id FK
    numeric     quantity
    numeric     unit_price
    numeric     discount_percentage
    bigint      campaign_id FK
    numeric     discount_amount
    numeric     line_total
    timestampz  created_at
  }
  PURCHASE_ORDERS {
    bigint                purchase_order_id PK
    bigint                supplier_id FK
    date                  order_date
    date                  expected_delivery
    timestampz            actual_delivery
    purchase_order_status status
    timestampz            created_at
    timestampz            updated_at
  }
  PURCHASE_ORDER_ITEMS {
    bigint      purchase_order_item_id PK
    bigint      purchase_order_id FK
    bigint      product_id FK
    numeric     quantity_ordered
    numeric     quantity_received
    numeric     unit_price
    numeric     line_total
    numeric     line_total_received
    timestampz  created_at
  }
  STOCK_MOVEMENTS {
    bigint  movement_id PK
    bigint  product_id FK
    varchar movement_type
    varchar movement_source
    numeric quantity
    numeric before_level
    numeric after_level
    timestampz movement_time
    timestampz created_at
  }
  CURRENT_STOCK {
    bigint      product_id PK
    numeric     quantity_on_hand
    numeric     quantity_reserved
    numeric     quantity_available
    bigint      last_movement_id FK
    timestampz  last_updated
  }
  PRODUCTS ||--o{ STOCK_MOVEMENTS : has_history
  PRODUCTS ||--|| CURRENT_STOCK : has_snapshot
  PRODUCTS ||--o{ PURCHASE_ORDER_ITEMS : appears_in
  PRODUCTS ||--o{ SALES_ORDER_ITEMS : appears_in
  PRODUCTS  ||--o{ PRODUCT_SUPPLIERS : is_supplied_by
  PRODUCTS  ||--o{ CAMPAIGN_PRODUCTS : is_target_by

  CURRENT_STOCK ||--o| STOCK_MOVEMENTS: is_last_update_for

  SUPPLIERS ||--o{ PURCHASE_ORDERS : receives
  SUPPLIERS ||--o{ PRODUCT_SUPPLIERS : supplies

  PURCHASE_ORDERS ||--o{ PURCHASE_ORDER_ITEMS : contains

  CUSTOMERS ||--o{ CUSTOMER_SPECIAL_OFFERS : has
  CUSTOMERS ||--o{ SALES_ORDERS : places

  CUSTOMER_SPECIAL_OFFERS |o--o{ SALES_ORDERS : applied_to

  SALES_ORDERS ||--o{ SALES_ORDER_ITEMS : contains

  CAMPAIGNS |o--o{ SALES_ORDER_ITEMS : influences
  CAMPAIGNS ||--o{ CAMPAIGN_PRODUCTS : targets
```

Highlights:
- **products**, **suppliers**, **customers**
- **sales_orders / sales_order_items**
- **purchase_orders / purchase_order_items**
- **campaigns / campaign_products**
- **stock_movements** (history)
- **current_stock** (real-time snapshot)

---

## 💻 Frontend Mockups

[Figma](https://www.figma.com/proto/7DF2uo0ZNED65x62reHOrr/Talep-Tahmini-ve-Stok-Yönetimi-Projesi?page-id=16%3A7&node-id=16-8&p=f&viewport=-809%2C102%2C0.61&t=Vn6HKj9Hii36xQXt-1&scaling=min-zoom&content-scaling=fixed&starting-point-node-id=16%3A8)

---

## 🔄 Flow Example

```mermaid
    flowchart LR
    A([Start]) --> B[Stock Management System]

    %% Top-level domains
    B --> P[Products]
    B --> S[Suppliers]
    B --> C[Customers]
    B --> O[Orders]
    B --> G[Campaigns]

    %% Products branch
    P --> P1{Current stock<br/>below safety stock?}
    P1 -- Yes --> P2[Notify Purchasing Unit]
    P2 --> P3[Show Safety Stock Screen / Details]
    P1 -- No --> P4[Product list remains same]

    P --> P5{New product exists?}
    P5 -- Yes --> P6[Add product to supplier list]
    P5 -- No --> P7[No change]

    %% Suppliers branch
    S --> S1{New supplier exists?}
    S1 -- Yes --> S2[Add supplier to list]
    S1 -- No --> S3[No change]
    S --> S4{Supplier still active?}
    S4 -- No --> S5[Remove inactive supplier]
    S4 -- Yes --> S6[Supplier list remains same]

    %% Customers branch
    C --> C1{New customer exists?}
    C1 -- Yes --> C2[Add customer to list]
    C1 -- No --> C3[No change]
    C --> C4{Customer still active?}
    C4 -- No --> C5[Remove inactive customer]
    C4 -- Yes --> C6[Customer list remains same]

    %% Orders branch
    O --> O1{Purchase order received?}
    O1 -- Yes --> O2[Increase current stock]
    O1 -- No --> O3[No change]

    O --> O4{Sales order committed?}
    O4 -- Yes --> O5[Decrease current stock]
    O4 -- No --> O6[No change]

    %% Campaigns branch
    G --> G1{Campaign exists/updated?}
    G1 -- Yes --> G2[Add/Update campaign products]
    G1 -- No --> G3[Campaign list remains same]

    %% End
    P4 & P7 & S3 & S6 & C6 & O3 & O6 & G3 --> Z([End])
```

---

## ⚙️ Local Development
Requirements
- Node.js 18+ / npm or pnpm
- Java 17+ (Spring Boot)
- Python 3.10+ (Forecast service)
- PostgreSQL 14+
- Docker

---

## SETUP

### Frontend
- cd frontend
- npm install
- npm start

### Inventory Service
- cd inventory-service
- ./mvnw spring-boot:run

### Planning Service
- cd planning-service
- ./mvnw spring-boot:run

### Forecast Service
- cd forecast-service
- python -m venv .venv && source .venv/bin/activate
- pip install -r requirements.txt
- python app.py

### Databases can be started with Docker

---

## 🛠️ Tech Stack
- Frontend: Angular 20
- Backend Services: Spring Boot (Inventory, Planning, Gateway)
- Forecasting: Python (FastAPI/Flask + ML/Stats)
- Database: PostgreSQL
- AI Integration: Ollama

---

## 👥 Contributors
- [Eren GÜRELİ](https://www.linkedin.com/in/eren-gureli/)
- [Irmak Aslan](https://www.linkedin.com/in/irmak-aslan/)
- [Kasım Deliacı](https://www.linkedin.com/in/kasimdeliaci/)
- [Yiğit Şevki Kaplan](https://www.linkedin.com/in/yigitskaplan/)
