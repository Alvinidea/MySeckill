--秒杀存储过程
DELIMITER $$ --console ; 转换为 $$
--定义存储过程
-- 参数
-- 参数：in 输入参数；out 输出参数
-- row_count用来返回上一条修改类型的sql(delelte,insert,update)的影响行数
--      = 0 :表示未修改数据;
--      > 0:表示修改的行数;
--      < 0:sql错误/未执行修改
CREATE PROCEDURE  `seckill`.`execute_seckill`
   (in v_seckill_id bigint ,in v_phone bigint,
    in v_kill_time timestamp  ,out r_result int)
    BEGIN
        DECLARE  insert_count int DEFAULT 0;
        START TRANSACTION ;
        INSERT ignore into success_killed
            (seckill_id ,user_phone ,create_time)
            values (v_seckill_id,v_phone,v_kill_time);
        select row_count() into insert_count;
        IF (insert_count = 0) THEN
            ROLLBACK ;
            set r_result = -1;
        ELSEIF(insert_count < 0) THEN
            ROLLBACK  ;
            set r_result = -2;
        ELSE
            UPDATE seckill
            set number = number - 1
            where seckill_id = v_seckill_id
                and end_time > v_kill_time
                and start_time < v_kill_time
                and number > 0;
            select row_count() into insert_count;
            IF (insert_count = 0) THEN
                ROLLBACK ;
                set r_result = 0;
            ELSEIF(insert_count < 0) THEN
                ROLLBACK  ;
                set r_result = -2;
            ELSE
                COMMIT ;
                set r_result = 1;
            END IF;
        END IF;
    END;
$$
--存储过程定义结束

DELIMITER ;

set @r_result = -3;
--执行存储过程
call execute_seckill(1,18466660000,now(),@r_result);

--获取结果
select @r_result;

--存储过程
--1、存储过程优化目标：事务行级锁持有的时间
--2、不要过度依赖存储过程
--3、简单的逻辑，可以应用存储过程
--4、QPS：一个秒杀单6000/qps


-- show create procedure execute_seckill\G 查看存储过程


/* 操作记录
mysql> DELIMITER $$
mysql> CREATE PROCEDURE  `seckill`.`execute_seckill`
    ->    (in v_seckill_id bigint ,in v_phone bigint,
    ->     in v_kill_time timestamp  ,out r_result int)
    ->     BEGIN
    ->         DECLARE  insert_count int DEFAULT 0;
    ->         START TRANSACTION ;
    ->         INSERT ignore into success_killed
    ->             (seckill_id ,user_phone ,create_time)
    ->             values (v_seckill_id,v_phone,v_kill_time);
    ->         select row_count() into insert_count;
    ->         IF (insert_count = 0) THEN
    ->             ROLLBACK ;
    ->             set r_result = -1;
    ->         ELSEIF(insert_count < 0) THEN
    ->             ROLLBACK  ;
    ->             set r_result = -2;
    ->         ELSE
    ->             UPDATE seckill
    ->             set number = number - 1
    ->             where seckill_id = v_seckill_id
    ->                 and end_time > v_kill_time
    ->                 and start_time < v_kill_time
    ->                 and number > 0;
    ->             select row_count() into insert_count;
    ->             IF (insert_count = 0) THEN
    ->                 ROLLBACK ;
    ->                 set r_result = 0;
    ->             ELSEIF(insert_count < 0) THEN
    ->                 ROLLBACK  ;
    ->                 set r_result = -2;
    ->             ELSE
    ->                 COMMIT ;
    ->                 set r_result = 1;
    ->             END IF;
    ->         END IF;
    ->     END;
    -> $$
Query OK, 0 rows affected (0.01 sec)

mysql> show create procedure execute_seckill\G
*************************** 1. row ***************************
           Procedure: execute_seckill
            sql_mode: STRICT_TRANS_TABLES,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION
    Create Procedure: CREATE DEFINER=`root`@`localhost` PROCEDURE `execute_seckill`(in v_seckill_id bigint ,in v_phone bigint,
    in v_kill_time timestamp  ,out r_result int)
BEGIN
        DECLARE  insert_count int DEFAULT 0;
        START TRANSACTION ;
        INSERT ignore into success_killed
            (seckill_id ,user_phone ,create_time)
            values (v_seckill_id,v_phone,v_kill_time);
        select row_count() into insert_count;
        IF (insert_count = 0) THEN
            ROLLBACK ;
            set r_result = -1;
        ELSEIF(insert_count < 0) THEN
            ROLLBACK  ;
            set r_result = -2;
        ELSE
            UPDATE seckill
            set number = number - 1
            where seckill_id = v_seckill_id
                and end_time > v_kill_time
                and start_time < v_kill_time
                and number > 0;
            select row_count() into insert_count;
            IF (insert_count = 0) THEN
                ROLLBACK ;
                set r_result = 0;
            ELSEIF(insert_count < 0) THEN
                ROLLBACK  ;
                set r_result = -2;
            ELSE
                COMMIT ;
                set r_result = 1;
            END IF;
        END IF;
    END
character_set_client: utf8
collation_connection: utf8_general_ci
  Database Collation: utf8_general_ci
1 row in set (0.00 sec)

mysql> DELIMITER ;
mysql> set @r_result = -3;
Query OK, 0 rows affected (0.00 sec)

mysql> call execute_seckill(1,18466660000,now(),@r_result);
Query OK, 0 rows affected (0.01 sec)

mysql> select @r_result;
+-----------+
| @r_result |
+-----------+
|         0 |
+-----------+
1 row in set (0.00 sec)
*/
