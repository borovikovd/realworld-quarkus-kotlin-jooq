CREATE TABLE "public"."idempotency_key" (
  "key"             varchar(255) NOT NULL,
  "scope"           varchar(50)  NOT NULL,
  "request_path"    varchar(500) NOT NULL,
  "response_status" integer      NULL,
  "response_body"   text         NULL,
  "created_at"      timestamptz  NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "expires_at"      timestamptz  NOT NULL,
  PRIMARY KEY ("key", "scope")
);
CREATE INDEX "idx_idempotency_key_expires_at" ON "public"."idempotency_key" ("expires_at");
