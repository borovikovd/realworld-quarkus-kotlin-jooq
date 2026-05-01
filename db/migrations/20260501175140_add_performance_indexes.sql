-- Create index "idx_refresh_token_expires_at" to table: "refresh_token"
CREATE INDEX "idx_refresh_token_expires_at" ON "auth"."refresh_token" ("expires_at");
-- Create index "idx_favorites_user_id" to table: "favorites"
CREATE INDEX "idx_favorites_user_id" ON "public"."favorites" ("user_id");
-- Create index "idx_followers_follower_id" to table: "followers"
CREATE INDEX "idx_followers_follower_id" ON "public"."followers" ("follower_id");
