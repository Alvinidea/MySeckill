create table role(
    `role_id`  bigint not null auto_increment comment '角色id',
    `role_name` varchar(128) not null comment '角色name',
    `role_description` varchar(128) comment '角色描述',
    `role_resource` varchar(128) comment '授权的资源',
    `available` bool comment '是否可用'
)charset=utf8 COMMENT='角色表';


create table iuser(
    `user_id` bigint not null auto_increment comment '用户id',
    `user_phone` bigint unique NOT NULL COMMENT '用户手机号',
    `user_name` varchar(128) not null comment '用户name',
    `user_pwd` varchar(128) comment '密码',
    `user_auth` integer comment '用户权限Authority',
    `user_description` varchar(128) comment '用户描述',
     PRIMARY KEY (user_id),
     key idx_user_id(user_id)
)engine=InnoDB charset=utf8  COMMENT='用户表';

insert into iuser(user_phone, user)