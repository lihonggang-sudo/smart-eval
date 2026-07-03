create table admin_user (
    id bigserial primary key,
    username varchar(64) not null,
    password_hash varchar(255) not null,
    display_name varchar(64) not null,
    enabled boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uk_admin_user_username unique (username)
);

create table department (
    id bigserial primary key,
    name varchar(128) not null,
    sort_order integer not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uk_department_name unique (name)
);

create table evaluatee (
    id bigserial primary key,
    name varchar(64) not null,
    department_id bigint not null references department(id),
    enabled boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index idx_evaluatee_department_id on evaluatee(department_id);

create table evaluator_user (
    id bigserial primary key,
    username varchar(64) not null,
    password_plain varchar(64) not null,
    department_id bigint not null references department(id),
    enabled boolean not null default true,
    submitted boolean not null default false,
    submitted_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uk_evaluator_user_username unique (username)
);

create index idx_evaluator_user_department_id on evaluator_user(department_id);
create index idx_evaluator_user_submitted on evaluator_user(submitted);

create table evaluation_record (
    id bigserial primary key,
    evaluator_user_id bigint not null references evaluator_user(id),
    evaluatee_id bigint not null references evaluatee(id),
    department_id bigint not null references department(id),
    grade varchar(32) not null,
    comment1 varchar(255),
    comment2 varchar(255),
    created_at timestamptz not null default now(),
    constraint uk_evaluation_record_user_evaluatee unique (evaluator_user_id, evaluatee_id),
    constraint ck_evaluation_record_grade check (grade in ('EXCELLENT', 'COMPETENT', 'BASIC_COMPETENT', 'INCOMPETENT'))
);

create index idx_evaluation_record_evaluatee_id on evaluation_record(evaluatee_id);
create index idx_evaluation_record_evaluator_user_id on evaluation_record(evaluator_user_id);
create index idx_evaluation_record_department_id on evaluation_record(department_id);
