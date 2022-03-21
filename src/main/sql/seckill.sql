--秒杀存储过程
-- 1. 先进行秒杀，将秒杀数据插入对应表中 success_killed
-- 2. 然后减少库存

DELIMITER $$ --console ;  将 $$ 作为结束符（替代 ;）
-- 转换为 $$ MySql中 delimiter 详解 https://blog.csdn.net/yuxin6866/article/details/52722913
--定义存储过程
-- 参数
-- 参数：in 输入参数；out 输出参数
-- row_count用来返回上一条修改类型的sql(delelte,insert,update)的影响行数
--      = 0 :表示未修改数据;
--      > 0:表示修改的行数;
--      < 0:sql错误/未执行修改
/*
IN:参数的值必须在调用存储过程时指定，在存储过程中修改该参数的值不能被返回，为默认值
OUT:该值可在存储过程内部被改变，并可返回
INOUT:调用时指定，并且可被改变和返回
*/

CREATE PROCEDURE  `seckill`.`execute_seckill`
   (in v_seckill_id bigint ,in v_phone bigint,
    in v_kill_time timestamp  ,out r_result int)    -- Mysql的存储过程就是相当于是Java的函数 in：输入函数  out：输出参数
    BEGIN
        DECLARE  insert_count int DEFAULT 0;        -- DECLARE 用于定义变量，在存储过程和函数中通过declare定义变量在BEGIN...END中
        START TRANSACTION ;                         -- 开启事务
        INSERT ignore into success_killed           -- 插入数据
            (seckill_id ,user_phone ,create_time)
            values (v_seckill_id,v_phone,v_kill_time);
        -- 判断Update或Delete影响的行数用row_count()函数进行判断，这里需要注意，如果Update前后的值一 样，row_count则为0，
        select row_count() into insert_count;       -- INSERT 操作所影响的数据行数 并赋值给 insert_count;
        IF (insert_count = 0) THEN                      -- INSERT 已存在对应数据 重复秒杀
            ROLLBACK ;
            set r_result = -1;
        ELSEIF(insert_count < 0) THEN                   -- INSERT 出现未知错误 系统异常
            ROLLBACK  ;
            set r_result = -2;
        ELSE                                            -- INSERT 秒杀成功
            UPDATE seckill                              -- 减少库存操作
            set number = number - 1
            where seckill_id = v_seckill_id
                and end_time > v_kill_time
                and start_time < v_kill_time
                and number > 0;                         -- 更新数据
            select row_count() into insert_count;       -- UPDATE 操作所影响的数据行数 并赋值给 insert_count;
            IF (insert_count = 0) THEN
                ROLLBACK ;
                set r_result = 0;                       -- 返回状态
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

DELIMITER ;                             -- 结束符重新设定为 ; 号

set @r_result = -3;                     --定义用户变量
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

/*
    <!-- statementType 声明指向的是什么类型，其中CALLABLE是执行存储过程和函数的-->
    <select id="killByProcedure" parameterType="map" statementType="CALLABLE">
        call execute_seckill(
            #{seckillId,jdbcType=BIGINT,mode=IN},
            #{phone,jdbcType=BIGINT,mode=IN},
            #{killTime,jdbcType=TIMESTAMP,mode=IN},
            #{result,jdbcType=INTEGER,mode=OUT}
        )
    </select>
*/
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
