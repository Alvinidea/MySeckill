-- 创建秒杀数据库
create database seckill;

-- 使用 seckill 数据库
use seckill;

-- 创建秒杀数据库
create table seckill(
    `seckill_id` bigint NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    `name` varchar(120) NOT NULL COMMENT '商品名称',
    `number` int NOT NULL COMMENT '商品数目',
    `start_time` timestamp NOT NULL COMMENT '秒杀开始时间',
    `end_time` timestamp NOT NULL COMMENT '秒杀结束时间',
    `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '秒杀创建时间',
    /* 自动设置创建时间 */
    PRIMARY KEY (seckill_id),
    key idx_start_time(start_time),
    key idx_end_time(end_time),
    key idx_create_time(create_time)
) engine=InnoDB auto_increment=1000 DEFAULT charset=utf8 COMMENT='秒杀数据表';


-- 初始化数据
insert into seckill(name, number, start_time, end_time)
values
('1000秒杀iPhone13', 1000, '2022-3-16 00:00:00', '2022-3-18 00:00:00'),
('2000秒杀iPhone13', 200, '2022-3-16 00:00:00', '2022-3-18 00:00:00'),
('3000秒杀iPhone13', 300, '2022-3-16 00:00:00', '2022-3-21 00:00:00'),
('4000秒杀iPhone13', 400, '2022-3-16 00:00:00', '2022-3-23 00:00:00');

insert into seckill(name, number, start_time, end_time)
values
('1000秒杀iPhone13', 1000, '2022-3-16 00:00:00', '2023-4-18 00:00:00'),
('2000秒杀iPhone13', 200, '2022-3-16 00:00:00', '2023-4-18 00:00:00'),
('3000秒杀iPhone13', 300, '2022-3-16 00:00:00', '2023-4-21 00:00:00'),
('4000秒杀iPhone13', 400, '2022-3-16 00:00:00', '2023-4-23 00:00:00');
-- 秒杀明细表
create table success_killed(
    `seckill_id` bigint NOT NULL COMMENT '秒杀商品id',
    `user_phone` bigint NOT NULL COMMENT '用户手机号',
    `state` tinyint DEFAULT -1 NOT NULL COMMENT '-1 无效，0成功，1已付款，',
    `create_time` timestamp NOT NULL,
    PRIMARY KEY(seckill_id, user_phone),
    key idx_create_time(create_time)
)engine=InnoDB DEFAULT charset=utf8 COMMENT='秒杀明细表';

-- 链接DB
mysql -uroot -p

-- 手写DDL

-- MySQL 去除列的 on update current_timestamp 属性
-- 每次修改表数据，对应的start_time都更新为了当前时间。。。
alter table seckill modify start_time timestamp not null default current_timestamp;

insert into seckill(name, number, start_time, end_time)
values
('1秒杀iPhone13', 1000, '2023-3-16 00:00:00', '2024-3-18 00:00:00');


insert into seckill(name, number, start_time, end_time)
values
('1000秒杀', 1000, '2022-3-23 00:00:00', '2023-3-18 00:00:00');

insert into seckill(name, number, start_time, end_time)
values
('test seckill', 1000, '2022-4-25 00:00:00', '2033-3-18 00:00:00');