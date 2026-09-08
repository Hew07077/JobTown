-- Run this in the Supabase SQL Editor for the project the Android app uses.
-- Backs the "Not interested" action: once a seeker dismisses a job, it's
-- hidden from their Recommended Jobs list from then on (mirrors saved_jobs).

CREATE TABLE IF NOT EXISTS public.dismissed_jobs (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
  job_id uuid NOT NULL REFERENCES public.jobs(id) ON DELETE CASCADE,
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (user_id, job_id)
);

ALTER TABLE public.dismissed_jobs ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS dismissed_jobs_select ON public.dismissed_jobs;
DROP POLICY IF EXISTS dismissed_jobs_insert ON public.dismissed_jobs;
DROP POLICY IF EXISTS dismissed_jobs_delete ON public.dismissed_jobs;

-- A seeker can only see, add, and remove their own dismissals.
CREATE POLICY dismissed_jobs_select ON public.dismissed_jobs
  FOR SELECT TO authenticated
  USING (user_id::text = (auth.uid())::text);

CREATE POLICY dismissed_jobs_insert ON public.dismissed_jobs
  FOR INSERT TO authenticated
  WITH CHECK (user_id::text = (auth.uid())::text);

CREATE POLICY dismissed_jobs_delete ON public.dismissed_jobs
  FOR DELETE TO authenticated
  USING (user_id::text = (auth.uid())::text);
