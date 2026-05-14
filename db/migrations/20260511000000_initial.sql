-- Create "user" table
CREATE TABLE "public"."user" (
  "id" bigserial NOT NULL,
  "email" character varying(255) NOT NULL,
  "username" character varying(255) NOT NULL,
  "password_hash" text NOT NULL,
  "bio" text NULL,
  "image" character varying(1000) NULL,
  "created_at" timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY ("id"),
  CONSTRAINT "user_email_key" UNIQUE ("email"),
  CONSTRAINT "user_username_key" UNIQUE ("username")
);
-- Create index "idx_user_email" to table: "user"
CREATE INDEX "idx_user_email" ON "public"."user" ("email");
-- Create index "idx_user_username" to table: "user"
CREATE INDEX "idx_user_username" ON "public"."user" ("username");
-- Create "article" table
CREATE TABLE "public"."article" (
  "id" bigserial NOT NULL,
  "slug" character varying(255) NOT NULL,
  "title" character varying(255) NOT NULL,
  "description" text NOT NULL,
  "body" text NOT NULL,
  "author_id" bigint NOT NULL,
  "created_at" timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY ("id"),
  CONSTRAINT "article_slug_key" UNIQUE ("slug"),
  CONSTRAINT "article_author_id_fkey" FOREIGN KEY ("author_id") REFERENCES "public"."user" ("id") ON UPDATE CASCADE ON DELETE CASCADE
);
-- Create index "idx_article_author" to table: "article"
CREATE INDEX "idx_article_author" ON "public"."article" ("author_id");
-- Create index "idx_article_created_at" to table: "article"
CREATE INDEX "idx_article_created_at" ON "public"."article" ("created_at" DESC);
-- Create index "idx_article_slug" to table: "article"
CREATE INDEX "idx_article_slug" ON "public"."article" ("slug");
-- Create "tag" table
CREATE TABLE "public"."tag" (
  "id" bigserial NOT NULL,
  "name" character varying(255) NOT NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "tag_name_key" UNIQUE ("name")
);
-- Create index "idx_tag_name" to table: "tag"
CREATE INDEX "idx_tag_name" ON "public"."tag" ("name");
-- Create "article_tag" table
CREATE TABLE "public"."article_tag" (
  "article_id" bigint NOT NULL,
  "tag_id" bigint NOT NULL,
  PRIMARY KEY ("article_id", "tag_id"),
  CONSTRAINT "article_tag_article_id_fkey" FOREIGN KEY ("article_id") REFERENCES "public"."article" ("id") ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT "article_tag_tag_id_fkey" FOREIGN KEY ("tag_id") REFERENCES "public"."tag" ("id") ON UPDATE CASCADE ON DELETE CASCADE
);
-- Create index "idx_article_tag_article_id" to table: "article_tag"
CREATE INDEX "idx_article_tag_article_id" ON "public"."article_tag" ("article_id");
-- Create index "idx_article_tag_tag_id" to table: "article_tag"
CREATE INDEX "idx_article_tag_tag_id" ON "public"."article_tag" ("tag_id");
-- Create "comment" table
CREATE TABLE "public"."comment" (
  "id" bigserial NOT NULL,
  "body" text NOT NULL,
  "article_id" bigint NOT NULL,
  "author_id" bigint NOT NULL,
  "created_at" timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY ("id"),
  CONSTRAINT "comment_article_id_fkey" FOREIGN KEY ("article_id") REFERENCES "public"."article" ("id") ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT "comment_author_id_fkey" FOREIGN KEY ("author_id") REFERENCES "public"."user" ("id") ON UPDATE CASCADE ON DELETE CASCADE
);
-- Create index "idx_comment_article_id" to table: "comment"
CREATE INDEX "idx_comment_article_id" ON "public"."comment" ("article_id");
-- Create index "idx_comment_author_id" to table: "comment"
CREATE INDEX "idx_comment_author_id" ON "public"."comment" ("author_id");
-- Create "favorite" table
CREATE TABLE "public"."favorite" (
  "user_id" bigint NOT NULL,
  "article_id" bigint NOT NULL,
  PRIMARY KEY ("user_id", "article_id"),
  CONSTRAINT "favorite_article_id_fkey" FOREIGN KEY ("article_id") REFERENCES "public"."article" ("id") ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT "favorite_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."user" ("id") ON UPDATE CASCADE ON DELETE CASCADE
);
-- Create index "idx_favorite_article_id" to table: "favorite"
CREATE INDEX "idx_favorite_article_id" ON "public"."favorite" ("article_id");
-- Create index "idx_favorite_user_id" to table: "favorite"
CREATE INDEX "idx_favorite_user_id" ON "public"."favorite" ("user_id");
-- Create "follower" table
CREATE TABLE "public"."follower" (
  "follower_id" bigint NOT NULL,
  "followee_id" bigint NOT NULL,
  PRIMARY KEY ("follower_id", "followee_id"),
  CONSTRAINT "follower_followee_id_fkey" FOREIGN KEY ("followee_id") REFERENCES "public"."user" ("id") ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT "follower_follower_id_fkey" FOREIGN KEY ("follower_id") REFERENCES "public"."user" ("id") ON UPDATE CASCADE ON DELETE CASCADE
);
-- Create index "idx_follower_followee_id" to table: "follower"
CREATE INDEX "idx_follower_followee_id" ON "public"."follower" ("followee_id");
-- Create index "idx_follower_follower_id" to table: "follower"
CREATE INDEX "idx_follower_follower_id" ON "public"."follower" ("follower_id");
-- Create "refresh_token" table
CREATE TABLE "public"."refresh_token" (
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
CREATE INDEX "idx_refresh_token_expires_at" ON "public"."refresh_token" ("expires_at");
-- Create index "idx_refresh_token_user_id" to table: "refresh_token"
CREATE INDEX "idx_refresh_token_user_id" ON "public"."refresh_token" ("user_id");
-- Create "revoked_token" table
CREATE TABLE "public"."revoked_token" (
  "jti" uuid NOT NULL,
  "user_id" bigint NOT NULL,
  "expires_at" timestamptz NOT NULL,
  PRIMARY KEY ("jti")
);
-- Create index "idx_revoked_token_expires_at" to table: "revoked_token"
CREATE INDEX "idx_revoked_token_expires_at" ON "public"."revoked_token" ("expires_at");
