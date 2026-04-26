-- public schema

CREATE TABLE "public"."user" (
  "id"         bigserial    NOT NULL,
  "deleted_at" timestamptz  NULL,
  "created_at" timestamptz  NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamptz  NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY ("id")
);

CREATE TABLE "public"."articles" (
  "id"          bigserial    NOT NULL,
  "slug"        varchar(255) NOT NULL,
  "title"       varchar(255) NOT NULL,
  "description" text         NOT NULL,
  "body"        text         NOT NULL,
  "author_id"   bigint       NOT NULL,
  "created_at"  timestamptz  NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updated_at"  timestamptz  NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY ("id"),
  CONSTRAINT "articles_slug_key" UNIQUE ("slug"),
  CONSTRAINT "articles_author_id_fkey" FOREIGN KEY ("author_id") REFERENCES "public"."user" ("id") ON UPDATE CASCADE ON DELETE CASCADE
);
CREATE INDEX "idx_articles_author"     ON "public"."articles" ("author_id");
CREATE INDEX "idx_articles_created_at" ON "public"."articles" ("created_at" DESC);
CREATE INDEX "idx_articles_slug"       ON "public"."articles" ("slug");

CREATE TABLE "public"."tags" (
  "id"   bigserial    NOT NULL,
  "name" varchar(255) NOT NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "tags_name_key" UNIQUE ("name")
);
CREATE INDEX "idx_tags_name" ON "public"."tags" ("name");

CREATE TABLE "public"."article_tags" (
  "article_id" bigint NOT NULL,
  "tag_id"     bigint NOT NULL,
  PRIMARY KEY ("article_id", "tag_id"),
  CONSTRAINT "article_tags_article_id_fkey" FOREIGN KEY ("article_id") REFERENCES "public"."articles" ("id") ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT "article_tags_tag_id_fkey"     FOREIGN KEY ("tag_id")     REFERENCES "public"."tags"     ("id") ON UPDATE CASCADE ON DELETE CASCADE
);
CREATE INDEX "idx_article_tags_article_id" ON "public"."article_tags" ("article_id");
CREATE INDEX "idx_article_tags_tag_id"     ON "public"."article_tags" ("tag_id");

CREATE TABLE "public"."comments" (
  "id"         bigserial   NOT NULL,
  "body"       text        NOT NULL,
  "article_id" bigint      NOT NULL,
  "author_id"  bigint      NOT NULL,
  "created_at" timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY ("id"),
  CONSTRAINT "comments_article_id_fkey" FOREIGN KEY ("article_id") REFERENCES "public"."articles" ("id") ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT "comments_author_id_fkey"  FOREIGN KEY ("author_id")  REFERENCES "public"."user"     ("id") ON UPDATE CASCADE ON DELETE CASCADE
);
CREATE INDEX "idx_comments_article_id" ON "public"."comments" ("article_id");
CREATE INDEX "idx_comments_author_id"  ON "public"."comments" ("author_id");

CREATE TABLE "public"."favorites" (
  "user_id"    bigint NOT NULL,
  "article_id" bigint NOT NULL,
  PRIMARY KEY ("user_id", "article_id"),
  CONSTRAINT "favorites_article_id_fkey" FOREIGN KEY ("article_id") REFERENCES "public"."articles" ("id") ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT "favorites_user_id_fkey"    FOREIGN KEY ("user_id")    REFERENCES "public"."user"     ("id") ON UPDATE CASCADE ON DELETE CASCADE
);
CREATE INDEX "idx_favorites_article_id" ON "public"."favorites" ("article_id");

CREATE TABLE "public"."followers" (
  "follower_id" bigint NOT NULL,
  "followee_id" bigint NOT NULL,
  PRIMARY KEY ("follower_id", "followee_id"),
  CONSTRAINT "followers_followee_id_fkey" FOREIGN KEY ("followee_id") REFERENCES "public"."user" ("id") ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT "followers_follower_id_fkey" FOREIGN KEY ("follower_id") REFERENCES "public"."user" ("id") ON UPDATE CASCADE ON DELETE CASCADE
);
CREATE INDEX "idx_followers_followee_id" ON "public"."followers" ("followee_id");

-- vault schema

CREATE SCHEMA IF NOT EXISTS "vault";

CREATE TABLE "vault"."encryption_key" (
  "id"             uuid        NOT NULL DEFAULT gen_random_uuid(),
  "user_id"        bigint      NOT NULL,
  "key_ciphertext" bytea       NOT NULL,
  "algorithm"      text        NOT NULL DEFAULT 'AES-256-GCM',
  "created_at"     timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY ("id"),
  CONSTRAINT "encryption_key_user_id_key"  UNIQUE ("user_id"),
  CONSTRAINT "encryption_key_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."user" ("id") ON DELETE CASCADE
);

CREATE TABLE "vault"."person" (
  "id"                uuid        NOT NULL DEFAULT gen_random_uuid(),
  "user_id"           bigint      NOT NULL,
  "encryption_key_id" uuid        NULL,
  "email_enc"         text        NOT NULL,
  "email_hash"        varchar(64) NOT NULL,
  "email_verified_at" timestamptz NULL,
  "username_enc"      text        NOT NULL,
  "username_hash"     varchar(64) NOT NULL,
  "bio_enc"           text        NULL,
  "image_enc"         text        NULL,
  "created_at"        timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updated_at"        timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY ("id"),
  CONSTRAINT "person_user_id_key"           UNIQUE ("user_id"),
  CONSTRAINT "person_email_hash_key"        UNIQUE ("email_hash"),
  CONSTRAINT "person_username_hash_key"     UNIQUE ("username_hash"),
  CONSTRAINT "person_user_id_fkey"          FOREIGN KEY ("user_id")           REFERENCES "public"."user"            ("id") ON DELETE CASCADE,
  CONSTRAINT "person_encryption_key_id_fkey" FOREIGN KEY ("encryption_key_id") REFERENCES "vault"."encryption_key" ("id") ON DELETE SET NULL
);
CREATE INDEX "idx_person_email_hash"    ON "vault"."person" ("email_hash");
CREATE INDEX "idx_person_username_hash" ON "vault"."person" ("username_hash");

-- auth schema

CREATE SCHEMA IF NOT EXISTS "auth";

CREATE TABLE "auth"."password" (
  "user_id"    bigint      NOT NULL,
  "hash"       text        NOT NULL,
  "created_at" timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY ("user_id"),
  CONSTRAINT "password_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."user" ("id") ON DELETE CASCADE
);
