-- Add refresh_token table for short-lived access + refresh token auth.

CREATE TABLE "auth"."refresh_token" (
  "id"         uuid        NOT NULL DEFAULT gen_random_uuid(),
  "user_id"    bigint      NOT NULL,
  "token_hash" varchar(44) NOT NULL,
  "expires_at" timestamptz NOT NULL,
  "revoked_at" timestamptz NULL,
  "created_at" timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY ("id"),
  CONSTRAINT "refresh_token_token_hash_key" UNIQUE ("token_hash"),
  CONSTRAINT "refresh_token_user_id_fkey"   FOREIGN KEY ("user_id") REFERENCES "public"."user" ("id") ON DELETE CASCADE
);
CREATE INDEX "idx_refresh_token_user_id" ON "auth"."refresh_token" ("user_id");
