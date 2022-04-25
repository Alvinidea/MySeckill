create table role(
    `role_id`  bigint not null auto_increment comment '角色id',
    `role_name` varchar(128) not null comment '角色name',
    `role_description` varchar(128) comment '角色描述',
    `role_resource` varchar(128) comment '授权的资源',
    `available` bool comment '是否可用'
)charset=utf8 COMMENT='角色表';