schema "public" {}

schema "auth" {}

table "idempotency_key" {
  schema = schema.public
  column "key" {
    null = false
    type = varchar(255)
  }
  column "scope" {
    null = false
    type = varchar(50)
  }
  column "request_path" {
    null = false
    type = varchar(500)
  }
  column "response_status" {
    null = true
    type = integer
  }
  column "response_body" {
    null = true
    type = text
  }
  column "created_at" {
    null    = false
    type    = timestamptz
    default = sql("CURRENT_TIMESTAMP")
  }
  column "expires_at" {
    null = false
    type = timestamptz
  }
  primary_key {
    columns = [column.key, column.scope]
  }
  index "idx_idempotency_key_expires_at" {
    columns = [column.expires_at]
  }
}

table "user" {
  schema = schema.public
  column "id" {
    null = false
    type = bigserial
  }
  column "email" {
    null = false
    type = varchar(255)
  }
  column "username" {
    null = false
    type = varchar(255)
  }
  column "password_hash" {
    null = false
    type = text
  }
  column "bio" {
    null = true
    type = text
  }
  column "image" {
    null = true
    type = varchar(1000)
  }
  column "deleted_at" {
    null = true
    type = timestamptz
  }
  column "created_at" {
    null    = false
    type    = timestamptz
    default = sql("CURRENT_TIMESTAMP")
  }
  column "updated_at" {
    null    = false
    type    = timestamptz
    default = sql("CURRENT_TIMESTAMP")
  }
  primary_key {
    columns = [column.id]
  }
  unique "user_email_key" {
    columns = [column.email]
  }
  unique "user_username_key" {
    columns = [column.username]
  }
  index "idx_user_email" {
    columns = [column.email]
  }
  index "idx_user_username" {
    columns = [column.username]
  }
}

table "refresh_token" {
  schema = schema.auth
  column "id" {
    null    = false
    type    = uuid
    default = sql("gen_random_uuid()")
  }
  column "user_id" {
    null = false
    type = bigint
  }
  column "token_hash" {
    null = false
    type = varchar(100)
  }
  column "expires_at" {
    null = false
    type = timestamptz
  }
  column "revoked_at" {
    null = true
    type = timestamptz
  }
  column "created_at" {
    null    = false
    type    = timestamptz
    default = sql("CURRENT_TIMESTAMP")
  }
  primary_key {
    columns = [column.id]
  }
  unique "refresh_token_token_hash_key" {
    columns = [column.token_hash]
  }
  foreign_key "refresh_token_user_id_fkey" {
    columns     = [column.user_id]
    ref_columns = [table.user.column.id]
    on_delete   = CASCADE
  }
  index "idx_refresh_token_user_id" {
    columns = [column.user_id]
  }
  index "idx_refresh_token_expires_at" {
    columns = [column.expires_at]
  }
}

table "revoked_token" {
  schema = schema.auth
  column "jti" {
    null = false
    type = uuid
  }
  column "user_id" {
    null = false
    type = bigint
  }
  column "expires_at" {
    null = false
    type = timestamptz
  }
  primary_key {
    columns = [column.jti]
  }
  foreign_key "revoked_token_user_id_fkey" {
    columns     = [column.user_id]
    ref_columns = [table.user.column.id]
    on_delete   = CASCADE
  }
  index "idx_revoked_token_expires_at" {
    columns = [column.expires_at]
  }
}

table "followers" {
  schema = schema.public
  column "follower_id" {
    null = false
    type = bigint
  }
  column "followee_id" {
    null = false
    type = bigint
  }
  primary_key {
    columns = [column.follower_id, column.followee_id]
  }
  foreign_key "followers_follower_id_fkey" {
    columns     = [column.follower_id]
    ref_columns = [table.user.column.id]
    on_update   = CASCADE
    on_delete   = CASCADE
  }
  foreign_key "followers_followee_id_fkey" {
    columns     = [column.followee_id]
    ref_columns = [table.user.column.id]
    on_update   = CASCADE
    on_delete   = CASCADE
  }
  index "idx_followers_followee_id" {
    columns = [column.followee_id]
  }
  index "idx_followers_follower_id" {
    columns = [column.follower_id]
  }
}

