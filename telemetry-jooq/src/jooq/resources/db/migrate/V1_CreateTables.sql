create table if not exists `pages` (
  `id`             int          not null,
  `created_at`     datetime     not null,
  `updated_at`     datetime     not null,

  primary key(`id`)
) character set = latin1;
