-- Remove evaluation_period_id from department. Departments become global
-- (not period-scoped), while evaluatee / evaluator_user / evaluation_record
-- keep their own evaluation_period_id.

-- 1. Drop the period-scoped unique constraint and the FK/index that depend on
--    the evaluation_period_id column.
alter table department drop constraint uk_department_period_name;
alter table department drop constraint fk_department_evaluation_period;
drop index idx_department_evaluation_period_id;

-- 2. Drop the column itself.
alter table department drop column evaluation_period_id;

-- 3. Restore global uniqueness on department name.
alter table department add constraint uk_department_name unique (name);
