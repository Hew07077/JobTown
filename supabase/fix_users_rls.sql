-- Run this in the Supabase SQL Editor for the project the Android app uses
-- (https://neyyuqwgqlvcdwabzmut.supabase.co).
-- Lets a signed-in user insert/update their own users + user_profiles rows.

ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_profiles ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS users_select ON public.users;
DROP POLICY IF EXISTS users_insert ON public.users;
DROP POLICY IF EXISTS users_update ON public.users;
DROP POLICY IF EXISTS "users_select" ON public.users;
DROP POLICY IF EXISTS "users_insert" ON public.users;
DROP POLICY IF EXISTS "users_update" ON public.users;

CREATE POLICY users_select ON public.users
  FOR SELECT TO authenticated
  USING (true);

CREATE POLICY users_insert ON public.users
  FOR INSERT TO authenticated
  WITH CHECK (id::text = (auth.uid())::text);

CREATE POLICY users_update ON public.users
  FOR UPDATE TO authenticated
  USING (id::text = (auth.uid())::text)
  WITH CHECK (id::text = (auth.uid())::text);

DROP POLICY IF EXISTS user_profiles_select ON public.user_profiles;
DROP POLICY IF EXISTS user_profiles_insert ON public.user_profiles;
DROP POLICY IF EXISTS user_profiles_update ON public.user_profiles;
DROP POLICY IF EXISTS "user_profiles_select" ON public.user_profiles;
DROP POLICY IF EXISTS "user_profiles_insert" ON public.user_profiles;
DROP POLICY IF EXISTS "user_profiles_update" ON public.user_profiles;

CREATE POLICY user_profiles_select ON public.user_profiles
  FOR SELECT TO authenticated
  USING (true);

CREATE POLICY user_profiles_insert ON public.user_profiles
  FOR INSERT TO authenticated
  WITH CHECK (id::text = (auth.uid())::text);

CREATE POLICY user_profiles_update ON public.user_profiles
  FOR UPDATE TO authenticated
  USING (id::text = (auth.uid())::text)
  WITH CHECK (id::text = (auth.uid())::text);

GRANT USAGE ON SCHEMA public TO authenticated;
GRANT SELECT, INSERT, UPDATE ON public.users TO authenticated;
GRANT SELECT, INSERT, UPDATE ON public.user_profiles TO authenticated;
