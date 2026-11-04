-- Step 1: Create evaluation_period table
create table evaluation_period (
    id bigserial primary key,
    name varchar(255) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index idx_evaluation_period_created_at on evaluation_period(created_at desc);

-- Step 2: Add evaluation_period_id columns to existing tables
alter table department add column evaluation_period_id bigint;
alter table evaluatee add column evaluation_period_id bigint;
alter table evaluator_user add column evaluation_period_id bigint;
alter table evaluation_record add column evaluation_period_id bigint;

-- Step 3: Add foreign key constraints with cascade delete
alter table department add constraint fk_department_evaluation_period
    foreign key (evaluation_period_id) references evaluation_period(id) on delete cascade;

alter table evaluatee add constraint fk_evaluatee_evaluation_period
    foreign key (evaluation_period_id) references evaluation_period(id) on delete cascade;

alter table evaluator_user add constraint fk_evaluator_user_evaluation_period
    foreign key (evaluation_period_id) references evaluation_period(id) on delete cascade;

alter table evaluation_record add constraint fk_evaluation_record_evaluation_period
    foreign key (evaluation_period_id) references evaluation_period(id) on delete cascade;

-- Step 4: Add indexes
create index idx_department_evaluation_period_id on department(evaluation_period_id);
create index idx_evaluatee_evaluation_period_id on evaluatee(evaluation_period_id);
create index idx_evaluator_user_evaluation_period_id on evaluator_user(evaluation_period_id);
create index idx_evaluation_record_evaluation_period_id on evaluation_record(evaluation_period_id);

-- Step 5: Modify unique constraints to be period-scoped
alter table department drop constraint uk_department_name;
alter table department add constraint uk_department_period_name unique (evaluation_period_id, name);

alter table evaluator_user drop constraint uk_evaluator_user_username;
alter table evaluator_user add constraint uk_evaluator_user_period_username unique (evaluation_period_id, username);

alter table evaluation_record drop constraint uk_evaluation_record_user_evaluatee;
alter table evaluation_record add constraint uk_evaluation_record_period_user_evaluatee
    unique (evaluation_period_id, evaluator_user_id, evaluatee_id);

-- Step 6: Create default period if there's existing data
do $$
begin
    if exists (select 1 from department limit 1) or
       exists (select 1 from evaluatee limit 1) or
       exists (select 1 from evaluator_user limit 1) or
       exists (select 1 from evaluation_record limit 1) then

        insert into evaluation_period (name, created_at, updated_at)
        values ('默认批次', now(), now());

        update department set evaluation_period_id = (select id from evaluation_period limit 1) where evaluation_period_id is null;
        update evaluatee set evaluation_period_id = (select id from evaluation_period limit 1) where evaluation_period_id is null;
        update evaluator_user set evaluation_period_id = (select id from evaluation_period limit 1) where evaluation_period_id is null;
        update evaluation_record set evaluation_period_id = (select id from evaluation_period limit 1) where evaluation_period_id is null;
    end if;
end $$;

-- Step 7: Set not null constraints
alter table department alter column evaluation_period_id set not null;
alter table evaluatee alter column evaluation_period_id set not null;
alter table evaluator_user alter column evaluation_period_id set not null;
alter table evaluation_record alter column evaluation_period_id set not null;