table "articles" {
  schema = schema.public
  column "id" {
    null = false
    type = bigserial
  }
  column "slug" {
    null = false
    type = varchar(255)
  }
  column "title" {
    null = false
    type = varchar(255)
  }
  column "description" {
    null = false
    type = text
  }
  column "body" {
    null = false
    type = text
  }
  column "author_id" {
    null = false
    type = bigint
  }
  column "created_at" {
    null    = false
    type    = timestamptz
    default = sql("CURRENT_TIMESTAMP")
  }
  column "updated_at" {
    null    = false
    type    = timestamptz
    default = sql("CURRENT_TIMESTAMP")
  }
  primary_key {
    columns = [column.id]
  }
  unique "articles_slug_key" {
    columns = [column.slug]
  }
  foreign_key "articles_author_id_fkey" {
    columns     = [column.author_id]
    ref_columns = [table.user.column.id]
    on_update   = CASCADE
    on_delete   = CASCADE
  }
  index "idx_articles_slug" {
    columns = [column.slug]
  }
  index "idx_articles_author" {
    columns = [column.author_id]
  }
  index "idx_articles_created_at" {
    on {
      column = column.created_at
      desc   = true
    }
  }
}

table "tags" {
  schema = schema.public
  column "id" {
    null = false
    type = bigserial
  }
  column "name" {
    null = false
    type = varchar(255)
  }
  primary_key {
    columns = [column.id]
  }
  unique "tags_name_key" {
    columns = [column.name]
  }
  index "idx_tags_name" {
    columns = [column.name]
  }
}

table "article_tags" {
  schema = schema.public
  column "article_id" {
    null = false
    type = bigint
  }
  column "tag_id" {
    null = false
    type = bigint
  }
  primary_key {
    columns = [column.article_id, column.tag_id]
  }
  foreign_key "article_tags_article_id_fkey" {
    columns     = [column.article_id]
    ref_columns = [table.articles.column.id]
    on_update   = CASCADE
    on_delete   = CASCADE
  }
  foreign_key "article_tags_tag_id_fkey" {
    columns     = [column.tag_id]
    ref_columns = [table.tags.column.id]
    on_update   = CASCADE
    on_delete   = CASCADE
  }
  index "idx_article_tags_article_id" {
    columns = [column.article_id]
  }
  index "idx_article_tags_tag_id" {
    columns = [column.tag_id]
  }
}

table "favorites" {
  schema = schema.public
  column "user_id" {
    null = false
    type = bigint
  }
  column "article_id" {
    null = false
    type = bigint
  }
  primary_key {
    columns = [column.user_id, column.article_id]
  }
  foreign_key "favorites_user_id_fkey" {
    columns     = [column.user_id]
    ref_columns = [table.user.column.id]
    on_update   = CASCADE
    on_delete   = CASCADE
  }
  foreign_key "favorites_article_id_fkey" {
    columns     = [column.article_id]
    ref_columns = [table.articles.column.id]
    on_update   = CASCADE
    on_delete   = CASCADE
  }
  index "idx_favorites_article_id" {
    columns = [column.article_id]
  }
  index "idx_favorites_user_id" {
    columns = [column.user_id]
  }
}

table "comments" {
  schema = schema.public
  column "id" {
    null = false
    type = bigserial
  }
  column "body" {
    null = false
    type = text
  }
  column "article_id" {
    null = false
    type = bigint
  }
  column "author_id" {
    null = false
    type = bigint
  }
  column "created_at" {
    null    = false
    type    = timestamptz
    default = sql("CURRENT_TIMESTAMP")
  }
  column "updated_at" {
    null    = false
    type    = timestamptz
    default = sql("CURRENT_TIMESTAMP")
  }
  primary_key {
    columns = [column.id]
  }
  foreign_key "comments_article_id_fkey" {
    columns     = [column.article_id]
    ref_columns = [table.articles.column.id]
    on_update   = CASCADE
    on_delete   = CASCADE
  }
  foreign_key "comments_author_id_fkey" {
    columns     = [column.author_id]
    ref_columns = [table.user.column.id]
    on_update   = CASCADE
    on_delete   = CASCADE
  }
  index "idx_comments_article_id" {
    columns = [column.article_id]
  }
  index "idx_comments_author_id" {
    columns = [column.author_id]
  }
}
