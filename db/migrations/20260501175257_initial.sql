-- Create "idempotency_key" table
CREATE TABLE "public"."idempotency_key" (
  "key" character varying(255) NOT NULL,
  "scope" character varying(50) NOT NULL,
  "request_path" character varying(500) NOT NULL,
  "response_status" integer NULL,
  "response_body" text NULL,
  "created_at" timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "expires_at" timestamptz NOT NULL,
  PRIMARY KEY ("key", "scope")
);
-- Create index "idx_idempotency_key_expires_at" to table: "idempotency_key"
CREATE INDEX "idx_idempotency_key_expires_at" ON "public"."idempotency_key" ("expires_at");
-- Add new schema named "vault"
CREATE SCHEMA "vault";
-- Create "user" table
CREATE TABLE "public"."user" (
  "id" bigserial NOT NULL,
  "deleted_at" timestamptz NULL,
  "created_at" timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY ("id")
);
-- Add new schema named "auth"
CREATE SCHEMA "auth";
-- Create "articles" table
CREATE TABLE "public"."articles" (
  "id" bigserial NOT NULL,
  "slug" character varying(255) NOT NULL,
  "title" character varying(255) NOT NULL,
  "description" text NOT NULL,
  "body" text NOT NULL,
  "author_id" bigint NOT NULL,
  "created_at" timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY ("id"),
  CONSTRAINT "articles_slug_key" UNIQUE ("slug"),
  CONSTRAINT "articles_author_id_fkey" FOREIGN KEY ("author_id") REFERENCES "public"."user" ("id") ON UPDATE CASCADE ON DELETE CASCADE
);
-- Create index "idx_articles_author" to table: "articles"
CREATE INDEX "idx_articles_author" ON "public"."articles" ("author_id");
-- Create index "idx_articles_created_at" to table: "articles"
CREATE INDEX "idx_articles_created_at" ON "public"."articles" ("created_at" DESC);
-- Create index "idx_articles_slug" to table: "articles"
CREATE INDEX "idx_articles_slug" ON "public"."articles" ("slug");
-- Create "tags" table
CREATE TABLE "public"."tags" (
  "id" bigserial NOT NULL,
  "name" character varying(255) NOT NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "tags_name_key" UNIQUE ("name")
);
-- Create index "idx_tags_name" to table: "tags"
CREATE INDEX "idx_tags_name" ON "public"."tags" ("name");
-- Create "article_tags" table
CREATE TABLE "public"."article_tags" (
  "article_id" bigint NOT NULL,
  "tag_id" bigint NOT NULL,
  PRIMARY KEY ("article_id", "tag_id"),
  CONSTRAINT "article_tags_article_id_fkey" FOREIGN KEY ("article_id") REFERENCES "public"."articles" ("id") ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT "article_tags_tag_id_fkey" FOREIGN KEY ("tag_id") REFERENCES "public"."tags" ("id") ON UPDATE CASCADE ON DELETE CASCADE
);
-- Create index "idx_article_tags_article_id" to table: "article_tags"
CREATE INDEX "idx_article_tags_article_id" ON "public"."article_tags" ("article_id");
-- Create index "idx_article_tags_tag_id" to table: "article_tags"
CREATE INDEX "idx_article_tags_tag_id" ON "public"."article_tags" ("tag_id");
-- Create "comments" table
CREATE TABLE "public"."comments" (
  "id" bigserial NOT NULL,
  "body" text NOT NULL,
  "article_id" bigint NOT NULL,
  "author_id" bigint NOT NULL,
  "created_at" timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY ("id"),
  CONSTRAINT "comments_article_id_fkey" FOREIGN KEY ("article_id") REFERENCES "public"."articles" ("id") ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT "comments_author_id_fkey" FOREIGN KEY ("author_id") REFERENCES "public"."user" ("id") ON UPDATE CASCADE ON DELETE CASCADE
);
-- Create index "idx_comments_article_id" to table: "comments"
CREATE INDEX "idx_comments_article_id" ON "public"."comments" ("article_id");
-- Create index "idx_comments_author_id" to table: "comments"
CREATE INDEX "idx_comments_author_id" ON "public"."comments" ("author_id");
-- Create "favorites" table
CREATE TABLE "public"."favorites" (
  "user_id" bigint NOT NULL,
  "article_id" bigint NOT NULL,
  PRIMARY KEY ("user_id", "article_id"),
  CONSTRAINT "favorites_article_id_fkey" FOREIGN KEY ("article_id") REFERENCES "public"."articles" ("id") ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT "favorites_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."user" ("id") ON UPDATE CASCADE ON DELETE CASCADE
);
-- Create index "idx_favorites_article_id" to table: "favorites"
CREATE INDEX "idx_favorites_article_id" ON "public"."favorites" ("article_id");
-- Create index "idx_favorites_user_id" to table: "favorites"
CREATE INDEX "idx_favorites_user_id" ON "public"."favorites" ("user_id");
-- Create "followers" table
CREATE TABLE "public"."followers" (
  "follower_id" bigint NOT NULL,
  "followee_id" bigint NOT NULL,
  PRIMARY KEY ("follower_id", "followee_id"),
  CONSTRAINT "followers_followee_id_fkey" FOREIGN KEY ("followee_id") REFERENCES "public"."user" ("id") ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT "followers_follower_id_fkey" FOREIGN KEY ("follower_id") REFERENCES "public"."user" ("id") ON UPDATE CASCADE ON DELETE CASCADE
);
-- Create index "idx_followers_followee_id" to table: "followers"
CREATE INDEX "idx_followers_followee_id" ON "public"."followers" ("followee_id");
-- Create index "idx_followers_follower_id" to table: "followers"
CREATE INDEX "idx_followers_follower_id" ON "public"."followers" ("follower_id");
-- Create "password" table
CREATE TABLE "auth"."password" (
  "user_id" bigint NOT NULL,
  "hash" text NOT NULL,
  "created_at" timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY ("user_id"),
  CONSTRAINT "password_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."user" ("id") ON UPDATE NO ACTION ON DELETE CASCADE
);
-- Create "person" table
CREATE TABLE "vault"."person" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "user_id" bigint NOT NULL,
  "email_enc" bytea NOT NULL,
  "email_hash" character varying(100) NOT NULL,
  "email_verified_at" timestamptz NULL,
  "username_enc" bytea NOT NULL,
  "username_hash" character varying(100) NOT NULL,
  "bio_enc" bytea NULL,
  "image_enc" bytea NULL,
  "created_at" timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY ("id"),
  CONSTRAINT "person_email_hash_key" UNIQUE ("email_hash"),
  CONSTRAINT "person_user_id_key" UNIQUE ("user_id"),
  CONSTRAINT "person_username_hash_key" UNIQUE ("username_hash"),
  CONSTRAINT "person_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."user" ("id") ON UPDATE NO ACTION ON DELETE CASCADE
);
-- Create index "idx_person_email_hash" to table: "person"
CREATE INDEX "idx_person_email_hash" ON "vault"."person" ("email_hash");
-- Create index "idx_person_username_hash" to table: "person"
CREATE INDEX "idx_person_username_hash" ON "vault"."person" ("username_hash");
-- Create "refresh_token" table
CREATE TABLE "auth"."refresh_token" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "user_id" bigint NOT NULL,
  "token_hash" character varying(100) NOT NULL,
  "expires_at" timestamptz NOT NULL,
  "revoked_at" timestamptz NULL,
  "created_at" timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY ("id"),
  CONSTRAINT "refresh_token_token_hash_key" UNIQUE ("token_hash"),
  CONSTRAINT "refresh_token_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."user" ("id") ON UPDATE NO ACTION ON DELETE CASCADE
);
-- Create index "idx_refresh_token_expires_at" to table: "refresh_token"
CREATE INDEX "idx_refresh_token_expires_at" ON "auth"."refresh_token" ("expires_at");
-- Create index "idx_refresh_token_user_id" to table: "refresh_token"
CREATE INDEX "idx_refresh_token_user_id" ON "auth"."refresh_token" ("user_id");
-- Create "revoked_token" table
CREATE TABLE "auth"."revoked_token" (
  "jti" uuid NOT NULL,
  "user_id" bigint NOT NULL,
  "expires_at" timestamptz NOT NULL,
  PRIMARY KEY ("jti"),
  CONSTRAINT "revoked_token_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."user" ("id") ON UPDATE NO ACTION ON DELETE CASCADE
);
-- Create index "idx_revoked_token_expires_at" to table: "revoked_token"
CREATE INDEX "idx_revoked_token_expires_at" ON "auth"."revoked_token" ("expires_at");
