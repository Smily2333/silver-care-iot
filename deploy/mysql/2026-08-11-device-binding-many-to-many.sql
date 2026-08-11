-- Pass the target database explicitly with mysql --database=DATABASE_NAME.
-- No USE statement is allowed here, so a development migration cannot silently target production.

-- 一块手表可由多个家庭成员共同查看，但账号和手表两侧均由应用限制为最多 4 个绑定。
-- 应用升级前执行；执行前确认不存在重复的 (user_id, device_id) 组合。
ALTER TABLE device_bindings
    DROP INDEX idx_device_bindings_device;

ALTER TABLE device_bindings
    ADD INDEX idx_device_bindings_device (device_id),
    ADD UNIQUE INDEX uk_device_bindings_user_device (user_id, device_id);
